import { useMemo } from 'react';
import { View, Text, Image, Input } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { Icon } from '@/components/common/Icon';
import type { PackageDetailCoach } from '@/types/package';

export const COACH_STATUS_ACTIVE = 1;

export function useStatusBarHeight(): number {
  return useMemo(() => Taro.getSystemInfoSync().statusBarHeight || 20, []);
}

export const PAGE_PATHS = {
  home: '/pages/index/index',
  coachList: '/pages/coach/index',
  coachDetail: '/pages/coach/detail/index',
  orderConfirm: '/pages/order/confirm/index',
  payment: '/pages/order/payment/index',
  termsDetail: '/pages/terms/detail/index',
};

export const QUICK_HOURS = [4, 8, 12, 20];

export const VALIDITY_DAYS = [30, 60, 90, 180];

export interface StrokeOption {
  id: number;
  label: string;
}

export const STROKE_OPTIONS: StrokeOption[] = [
  { id: 1, label: '自由泳' },
  { id: 2, label: '蛙泳' },
  { id: 3, label: '仰泳' },
  { id: 4, label: '蝶泳' },
];

export function buildStrokeOptions(teachingStrokes: string[]): StrokeOption[] {
  const normalized = teachingStrokes
    .map((s) => s.trim())
    .filter((s) => s.length > 0);
  const filtered = STROKE_OPTIONS.filter((option) =>
    normalized.includes(option.label)
  );
  return filtered.length > 0 ? filtered : STROKE_OPTIONS;
}

export type AgreementKey = 'userNotice' | 'health' | 'disclaimer';

export interface AgreementItem {
  key: AgreementKey;
  label: string;
  type: string;
}

export const AGREEMENT_ITEMS: AgreementItem[] = [
  {
    key: 'userNotice',
    label: '《用户须知》',
    type: 'user-notice',
  },
  {
    key: 'health',
    label: '《健康承诺书》',
    type: 'health',
  },
  {
    key: 'disclaimer',
    label: '《免责协议》',
    type: 'disclaimer',
  },
];

