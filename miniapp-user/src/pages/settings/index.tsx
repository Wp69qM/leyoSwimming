import { useEffect } from 'react';
import Taro from '@tarojs/taro';
import { View, Text } from '@tarojs/components';
import { useAuthStore } from '@/stores/authStore';
import './index.scss';

const SYSTEM_INFO = Taro.getSystemInfoSync();
const STATUS_BAR_HEIGHT = SYSTEM_INFO.statusBarHeight || 0;
const NAV_BAR_HEIGHT = 44;

interface SettingItem {
  label: string;
  iconClass: string;
  url?: string;
}

const SETTING_ITEMS: SettingItem[] = [
  {
    label: '账号安全',
    iconClass: 'settings__icon--security',
  },
  {
    label: '用户须知',
    iconClass: 'settings__icon--terms',
    url: '/pages/terms/index',
  },
  {
    label: '隐私协议',
    iconClass: 'settings__icon--privacy',
    url: '/pages/privacy/index',
  },
];

const LOGIN_URL = '/pages/login/wechat/index';

export default function SettingsPage() {
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);

  useEffect(() => {
    if (!isLoggedIn) {
      Taro.redirectTo({ url: LOGIN_URL });
    }
  }, [isLoggedIn]);

  function handleItemClick(item: SettingItem) {
    if (!item.url) {
      Taro.showToast({ title: '功能开发中', icon: 'none' });
      return;
    }
    Taro.navigateTo({ url: item.url });
  }

  function handleBack() {
    Taro.navigateBack();
  }

  return (
    <View className='settings'>
      <View
        className='settings__navbar'
        style={{ paddingTop: `${STATUS_BAR_HEIGHT}px` }}
      >
        <View
          className='settings__navbar-inner'
          style={{ height: `${NAV_BAR_HEIGHT}px` }}
        >
          <View className='settings__navbar-back' onClick={handleBack}>
            <Text className='settings__navbar-back-icon'>&#8249;</Text>
          </View>
          <Text className='settings__navbar-title'>设置</Text>
        </View>
      </View>

      <View
        className='settings__content'
        style={{ paddingTop: `${STATUS_BAR_HEIGHT + NAV_BAR_HEIGHT}px` }}
      >
        <View className='settings__card'>
        {SETTING_ITEMS.map((item) => (
          <View
            key={item.label}
            className='settings__item'
            onClick={() => handleItemClick(item)}
          >
            <View className='settings__left'>
              <View className={`settings__icon ${item.iconClass}`} />
              <Text className='settings__label'>{item.label}</Text>
            </View>
            <Text className='settings__arrow'>&#8250;</Text>
          </View>
        ))}
        </View>
      </View>
    </View>
  );
}
