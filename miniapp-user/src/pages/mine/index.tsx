import { useEffect, useState } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Image, Button } from '@tarojs/components';
import { getProfile, type UserProfile } from '@/api/profile';
import { logout } from '@/api/auth';
import { handleBusinessError } from '@/api/request';
import { useAuthStore } from '@/stores/authStore';
import './index.scss';

interface MenuItem {
  label: string;
  iconClass: string;
  url?: string;
  requireAuth?: boolean;
}

const MENU_ITEMS: MenuItem[] = [
  {
    label: '我的套餐',
    iconClass: 'mine__menu-icon--package',
    url: '/pages/package/mine/index',
    requireAuth: true,
  },
  {
    label: '我的订单',
    iconClass: 'mine__menu-icon--order',
    requireAuth: true,
  },
  {
    label: '我的预约',
    iconClass: 'mine__menu-icon--booking',
    requireAuth: true,
  },
  {
    label: '隐私协议',
    iconClass: 'mine__menu-icon--privacy',
    url: '/pages/privacy/index',
    requireAuth: true,
  },
  {
    label: '设置',
    iconClass: 'mine__menu-icon--settings',
    url: '/pages/settings/index',
    requireAuth: true,
  },
];

const SYSTEM_INFO = Taro.getSystemInfoSync();
const STATUS_BAR_HEIGHT = SYSTEM_INFO.statusBarHeight || 0;
const NAV_BAR_HEIGHT = 44;

const LOGIN_URL = '/pages/login/wechat/index';

export default function MinePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [logoutLoading, setLogoutLoading] = useState(false);
  const {
    accessToken,
    refreshToken,
    logout: clearAuth,
    isLoggedIn,
  } = useAuthStore();

  useEffect(() => {
    if (!accessToken) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    async function load() {
      try {
        const data = await getProfile();
        if (cancelled) return;
        setProfile(data);
      } catch (err) {
        if (cancelled) return;
        Taro.showToast({ title: handleBusinessError(err), icon: 'none' });
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  function redirectToLogin() {
    Taro.redirectTo({ url: LOGIN_URL });
  }

  function handleProfileClick() {
    if (!isLoggedIn) {
      redirectToLogin();
      return;
    }
    Taro.navigateTo({ url: '/pages/profile/complete/index' });
  }

  function handleMenuClick(item: MenuItem) {
    if (!isLoggedIn && item.requireAuth) {
      redirectToLogin();
      return;
    }
    if (item.url) {
      const url = item.url;
      Taro.navigateTo({ url }).catch(() => {
        Taro.switchTab({ url });
      });
      return;
    }
    Taro.showToast({ title: '功能开发中', icon: 'none' });
  }

  function handleLogout() {
    Taro.showModal({
      title: '确定要退出登录吗？',
      cancelText: '取消',
      confirmText: '确认',
      success: async (res) => {
        if (!res.confirm) return;
        setLogoutLoading(true);
        try {
          if (refreshToken) {
            await logout(refreshToken);
          }
        } catch (err) {
          Taro.showToast({ title: handleBusinessError(err), icon: 'none' });
        } finally {
          clearAuth();
          setLogoutLoading(false);
        }
      },
    });
  }

  const displayName = profile?.name || '游泳爱好者';
  const avatarUrl = profile?.avatarUrl || '';

  return (
    <View className='mine'>
      <View
        className='mine__navbar'
        style={{ paddingTop: `${STATUS_BAR_HEIGHT}px` }}
      >
        <View
          className='mine__navbar-inner'
          style={{ height: `${NAV_BAR_HEIGHT}px` }}
        >
          <Text className='mine__navbar-title'>我的</Text>
        </View>
      </View>

      <View
        className='mine__content'
        style={{ paddingTop: `${STATUS_BAR_HEIGHT + NAV_BAR_HEIGHT}px` }}
      >
        <View className='mine__card mine__profile' onClick={handleProfileClick}>
          <View className='mine__avatar'>
            {avatarUrl ? (
              <Image
                className='mine__avatar-img'
                src={avatarUrl}
                mode='aspectFill'
              />
            ) : (
              <Text className='mine__avatar-text'>
                {isLoggedIn ? displayName.charAt(0) : '?'}
              </Text>
            )}
          </View>
          <View className='mine__info'>
            {isLoggedIn ? (
              <>
                <Text className='mine__name'>{displayName}</Text>
                <Text className='mine__status'>注册用户</Text>
              </>
            ) : (
              <>
                <Text className='mine__name'>请登录/注册</Text>
                <Text className='mine__status'>登录后查看个人中心</Text>
              </>
            )}
          </View>
        </View>

        <View className='mine__menu'>
          {MENU_ITEMS.map((item) => (
            <View
              key={item.label}
              className='mine__menu-item'
              onClick={() => handleMenuClick(item)}
            >
              <View className='mine__menu-left'>
                <View className={`mine__menu-icon ${item.iconClass}`} />
                <Text className='mine__menu-text'>{item.label}</Text>
              </View>
              <Text className='mine__menu-arrow'>&#8250;</Text>
            </View>
          ))}
        </View>

        {isLoggedIn && (
          <Button
            className={`mine__logout ${logoutLoading ? 'mine__logout--loading' : ''}`}
            onClick={handleLogout}
            loading={logoutLoading}
            disabled={logoutLoading}
          >
            {logoutLoading ? '退出中…' : '退出登录'}
          </Button>
        )}

        {loading && (
          <View className='mine__loading'>
            <Text className='mine__loading-text'>加载中…</Text>
          </View>
        )}
      </View>
    </View>
  );
}