export function formatPrice(price: string): string {
  const num = Number(price);
  if (Number.isNaN(num)) return price;
  if (Number.isInteger(num)) {
    return num.toLocaleString('zh-CN');
  }
  return num.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

export function StatusBarAndNavBar({
  title,
  onBack,
}: {
  title: string;
  onBack: () => void;
}) {
  const statusBarHeight = useStatusBarHeight();
  return (
    <View className='custom-config__header'>
      <View
        className='custom-config__status-bar'
        style={{ height: `${statusBarHeight}px` }}
      />
      <View className='custom-config__navbar'>
        <View className='custom-config__back' onClick={onBack}>
          <Icon name='arrow-left' className='custom-config__back-icon' />
        </View>
        <Text className='custom-config__title'>{title}</Text>
        <View className='custom-config__navbar-placeholder' />
      </View>
    </View>
  );
}

export function SelectedCoachCard({
  coach,
  onClick,
}: {
  coach: PackageDetailCoach;
  onClick: () => void;
}) {
  return (
    <View className='section-card selected-coach-card' onClick={onClick}>
      {coach.avatarUrl ? (
        <Image
          className='selected-coach-card__avatar'
          src={coach.avatarUrl}
          mode='aspectFill'
        />
      ) : (
        <View className='selected-coach-card__avatar selected-coach-card__avatar--placeholder'>
          <Text className='selected-coach-card__avatar-text'>
            {coach.name.charAt(0)}
          </Text>
        </View>
      )}
      <View className='selected-coach-card__info'>
        <View className='selected-coach-card__name-row'>
          <Text className='selected-coach-card__name'>{coach.name}</Text>
          <View className='selected-coach-card__rating'>
            <Icon name='star' className='selected-coach-card__star' />
            <Text className='selected-coach-card__rating-text'>
              {coach.rating}
            </Text>
          </View>
        </View>
        <Text className='selected-coach-card__meta'>
          擅长：{coach.teachingStrokes.join('/')} · 教龄 {coach.teachingYears}{' '}
          年 · 学员 {coach.totalStudents} 人
        </Text>
        <Text className='selected-coach-card__price'>
          参考单价 ¥{formatPrice(coach.referencePrice)}/节
        </Text>
      </View>
      <Icon name='arrow-right' className='selected-coach-card__arrow' />
    </View>
  );
}

export function HoursSelector({
  value,
  inputValue,
  min,
  max,
  error,
  onSelect,
  onInputChange,
  onInputBlur,
  onAdjust,
}: {
  value: number;
  inputValue: string;
  min: number;
  max: number;
  error: string | null;
  onSelect: (hours: number) => void;
  onInputChange: (value: string) => void;
  onInputBlur: () => void;
  onAdjust: (delta: number) => void;
}) {
  return (
    <View className='section-card hours-selector'>
      <SectionTitle title='课时数量' />
      <View className='hours-selector__quick'>
        {QUICK_HOURS.map((item) => {
          const selected = value === item && inputValue === String(item);
          return (
            <View
              key={item}
              className={`hours-selector__option ${selected ? 'hours-selector__option--selected' : ''}`}
              onClick={() => {
                onSelect(item);
                onInputChange(String(item));
              }}
            >
              <Text
                className={`hours-selector__option-text ${selected ? 'hours-selector__option-text--selected' : ''}`}
              >
                {item}节
              </Text>
            </View>
          );
        })}
      </View>
      <View className='hours-selector__custom'>
        <Text className='hours-selector__custom-label'>自定义</Text>
        <View className='hours-selector__custom-controls'>
          <View
            className='hours-selector__adjust hours-selector__adjust--minus'
            onClick={() => onAdjust(-1)}
          >
            <Text className='hours-selector__adjust-icon'>-</Text>
          </View>
          <Input
            className='hours-selector__input'
            type='number'
            value={inputValue}
            data-testid='hours-input'
            onInput={(e) => onInputChange(e.detail.value)}
            onBlur={onInputBlur}
          />
          <View
            className='hours-selector__adjust hours-selector__adjust--plus'
            onClick={() => onAdjust(1)}
          >
            <Text className='hours-selector__adjust-icon'>+</Text>
          </View>
          <Text className='hours-selector__unit'>节</Text>
        </View>
      </View>
      {error ? (
        <Text className='hours-selector__error'>{error}</Text>
      ) : (
        <Text className='hours-selector__hint'>
          可选范围 {min} ~ {max} 节
        </Text>
      )}
    </View>
  );
}

export function ValiditySelector({
  value,
  options,
  onSelect,
}: {
  value: number;
  options: number[];
  onSelect: (days: number) => void;
}) {
  const displayOptions = options.length > 0 ? options : VALIDITY_DAYS;
  return (
    <View className='section-card validity-selector'>
      <SectionTitle title='有效期' />
      <View className='validity-selector__options'>
        {displayOptions.map((item) => {
          const selected = value === item;
          return (
            <View
              key={item}
              className={`validity-selector__option ${selected ? 'validity-selector__option--selected' : ''}`}
              onClick={() => onSelect(item)}
            >
              <Text
                className={`validity-selector__option-text ${selected ? 'validity-selector__option-text--selected' : ''}`}
              >
                {item}天
              </Text>
            </View>
          );
        })}
      </View>
    </View>
  );
}

export function StrokeSelector({
  values,
  options,
  onToggle,
}: {
  values: number[];
  options: StrokeOption[];
  onToggle: (id: number) => void;
}) {
  return (
    <View className='section-card stroke-selector'>
      <SectionTitle title='选择泳姿' />
      <View className='stroke-selector__options'>
        {options.map((stroke) => {
          const selected = values.includes(stroke.id);
          return (
            <View
              key={stroke.id}
              className={`stroke-selector__pill ${selected ? 'stroke-selector__pill--selected' : ''}`}
              onClick={() => onToggle(stroke.id)}
            >
              <Text
                className={`stroke-selector__pill-text ${selected ? 'stroke-selector__pill-text--selected' : ''}`}
              >
                {stroke.label}
              </Text>
            </View>
          );
        })}
      </View>
    </View>
  );
}

export function PriceBreakdown({
  coachName,
  unitPrice,
  hours,
  validDays,
  totalPrice,
}: {
  coachName: string;
  unitPrice: string;
  hours: number;
  validDays: number;
  totalPrice: string;
}) {
  return (
    <View className='section-card price-breakdown'>
      <SectionTitle title='价格明细' />
      <View className='price-breakdown__rows'>
        <PriceRow label='教练' value={coachName} />
        <PriceRow label='参考单价' value={`¥${formatPrice(unitPrice)} / 节`} />
        <PriceRow label='课时数量' value={`${hours} 节`} />
        <PriceRow label='有效期' value={`${validDays} 天`} />
        <View className='price-breakdown__divider' />
        <View className='price-breakdown__row price-breakdown__row--total'>
          <Text className='price-breakdown__label price-breakdown__label--total'>
            合计
          </Text>
          <Text className='price-breakdown__value price-breakdown__value--total'>
            ¥{formatPrice(totalPrice)}
          </Text>
        </View>
      </View>
    </View>
  );
}

function PriceRow({ label, value }: { label: string; value: string }) {
  return (
    <View className='price-breakdown__row'>
      <Text className='price-breakdown__label'>{label}</Text>
      <Text className='price-breakdown__value'>{value}</Text>
    </View>
  );
}

export function AgreementSection({
  values,
  onToggle,
}: {
  values: Record<AgreementKey, boolean>;
  onToggle: (key: AgreementKey) => void;
}) {
  function navigateToTerms(type: string) {
    void Taro.navigateTo({
      url: `${PAGE_PATHS.termsDetail}?type=${encodeURIComponent(type)}`,
    });
  }

  return (
    <View className='section-card agreement-section'>
      <Text className='agreement-section__title'>购买协议</Text>
      <View className='agreement-section__list'>
        {AGREEMENT_ITEMS.map((item) => {
          const checked = values[item.key];
          return (
            <View key={item.key} className='agreement-section__item'>
              <View
                className={`agreement-section__checkbox ${checked ? 'agreement-section__checkbox--checked' : ''}`}
                role='checkbox'
                aria-checked={checked}
                data-testid={`agreement-checkbox-${item.key}`}
                onClick={() => onToggle(item.key)}
              >
                {checked && (
                  <Icon
                    name='check'
                    className='agreement-section__check-icon'
                  />
                )}
              </View>
              <View className='agreement-section__text-wrap'>
                <Text className='agreement-section__text'>我已阅读并同意</Text>
                <Text
                  className='agreement-section__link'
                  onClick={() => navigateToTerms(item.type)}
                >
                  {item.label}
                </Text>
              </View>
            </View>
          );
        })}
      </View>
    </View>
  );
}

export function GuardianSection({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <View className='section-card guardian-section'>
      <Text className='guardian-section__title'>监护人信息</Text>
      <Text className='guardian-section__subtitle'>
        您为未成年人，购买正价套餐需填写监护人手机号
      </Text>
      <View className='guardian-section__input-wrap'>
        <Icon name='phone' className='guardian-section__input-icon' />
        <Input
          className='guardian-section__input'
          type='number'
          placeholder='请输入监护人手机号'
          value={value}
          onInput={(e) => onChange(e.detail.value)}
          maxlength={11}
        />
      </View>
    </View>
  );
}

export function BottomSubmitBar({
  totalPrice,
  disabled,
  loading,
  onSubmit,
}: {
  totalPrice: string;
  disabled: boolean;
  loading: boolean;
  onSubmit: () => void;
}) {
  return (
    <View className='bottom-submit-bar'>
      <View className='bottom-submit-bar__total'>
        <Text className='bottom-submit-bar__symbol'>¥</Text>
        <Text className='bottom-submit-bar__price'>
          {formatPrice(totalPrice)}
        </Text>
      </View>
      <View
        className={`bottom-submit-bar__button ${disabled ? 'bottom-submit-bar__button--disabled' : ''}`}
        data-testid='submit-button'
        onClick={!disabled ? onSubmit : undefined}
      >
        <Text className='bottom-submit-bar__button-text'>
          {loading ? '提交中...' : '提交订单'}
        </Text>
        <Icon name='arrow-right' className='bottom-submit-bar__button-icon' />
      </View>
    </View>
  );
}

export function ConflictBanner({
  onChangeCoach,
}: {
  onChangeCoach: () => void;
}) {
  return (
    <View className='conflict-banner'>
      <Icon name='warning' className='conflict-banner__icon' />
      <Text className='conflict-banner__text'>
        您已绑定其他教练，需先退订才能购买当前套餐
      </Text>
      <Text className='conflict-banner__action' onClick={onChangeCoach}>
        更换教练
      </Text>
    </View>
  );
}

export function CoachUnavailableBanner({ reason }: { reason: string }) {
  return (
    <View className='coach-unavailable-banner'>
      <Icon name='warning' className='coach-unavailable-banner__icon' />
      <Text className='coach-unavailable-banner__text'>{reason}</Text>
    </View>
  );
}

function SectionTitle({ title }: { title: string }) {
  return (
    <View className='section-title'>
      <View className='section-title__decoration' />
      <Text className='section-title__text'>{title}</Text>
    </View>
  );
}

export function CustomConfigSkeleton({ onBack }: { onBack: () => void }) {
  return (
    <View
      className='custom-config custom-config--skeleton'
      data-testid='custom-config-skeleton'
    >
      <StatusBarAndNavBar title='自定义套餐' onBack={onBack} />
      <View className='custom-config__content'>
        <View className='skeleton-card' />
        <View className='skeleton-card' />
        <View className='skeleton-card' />
        <View className='skeleton-card' />
      </View>
    </View>
  );
}
