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
  url?: string;
  action?: () => void;
}

export default function MinePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
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

  const menuItems: MenuItem[] = [
    {
      label: '我的套餐',
      action: () => Taro.showToast({ title: '功能开发中', icon: 'none' }),
    },
    {
      label: '我的订单',
      action: () => Taro.showToast({ title: '功能开发中', icon: 'none' }),
    },
    {
      label: '我的预约',
      action: () => Taro.showToast({ title: '功能开发中', icon: 'none' }),
    },
    { label: '隐私协议', url: '/pages/login/protocol/index' },
    {
      label: '设置',
      action: () => Taro.showToast({ title: '功能开发中', icon: 'none' }),
    },
  ];

  function handleMenuClick(item: MenuItem) {
    if (item.url) {
      Taro.navigateTo({ url: item.url }).catch(() => {
        Taro.switchTab({ url: item.url });
      });
    } else if (item.action) {
      item.action();
    }
  }

  function handleLogout() {
    Taro.showModal({
      title: '确认退出登录？',
      content: '退出后将清除本地登录状态',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          if (refreshToken) {
            await logout(refreshToken);
          }
        } catch (err) {
          Taro.showToast({ title: handleBusinessError(err), icon: 'none' });
        } finally {
          clearAuth();
          Taro.redirectTo({ url: '/pages/login/wechat/index' });
        }
      },
    });
  }

  function handleLogin() {
    Taro.redirectTo({ url: '/pages/login/wechat/index' });
  }

  if (loading) {
    return (
      <View className='mine mine--loading'>
        <Text className='mine__loading-text'>加载中…</Text>
      </View>
    );
  }

  if (!isLoggedIn) {
    return (
      <View className='mine mine--guest'>
        <Text className='mine__guest-title'>您还未登录</Text>
        <Text className='mine__guest-desc'>登录后查看个人中心</Text>
        <Button className='mine__login-btn' onClick={handleLogin}>
          去登录
        </Button>
      </View>
    );
  }

  const displayName = profile?.name || '游泳爱好者';
  const avatarUrl = profile?.avatarUrl || '';

  return (
    <View className='mine'>
      <View className='mine__header'>
        <Text className='mine__header-title'>我的</Text>
      </View>

      <View className='mine__card mine__profile'>
        <View className='mine__avatar'>
          {avatarUrl ? (
            <Image
              className='mine__avatar-img'
              src={avatarUrl}
              mode='aspectFill'
            />
          ) : (
            <Text className='mine__avatar-text'>{displayName.charAt(0)}</Text>
          )}
        </View>
        <View className='mine__info'>
          <Text className='mine__name'>{displayName}</Text>
          <Text className='mine__phone'>
            {profile?.phone
              ? `${profile.phone.slice(0, 3)}****${profile.phone.slice(-4)}`
              : ''}
          </Text>
        </View>
      </View>

      <View className='mine__menu'>
        {menuItems.map((item) => (
          <View
            key={item.label}
            className='mine__menu-item'
            onClick={() => handleMenuClick(item)}
          >
            <Text className='mine__menu-text'>{item.label}</Text>
            <Text className='mine__menu-arrow'>›</Text>
          </View>
        ))}
      </View>

      <Button className='mine__logout' onClick={handleLogout}>
        退出登录
      </Button>
    </View>
  );
}
