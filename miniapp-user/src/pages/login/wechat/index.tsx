import { useState } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Button } from '@tarojs/components';
import { ProtocolCheckbox } from '@/components/auth/ProtocolCheckbox';
import { wechatLogin } from '@/api/auth';
import { handleBusinessError } from '@/api/request';
import { useAuthStore } from '@/stores/authStore';
import { APP_NAME } from '@/constants';
import './index.scss';

const ERROR_MESSAGES: Record<string, string> = {
  TERMS_NOT_ACCEPTED: '请阅读并同意《用户须知》和《隐私协议》',
  WECHAT_API_ERROR: '微信服务暂时不可用，请稍后重试',
  WECHAT_CODE_INVALID: '登录凭证已失效，请重新点击登录',
  WECHAT_API_TIMEOUT: '网络异常，请重试',
};

export default function WechatLoginPage() {
  const [protocolChecked, setProtocolChecked] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorTip, setErrorTip] = useState('');
  const [showReauth, setShowReauth] = useState(false);
  const login = useAuthStore((state) => state.login);

  function validateProtocol(): boolean {
    if (!protocolChecked) {
      Taro.showToast({
        title: '请阅读并同意《用户须知》和《隐私协议》',
        icon: 'none',
      });
      setErrorTip('请阅读并同意《用户须知》和《隐私协议》');
      return false;
    }
    return true;
  }

  async function handleLogin(phoneEncryptedData?: string, phoneIv?: string) {
    if (!validateProtocol()) return;

    setLoading(true);
    setErrorTip('');
    setShowReauth(false);

    try {
      const { code } = await Taro.login();
      const res = await wechatLogin({
        code,
        phoneEncryptedData,
        phoneIv,
        termsAccepted: true,
        privacyAccepted: true,
        appType: 'user',
      });

      const { accessToken, refreshToken, expiresIn, profileCompleted, userId } =
        res;
      login(accessToken, refreshToken, expiresIn, { userId, profileCompleted });

      if (profileCompleted) {
        Taro.switchTab({ url: '/pages/index/index' });
      } else {
        Taro.redirectTo({ url: '/pages/profile/complete/index' });
      }
    } catch (error) {
      const message = handleBusinessError(error);
      const code = (error as { code?: string }).code;
      setErrorTip(ERROR_MESSAGES[code || ''] || message);
      if (code === 'WECHAT_AUTH_DENIED') {
        setShowReauth(true);
      }
    } finally {
      setLoading(false);
    }
  }

  async function handleGetPhoneNumber(e: {
    detail: { encryptedData?: string; iv?: string; errMsg?: string };
  }) {
    const { encryptedData, iv, errMsg } = e.detail;
    if (errMsg && !errMsg.includes('ok')) {
      setErrorTip('需要微信授权和手机号授权才能登录');
      setShowReauth(true);
      return;
    }
    await handleLogin(encryptedData, iv);
  }

  function handleReauth() {
    Taro.openSetting({
      success: () => {
        setErrorTip('');
        setShowReauth(false);
      },
    });
  }

  function navigateToPhoneLogin() {
    Taro.navigateTo({ url: '/pages/login/phone/index' });
  }

  function openProtocolModal(type: 'terms' | 'privacy') {
    Taro.navigateTo({ url: `/pages/login/protocol/index?type=${type}` });
  }

  return (
    <View className='wechat-login'>
      <View className='wechat-login__brand'>
        <View className='wechat-login__logo'>
          <View className='wechat-login__logo-icon' />
        </View>
        <Text className='wechat-login__name'>{APP_NAME}</Text>
        <Text className='wechat-login__slogan'>让每一次入水，都更有价值</Text>
        <Text className='wechat-login__guide'>微信一键登录，安全又便捷</Text>
      </View>

      {errorTip && (
        <View className='wechat-login__error'>
          <Text className='wechat-login__error-text'>{errorTip}</Text>
        </View>
      )}

      {showReauth && (
        <Button className='wechat-login__reauth' onClick={handleReauth}>
          重新授权
        </Button>
      )}

      <View className='wechat-login__footer'>
        <ProtocolCheckbox
          checked={protocolChecked}
          onChange={setProtocolChecked}
          onOpenTerms={() => openProtocolModal('terms')}
          onOpenPrivacy={() => openProtocolModal('privacy')}
        />
        <Button
          className={`wechat-login__button ${loading ? 'wechat-login__button--loading' : ''}`}
          openType='getPhoneNumber'
          onGetPhoneNumber={handleGetPhoneNumber}
          loading={loading}
          disabled={loading}
        >
          {loading ? '登录中…' : '微信一键登录'}
        </Button>
        <Text
          className='wechat-login__phone-link'
          onClick={navigateToPhoneLogin}
        >
          使用手机号登录
        </Text>
      </View>
    </View>
  );
}
