---
name: taro-react-patterns
description: Taro React 跨端小程序开发模式，涵盖组件规范、页面生命周期、路由跳转、微信 API 封装、状态管理、性能优化与测试策略。用于 leyoSwimming 用户端与教练端小程序开发。
metadata:
  origin: leyoSwimming
---

# Taro React Patterns

Idiomatic Taro React patterns for WeChat Mini Program development in leyoSwimming.

## When to Activate

- Writing or modifying Taro React pages/components in `miniapp-user/` or `miniapp-coach/`
- Reviewing `.tsx` files under Taro apps
- Designing page state, routing, or lifecycle logic
- Wiring WeChat APIs (login, phone, image upload, scan, payment)
- Choosing state management for cross-page shared state

## Core Principles

### 1. Use Taro Components, Not Web Tags

```tsx
// Good
import { View, Text, Image, Button, ScrollView } from '@tarojs/components';

function CoachCard({ coach }: { coach: Coach }) {
  return (
    <View className="coach-card">
      <Image className="avatar" src={coach.avatarUrl} mode="aspectFill" />
      <Text className="name">{coach.name}</Text>
      <Button className="btn" onClick={handleBook}>预约</Button>
    </View>
  );
}

// Bad: div/span/img/button are not valid in Taro JSX compilation
```

### 2. Page Lifecycle via Taro Hooks

Prefer Taro lifecycle hooks over raw `useEffect` for page-level side effects:

```tsx
import { useDidShow, useDidHide, useReady, usePullDownRefresh } from '@tarojs/taro';

export default function CoachListPage() {
  useReady(() => {
    // Page DOM ready, safe to query selector
  });

  useDidShow(() => {
    // Refresh data every time page appears
    refetch();
  });

  usePullDownRefresh(async () => {
    await refetch();
    Taro.stopPullDownRefresh();
  });
}
```

### 3. Style in rpx / px

- Use `rpx` for responsive layout across phone widths.
- Use `px` for hairline borders (`1px`) and fixed tiny offsets.
- Keep design-token alignment with Figma specs in `docs/figma/`.

```scss
.coach-card {
  padding: 24rpx;
  border-bottom: 1px solid #e5e5e5;

  .avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
  }
}
```

### 4. WeChat API Calls via Encapsulated Services

Never call `Taro.login`, `Taro.getUserProfile`, `Taro.request` directly in components. Use shared services:

```tsx
// miniapp-user/src/services/auth.ts
import Taro from '@tarojs/taro';
import { request } from '@/utils/request';

export async function wxLogin() {
  const { code } = await Taro.login();
  return request.post('/api/v1/auth/wechat-login', { code, appType: 'user' });
}
```

### 5. Navigation with Taro Router

```tsx
import Taro from '@tarojs/taro';

Taro.navigateTo({ url: '/pages/coach/detail?id=123' });
Taro.redirectTo({ url: '/pages/login/index' });
Taro.switchTab({ url: '/pages/home/index' });
Taro.navigateBack({ delta: 1 });
```

Pass data via URL query params; do not rely on global state for simple page-to-page data.

### 6. State Management Decision Tree

| State scope | Recommended solution |
|-------------|---------------------|
| Single component | `useState` / `useReducer` |
| Page-level shared between sections | `useState` + lifted state |
| Cross-page user/session data | Zustand / Jotai store |
| Server cache | TanStack Query (if compatible) or custom SWR hook |

Avoid Redux unless truly needed; Zustand is sufficient for miniapp complexity.

### 7. Form Handling

Use controlled inputs with `useState`. Validate on submit and per-field blur.

```tsx
const [form, setForm] = useState({ phone: '', code: '' });
const [errors, setErrors] = useState<Record<string, string>>({});

function handleChange(field: string, value: string) {
  setForm(prev => ({ ...prev, [field]: value }));
  if (errors[field]) {
    setErrors(prev => { const next = { ...prev }; delete next[field]; return next; });
  }
}
```

### 8. Performance

- Avoid passing new objects/functions inline to child components; memoize with `useMemo`/`useCallback` when children re-render often.
- Use lazy loading for long lists (`VirtualList` from `@tarojs/components`).
- Prefer `Image` `lazyLoad` for below-the-fold images.
- Keep single subpackage under 2MB; use Taro subpackages for user/coach split.

### 9. Error Handling

Centralize API errors in `request` interceptor:

```ts
request.interceptors.response.use(
  response => response,
  error => {
    if (error.statusCode === 401) {
      Taro.redirectTo({ url: '/pages/login/index' });
    }
    return Promise.reject(error);
  }
);
```

### 10. Testing

- Unit test hooks and utilities with Vitest.
- Component tests use `@tarojs/test-utils-react` if available; otherwise manual smoke tests in WeChat DevTools.
- E2E tests use WeChat Mini Program automator or manual checklist per user story.
