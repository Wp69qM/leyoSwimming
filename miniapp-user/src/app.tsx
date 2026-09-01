import { installCryptoPolyfill } from '@/utils/crypto-polyfill';

installCryptoPolyfill();

import { Component, PropsWithChildren } from 'react';
import Taro from '@tarojs/taro';
import { useAuthStore } from '@/stores/authStore';
import { isTokenExpired } from '@/api/request';
import './app.scss';

// H5 动态根字号：配合 pxtransform 按 375 设计稿等比缩放。
// 宽屏（>=768px）固定为 375px 设计稿对应的根字号，保持桌面端居中容器内比例一致。
if (process.env.TARO_ENV === 'h5') {
  const DESIGN_WIDTH = 375;
  const BASE_FONT_SIZE = 20;
  const DEVICE_RATIO = 1;
  const ROOT_VALUE = (BASE_FONT_SIZE / DEVICE_RATIO) * 2;

  const setRootFontSize = () => {
    const width = window.innerWidth;
    // 移动端按视口等比缩放，但不超过 375px 设计稿基准（避免平板/桌面端字体/图片过大）
    const fontSize = Math.min(ROOT_VALUE, (width / DESIGN_WIDTH) * ROOT_VALUE);
    document.documentElement.style.fontSize = `${fontSize}px`;
  };

  setRootFontSize();
  window.addEventListener('resize', setRootFontSize);
}

class App extends Component<PropsWithChildren<unknown>> {
  onLaunch() {
    Taro.loadFontFace({
      family: 'remixicon',
      source:
        'url("https://cdn.jsdelivr.net/npm/remixicon@3.5.0/fonts/remixicon.ttf")',
    }).catch(() => {
      // ignore font loading errors
    });
  }

  onShow() {
    const { restoreFromStorage, logout } = useAuthStore.getState();
    restoreFromStorage();
    const { isLoggedIn } = useAuthStore.getState();

    if (isLoggedIn && isTokenExpired()) {
      logout();
      Taro.redirectTo({ url: '/pages/login/wechat/index' });
    }
  }

  render() {
    return this.props.children;
  }
}

export default App;
