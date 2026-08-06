# Taro React Coding Style

Coding style for Taro React mini-program pages and components in leyoSwimming.

## File Organization

```
miniapp-user/src/
├── app.config.ts          # Taro page/route config
├── app.tsx
├── app.scss
├── pages/                 # Top-level pages
│   ├── login/
│   │   ├── index.tsx
│   │   ├── index.scss
│   │   └── index.config.ts
│   └── home/
│       └── index.tsx
├── components/            # Shared components
│   ├── Button/
│   │   ├── index.tsx
│   │   └── index.scss
│   └── CoachCard/
│       ├── index.tsx
│       └── index.scss
├── hooks/                 # Shared custom hooks
├── services/              # API clients
├── stores/                # Zustand/Jotai stores
├── utils/                 # Utilities
└── types/                 # Shared TypeScript types
```

## Naming

- Page components: `pages/<feature>/index.tsx`
- Shared components: `components/<PascalCase>/index.tsx`
- Custom hooks: `use<Action>.ts`
- API services: `services/<domain>.ts`
- Stores: `stores/<domain>Store.ts`

## Taro Component Usage

Always import from `@tarojs/components`:

```tsx
import { View, Text, Image, Button, Input, ScrollView } from '@tarojs/components';
```

Forbidden web tags in Taro JSX: `div`, `span`, `img`, `button`, `input`, `a`, `form`, `label`.

## Styling

- Use SCSS modules or BEM-style global SCSS.
- Use `rpx` for spacing, font sizes, widths, heights.
- Use `px` only for hairline borders (`1px`).
- Class names use kebab-case.

```scss
.login-page {
  padding: 32rpx;

  &__title {
    font-size: 40rpx;
    font-weight: 600;
    margin-bottom: 48rpx;
  }

  &__submit {
    margin-top: 64rpx;
    height: 88rpx;
    border-radius: 44rpx;
  }
}
```

## TypeScript

- Strict mode enabled.
- No `any` except for genuinely unknown third-party data.
- Define props interfaces with `React.FC` or plain function type.

```tsx
interface CoachCardProps {
  coach: CoachSummary;
  onBook: (coachId: number) => void;
}

export default function CoachCard({ coach, onBook }: CoachCardProps) {
  return (
    <View className="coach-card" onClick={() => onBook(coach.id)}>
      ...
    </View>
  );
}
```

## API & WeChat Calls

- All HTTP requests go through `utils/request.ts`.
- All WeChat API calls go through `services/` modules.
- No raw `Taro.request`, `Taro.login`, `Taro.getUserProfile` in components.

## Navigation

- Use `Taro.navigateTo` for stack push.
- Use `Taro.redirectTo` for replacing current page.
- Use `Taro.switchTab` for tabBar pages.
- Pass minimal data via URL query; avoid global state for transient navigation data.

## Forms

- Controlled inputs only.
- Validate before submit.
- Show inline errors per field.
- Disable submit button while submitting.

## Security

- Never store tokens in `localStorage`; use Taro storage (`Taro.setStorageSync`) only for non-sensitive cache.
- JWT access token should be kept in memory and refreshed via httpOnly cookie or secure storage.
- Sanitize any `rich-text` content from server.
- Validate all user URLs (`javascript:` schemes forbidden).
