import Taro from '@tarojs/taro';

export function getPageQuery(): Record<string, string> {
  const routerParams = Taro.getCurrentInstance().router?.params ?? {};
  // Taro 在小程序与非 H5 场景下能正常解析路由参数；当 id 已存在时直接复用，
  // 仅在 H5 hash 模式 Taro 解析异常时才手动 fallback 到 URL 解析。
  if (routerParams.id) {
    return routerParams as Record<string, string>;
  }

  if (process.env.TARO_ENV === 'h5' && typeof window !== 'undefined') {
    const hash = window.location.hash ?? '';
    const search = hash.includes('?')
      ? hash.split('?')[1]
      : window.location.search.slice(1);
    const result: Record<string, string> = {};
    if (search) {
      search.split('&').forEach((pair) => {
        const [key, value] = pair.split('=');
        if (key) {
          result[decodeURIComponent(key)] = value
            ? decodeURIComponent(value)
            : '';
        }
      });
    }
    return result;
  }

  return routerParams as Record<string, string>;
}
