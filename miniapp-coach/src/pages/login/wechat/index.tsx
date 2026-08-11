import { useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Button } from '@tarojs/components'
import { ProtocolCheckbox } from '@/components/auth/ProtocolCheckbox'
import { wechatLogin } from '@/api/auth'
import { handleBusinessError, getErrorCode, ApiError } from '@/api/request'
import { useAuthStore, getRedirectPageByStatus } from '@/stores/authStore'
import { usePolicyVersions } from '@/hooks/usePolicy'
import { APP_NAME } from '@/constants'
import './index.scss'

const ERROR_CODE_MESSAGES: Record<number, string> = {
  440001: '请阅读并同意《用户须知》和《隐私协议》',
  430002: '微信服务暂时不可用，请稍后重试',
  430001: '登录凭证已失效，请重新点击登录',
  430003: '网络异常，请重试'
}

export default function WechatLoginPage() {
  const [protocolChecked, setProtocolChecked] = useState(false)
  const [loading, setLoading] = useState(false)
  const [errorTip, setErrorTip] = useState('')
  const [showReauth, setShowReauth] = useState(false)
  const login = useAuthStore((state) => state.login)
  const { termsVersion, privacyVersion, loading: policyLoading, error: policyError } =
    usePolicyVersions()

  function validateProtocol(): boolean {
    if (!protocolChecked) {
      setErrorTip('请阅读并同意《用户须知》和《隐私协议》')
      return false
    }
    return true
  }

  function navigateToPhoneLogin() {
    Taro.navigateTo({ url: '/pages/login/phone/index' })
  }

  function openProtocolPage(type: 'terms' | 'privacy') {
    Taro.navigateTo({ url: `/pages/login/protocol/index?type=${type}` })
  }

  async function handleLogin(phoneEncryptedData?: string, phoneIv?: string) {
    if (!validateProtocol()) return

    if (policyLoading || !termsVersion || !privacyVersion) {
      Taro.showToast({ title: '协议加载中，请稍候', icon: 'none' })
      return
    }

    if (policyError) {
      Taro.showToast({ title: policyError, icon: 'none' })
      return
    }

    setLoading(true)
    setErrorTip('')
    setShowReauth(false)

    try {
      const { code } = await Taro.login()
      const result = await wechatLogin({
        code,
        phoneEncryptedData: phoneEncryptedData || '',
        phoneIv: phoneIv || '',
        termsAccepted: true,
        privacyAccepted: true,
        termsVersion,
        privacyVersion,
        appType: 'coach'
      })

      login(
        result.accessToken,
        result.refreshToken,
        result.expiresInSeconds,
        { id: result.coachId, status: result.coachStatus }
      )

      if (!result.profileCompleted) {
        Taro.redirectTo({ url: '/pages/profile/complete/index' })
        return
      }

      const redirectUrl = getRedirectPageByStatus(result.coachStatus)
      if (result.coachStatus === 1 || result.coachStatus === 4) {
        Taro.switchTab({ url: redirectUrl })
      } else {
        Taro.redirectTo({ url: redirectUrl })
      }
    } catch (error) {
      const errCode = getErrorCode(error)
      const message = errCode && ERROR_CODE_MESSAGES[errCode]
        ? ERROR_CODE_MESSAGES[errCode]
        : handleBusinessError(error)
      setErrorTip(message)
      if (errCode === 430002 || errCode === 430001 || (error instanceof ApiError && errCode === 0)) {
        setShowReauth(false)
      }
    } finally {
      setLoading(false)
    }
  }

  async function handleGetPhoneNumber(e: { detail: { encryptedData?: string; iv?: string; errMsg?: string } }) {
    const { encryptedData, iv, errMsg } = e.detail
    if (errMsg && !errMsg.includes('ok')) {
      setErrorTip('需要微信授权和手机号授权才能登录')
      setShowReauth(true)
      return
    }
    await handleLogin(encryptedData, iv)
  }

  function handleReauth() {
    Taro.openSetting({
      success: () => {
        setErrorTip('')
        setShowReauth(false)
      }
    })
  }

  return (
    <View className="wechat-login">
      <View className="wechat-login__brand">
        <View className="wechat-login__logo">
          <View className="wechat-login__logo-icon" />
        </View>
        <Text className="wechat-login__name">{APP_NAME}</Text>
        <Text className="wechat-login__slogan">专业教练，轻松开课</Text>
        <Text className="wechat-login__guide">微信一键登录，开启教练工作台</Text>
      </View>

      {errorTip && (
        <View className="wechat-login__error">
          <Text className="wechat-login__error-text">{errorTip}</Text>
        </View>
      )}

      {showReauth && (
        <Button className="wechat-login__reauth" onClick={handleReauth}>
          重新授权
        </Button>
      )}

      <View className="wechat-login__footer">
        <ProtocolCheckbox
          checked={protocolChecked}
          onChange={setProtocolChecked}
          onOpenTerms={() => openProtocolPage('terms')}
          onOpenPrivacy={() => openProtocolPage('privacy')}
        />
        <Button
          className={`wechat-login__button ${loading ? 'wechat-login__button--loading' : ''}`}
          openType="getPhoneNumber"
          onGetPhoneNumber={handleGetPhoneNumber}
          loading={loading}
          disabled={loading}
        >
          {loading ? '登录中…' : '微信一键登录'}
        </Button>
        <Text className="wechat-login__phone-link" onClick={navigateToPhoneLogin}>
          使用手机号登录
        </Text>
      </View>
    </View>
  )
}
