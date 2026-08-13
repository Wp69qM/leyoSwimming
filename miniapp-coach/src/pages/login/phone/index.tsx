import { useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import { ProtocolCheckbox } from '@/components/auth/ProtocolCheckbox'
import { phoneLogin } from '@/api/auth'
import { sendSmsCode } from '@/api/common'
import { handleBusinessError, getErrorCode } from '@/api/request'
import { useAuthStore, getRedirectPageByStatus } from '@/stores/authStore'
import { useCountdown } from '@/hooks/useCountdown'
import { usePolicyVersions } from '@/hooks/usePolicy'
import { formatPhoneInput, sanitizeCodeInput, isValidPhone } from '@/utils/phone'
import { APP_NAME } from '@/constants'
import './index.scss'

const ERROR_CODE_MESSAGES: Record<number, string> = {
  440001: '请阅读并同意《用户须知》和《隐私协议》',
  420001: '验证码错误或已过期',
  420002: '请 60 秒后再试',
  420003: '验证码发送失败，请稍后重试'
}

export default function PhoneLoginPage() {
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [protocolChecked, setProtocolChecked] = useState(false)
  const [loading, setLoading] = useState(false)
  const [sending, setSending] = useState(false)
  const [errorTip, setErrorTip] = useState('')
  const [phoneError, setPhoneError] = useState(false)
  const [codeError, setCodeError] = useState(false)
  const login = useAuthStore((state) => state.login)
  const { seconds, isRunning, start } = useCountdown({ initialSeconds: 60 })
  const { termsVersion, privacyVersion, loading: policyLoading, error: policyError } =
    usePolicyVersions()

  const canSend = isValidPhone(phone) && !isRunning && !sending
  const canSubmit = isValidPhone(phone) && code.length === 6

  function handlePhoneChange(value: string) {
    const formatted = formatPhoneInput(value)
    setPhone(formatted)
    if (phoneError) setPhoneError(false)
    if (errorTip === '请输入正确的手机号') setErrorTip('')
  }

  function handleCodeChange(value: string) {
    const formatted = sanitizeCodeInput(value)
    setCode(formatted)
    if (codeError) setCodeError(false)
    if (errorTip === '验证码错误或已过期') setErrorTip('')
  }

  async function handleSendCode() {
    if (!canSend) return

    if (!isValidPhone(phone)) {
      setPhoneError(true)
      setErrorTip('请输入正确的手机号')
      return
    }

    setSending(true)
    setErrorTip('')
    setPhoneError(false)

    try {
      await sendSmsCode({ phone, appType: 'coach', scene: 'login' })
      start()
    } catch (error) {
      const errCode = getErrorCode(error)
      const message = errCode && ERROR_CODE_MESSAGES[errCode]
        ? ERROR_CODE_MESSAGES[errCode]
        : handleBusinessError(error)
      setErrorTip(message)
    } finally {
      setSending(false)
    }
  }

  async function handleSubmit() {
    setErrorTip('')
    setPhoneError(false)
    setCodeError(false)

    if (!protocolChecked) {
      setErrorTip(ERROR_CODE_MESSAGES[440001])
      return
    }

    if (policyLoading || !termsVersion || !privacyVersion) {
      Taro.showToast({ title: '协议加载中，请稍候', icon: 'none' })
      return
    }

    if (policyError) {
      Taro.showToast({ title: policyError, icon: 'none' })
      return
    }

    if (!isValidPhone(phone)) {
      setPhoneError(true)
      setErrorTip('请输入正确的手机号')
      return
    }

    if (code.length !== 6) {
      setCodeError(true)
      setErrorTip('请输入6位验证码')
      return
    }

    setLoading(true)

    try {
      const result = await phoneLogin({
        phone,
        code,
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
      if (errCode === 420001) setCodeError(true)
    } finally {
      setLoading(false)
    }
  }

  function navigateToWechatLogin() {
    Taro.navigateTo({ url: '/pages/login/wechat/index' })
  }

  function openProtocolPage(type: 'terms' | 'privacy') {
    Taro.navigateTo({ url: `/pages/login/protocol/index?type=${type}` })
  }

  return (
    <View className="phone-login">
      <View className="phone-login__brand">
        <View className="phone-login__logo">
          <View className="phone-login__logo-icon" />
        </View>
        <Text className="phone-login__name">{APP_NAME}</Text>
        <Text className="phone-login__slogan">专业游泳约课平台</Text>
      </View>

      <View className="phone-login__form">
        <Text className="phone-login__title">手机号登录</Text>
        <Text className="phone-login__subtitle">输入手机号获取验证码，即可快速登录</Text>

        <View className="phone-login__field">
          <Text className="phone-login__label">手机号</Text>
          <View className={`phone-login__input-wrap ${phoneError ? 'phone-login__input-wrap--error' : ''}`}>
            <View className="phone-login__input-icon phone-login__input-icon--phone" />
            <Input
              className="phone-login__input"
              type="number"
              placeholder="请输入11位手机号"
              value={phone}
              onInput={(e) => handlePhoneChange(e.detail.value)}
              maxlength={11}
            />
          </View>
        </View>

        <View className="phone-login__field">
          <Text className="phone-login__label">验证码</Text>
          <View className={`phone-login__input-wrap ${codeError ? 'phone-login__input-wrap--error' : ''}`}>
            <View className="phone-login__input-icon phone-login__input-icon--code" />
            <Input
              className="phone-login__input"
              type="number"
              placeholder="请输入短信验证码"
              value={code}
              onInput={(e) => handleCodeChange(e.detail.value)}
              maxlength={6}
            />
            <Button
              className={`phone-login__code-btn ${canSend ? 'phone-login__code-btn--active' : ''}`}
              onClick={handleSendCode}
              disabled={!canSend}
              loading={sending}
            >
              {isRunning ? `${seconds}s后重发` : sending ? '发送中…' : '获取验证码'}
            </Button>
          </View>
        </View>

        <ProtocolCheckbox
          checked={protocolChecked}
          onChange={setProtocolChecked}
          onOpenTerms={() => openProtocolPage('terms')}
          onOpenPrivacy={() => openProtocolPage('privacy')}
        />

        {errorTip && (
          <View className="phone-login__error">
            <Text className="phone-login__error-text">{errorTip}</Text>
          </View>
        )}

        <Button
          className={`phone-login__submit ${loading ? 'phone-login__submit--loading' : ''}`}
          onClick={handleSubmit}
          disabled={loading || !canSubmit}
          loading={loading}
        >
          {loading ? '登录中…' : '登录'}
        </Button>

        <View className="phone-login__divider">
          <View className="phone-login__divider-line" />
          <Text className="phone-login__divider-text">其他登录方式</Text>
          <View className="phone-login__divider-line" />
        </View>

        <View className="phone-login__wechat-link" onClick={navigateToWechatLogin}>
          <View className="phone-login__wechat-icon" />
          <Text className="phone-login__wechat-text">使用微信登录</Text>
        </View>

        <Text className="phone-login__bottom-tip">未注册手机号将自动创建账号</Text>
      </View>
    </View>
  )
}
