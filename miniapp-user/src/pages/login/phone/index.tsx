import { useState } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Input, Button, Image } from '@tarojs/components';
import { ProtocolCheckbox } from '@/components/auth/ProtocolCheckbox';
import LogoIcon from '@/assets/calicat/icons/logo-swimming.svg';
import {
  ProtocolDrawer,
  type ProtocolTab,
} from '@/pages/login/protocol/ProtocolDrawer';
import { phoneLogin } from '@/api/auth';
import { sendSmsCode } from '@/api/common';
import { handleBusinessError, getErrorCode } from '@/api/request';
import { useAuthStore } from '@/stores/authStore';
import { useCountdown } from '@/hooks/useCountdown';
import { usePolicyVersions } from '@/hooks/usePolicy';
import { APP_NAME } from '@/constants';
import { formatPhoneInput, isValidPhone } from '@/utils/phone';
import './index.scss';

const ERROR_MESSAGES: Record<number, string> = {
  440001: '请阅读并同意《用户须知》和《隐私协议》',
  100003: '请输入正确的手机号',
  420001: '验证码错误或已过期',
  420002: '请 60 秒后再试',
  420003: '验证码发送失败，请稍后重试',
};

export default function PhoneLoginPage() {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [protocolChecked, setProtocolChecked] = useState(false);
  const [protocolVisible, setProtocolVisible] = useState(false);
  const [protocolInitialTab, setProtocolInitialTab] =
    useState<ProtocolTab>('terms');
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const [errorTip, setErrorTip] = useState('');
  const [phoneError, setPhoneError] = useState(false);
  const [codeError, setCodeError] = useState(false);
  const [loginSuccess, setLoginSuccess] = useState(false);
  const login = useAuthStore((state) => state.login);
  const { seconds, isRunning, start } = useCountdown({ initialSeconds: 60 });
  const {
    termsVersion,
    privacyVersion,
    loading: policyLoading,
    error: policyError,
  } = usePolicyVersions();

  const canSend = isValidPhone(phone) && !isRunning && !sending;
  const canSubmit = isValidPhone(phone) && code.length === 6;

  function handlePhoneChange(value: string) {
    const formatted = formatPhoneInput(value);
    setPhone(formatted);
    if (phoneError) setPhoneError(false);
    if (errorTip === ERROR_MESSAGES[100003]) setErrorTip('');
  }

  function handleCodeChange(value: string) {
    const formatted = value.replace(/\D/g, '').slice(0, 6);
    setCode(formatted);
    if (codeError) setCodeError(false);
    if (errorTip === ERROR_MESSAGES[420001]) setErrorTip('');
  }

  async function handleSendCode() {
    if (!canSend) return;

    setSending(true);
    setErrorTip('');
    setPhoneError(false);

    try {
      await sendSmsCode({ phone, appType: 'user', scene: 'login' });
      start();
    } catch (error) {
      const errCode = getErrorCode(error);
      setErrorTip(ERROR_MESSAGES[errCode ?? 0] || '验证码发送失败，请稍后重试');
    } finally {
      setSending(false);
    }
  }

  async function handleSubmit() {
    setErrorTip('');
    setPhoneError(false);
    setCodeError(false);

    if (!protocolChecked) {
      const tip = ERROR_MESSAGES[440001];
      Taro.showToast({
        title: tip,
        icon: 'none',
      });
      setErrorTip(tip);
      return;
    }

    if (policyLoading || !termsVersion || !privacyVersion) {
      Taro.showToast({ title: '协议加载中，请稍候', icon: 'none' });
      return;
    }

    if (policyError) {
      Taro.showToast({ title: policyError, icon: 'none' });
      return;
    }

    if (!isValidPhone(phone)) {
      setPhoneError(true);
      setErrorTip(ERROR_MESSAGES[100003]);
      return;
    }

    if (code.length !== 6) {
      setCodeError(true);
      setErrorTip('请输入6位验证码');
      return;
    }

    setLoading(true);

    try {
      const res = await phoneLogin({
        phone,
        code,
        termsAccepted: true,
        privacyAccepted: true,
        termsVersion,
        privacyVersion,
      });

      const { accessToken, refreshToken, expiresIn, profileCompleted, userId } =
        res;
      login(accessToken, refreshToken, expiresIn, {
        userId,
        phone,
        profileCompleted,
      });

      setLoginSuccess(true);
      setTimeout(() => {
        if (profileCompleted) {
          Taro.switchTab({ url: '/pages/index/index' });
        } else {
          Taro.redirectTo({ url: '/pages/profile/complete/index' });
        }
      }, 300);
    } catch (error) {
      const message = handleBusinessError(error);
      const errCode = getErrorCode(error);
      const tip = ERROR_MESSAGES[errCode ?? 0] || message;
      setErrorTip(tip);
      if (errCode === 420001) setCodeError(true);
    } finally {
      setLoading(false);
    }
  }

  function navigateToWechatLogin() {
    Taro.navigateTo({ url: '/pages/login/wechat/index' });
  }

  function openProtocolModal(type: 'terms' | 'privacy') {
    setProtocolInitialTab(type);
    setProtocolVisible(true);
  }

  function handleCloseProtocol() {
    setProtocolVisible(false);
  }

  function handleAgreeProtocol() {
    setProtocolChecked(true);
    setProtocolVisible(false);
  }

  return (
    <View className='phone-login'>
      <View className='phone-login__brand'>
        <View className='phone-login__logo'>
          <Image className='phone-login__logo-icon' src={LogoIcon} />
        </View>
        <Text className='phone-login__name'>{APP_NAME}</Text>
        <Text className='phone-login__slogan'>专业游泳约课平台</Text>
      </View>

      <View className='phone-login__card'>
        <Text className='phone-login__title'>手机号登录</Text>
        <Text className='phone-login__subtitle'>
          输入手机号获取验证码，即可快速登录
        </Text>

        <View className='phone-login__field'>
          <Text className='phone-login__label'>手机号</Text>
          <View
            className={`phone-login__input-row ${phoneError ? 'phone-login__input-row--error' : ''}`}
          >
            <Text className='phone-login__input-icon phone-login__input-icon--phone'>
              
            </Text>
            <Input
              className='phone-login__input'
              type='number'
              placeholder='请输入11位手机号'
              value={phone}
              onInput={(e) => handlePhoneChange(e.detail.value)}
              maxlength={11}
            />
          </View>
        </View>

        <View className='phone-login__field'>
          <Text className='phone-login__label'>验证码</Text>
          <View
            className={`phone-login__input-row ${codeError ? 'phone-login__input-row--error' : ''}`}
          >
            <Text className='phone-login__input-icon phone-login__input-icon--code'>
              
            </Text>
            <Input
              className='phone-login__input'
              type='number'
              placeholder='请输入短信验证码'
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
              {isRunning
                ? `${seconds}s后重发`
                : sending
                  ? '发送中…'
                  : '获取验证码'}
            </Button>
          </View>
        </View>

        <ProtocolCheckbox
          checked={protocolChecked}
          onChange={setProtocolChecked}
          onOpenTerms={() => openProtocolModal('terms')}
          onOpenPrivacy={() => openProtocolModal('privacy')}
        />

        {errorTip && (
          <View className='phone-login__error'>
            <View className='phone-login__error-icon' />
            <Text className='phone-login__error-text'>{errorTip}</Text>
          </View>
        )}

        <Button
          className={`phone-login__submit ${loading ? 'phone-login__submit--loading' : ''} ${loginSuccess ? 'phone-login__submit--success' : ''}`}
          onClick={handleSubmit}
          disabled={loading || !canSubmit || loginSuccess}
          loading={loading}
        >
          {loading ? '登录中…' : loginSuccess ? '登录成功' : '登录'}
        </Button>

        <View className='phone-login__divider'>
          <View className='phone-login__divider-line' />
          <Text className='phone-login__divider-text'>其他登录方式</Text>
          <View className='phone-login__divider-line' />
        </View>

        <View
          className='phone-login__wechat-entry'
          onClick={navigateToWechatLogin}
        >
          <Text className='phone-login__wechat-icon'></Text>
          <Text className='phone-login__wechat-text'>使用微信登录</Text>
        </View>

        <Text className='phone-login__tip'>未注册手机号将自动创建账号</Text>
      </View>

      <ProtocolDrawer
        visible={protocolVisible}
        initialTab={protocolInitialTab}
        onClose={handleCloseProtocol}
        onAgree={handleAgreeProtocol}
      />
    </View>
  );
}
