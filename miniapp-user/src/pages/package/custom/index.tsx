import { useCallback, useEffect, useMemo, useState } from 'react';
import { View, Text } from '@tarojs/components';
import { Icon } from '@/components/common/Icon';
import Taro from '@tarojs/taro';
import { createFormalOrder } from '@/api/order';
import {
  fetchActivePackage,
  fetchCustomPackageConfig,
  fetchPackageDetail,
} from '@/api/package';
import { getProfile } from '@/api/profile';
import { getErrorCode, handleBusinessError } from '@/api/request';
import { getPageQuery } from '@/utils/router';
import type {
  CustomPackageConfig,
  PackageDetailCoach,
  UserActivePackage,
} from '@/types/package';
import type { UserProfile } from '@/api/profile';

import {
  AgreementSection,
  BottomSubmitBar,
  COACH_STATUS_ACTIVE,
  CoachUnavailableBanner,
  ConflictBanner,
  CustomConfigSkeleton,
  GuardianSection,
  HoursSelector,
  PAGE_PATHS,
  PriceBreakdown,
  QUICK_HOURS,
  SelectedCoachCard,
  StatusBarAndNavBar,
  StrokeSelector,
  ValiditySelector,
  buildStrokeOptions,
} from './components';
import type { AgreementKey, StrokeOption } from './components';

import './index.scss';

const AGREEMENT_VERSIONS = {
  userNotice: '1.0.0',
  health: '1.0.0',
  disclaimer: '1.0.0',
};

export default function CustomPackageConfigPage() {
  const [detail, setDetail] = useState<PackageDetailCoach | null>(null);
  const [config, setConfig] = useState<CustomPackageConfig | null>(null);
  const [strokeOptions, setStrokeOptions] = useState<StrokeOption[]>([]);
  const [warning, setWarning] = useState<string | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [activePackage, setActivePackage] = useState<UserActivePackage | null>(
    null
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const [hours, setHours] = useState(8);
  const [hoursInput, setHoursInput] = useState('8');
  const [validDays, setValidDays] = useState(90);
  const [selectedStrokes, setSelectedStrokes] = useState<number[]>([]);
  const [agreements, setAgreements] = useState<Record<AgreementKey, boolean>>({
    userNotice: false,
    health: false,
    disclaimer: false,
  });
  const [guardianPhone, setGuardianPhone] = useState('');

  const params = useMemo(() => getPageQuery(), []);
  const packageId = Number(params.packageId);
  const coachId = Number(params.coachId);

  function resolveInitialHours(configData: CustomPackageConfig) {
    const targetDefaultHours = 8;
    const candidate =
      QUICK_HOURS.find(
        (h) => h >= configData.minHours && h >= targetDefaultHours
      ) ?? Math.max(configData.minHours, targetDefaultHours);
    return Math.min(candidate, configData.maxHours);
  }

  function resolveInitialDays(configData: CustomPackageConfig) {
    return (
      configData.allowedValidDays.find(
        (d) => d === configData.defaultValidDays
      ) ??
      configData.allowedValidDays.find(
        (d) => d >= configData.defaultValidDays
      ) ??
      configData.allowedValidDays[0] ??
      configData.defaultValidDays
    );
  }

  function resolveDefaultStrokes(
    matchedCoach: PackageDetailCoach,
    options: StrokeOption[]
  ) {
    const defaultStrokes = matchedCoach.teachingStrokes
      .map((label) => options.find((s) => s.label === label)?.id)
      .filter((id): id is number => id !== undefined);
    return defaultStrokes.length > 0 ? defaultStrokes : [options[0].id];
  }

  const loadData = useCallback(async () => {
    async function loadPackageAndConfig() {
      const [packageData, configData] = await Promise.all([
        fetchPackageDetail(packageId, coachId),
        fetchCustomPackageConfig(),
      ]);

      const matchedCoach = packageData.applicableCoaches.find(
        (c) => c.coachId === coachId
      );
      if (!matchedCoach) {
        throw new Error('教练信息不存在');
      }

      const options = buildStrokeOptions(matchedCoach.teachingStrokes);
      const initialHours = resolveInitialHours(configData);
      const initialDays = resolveInitialDays(configData);

      setDetail(matchedCoach);
      setConfig(configData);
      setStrokeOptions(options);
      setHours(initialHours);
      setHoursInput(String(initialHours));
      setValidDays(initialDays);
      setSelectedStrokes(resolveDefaultStrokes(matchedCoach, options));

      if (matchedCoach.status !== COACH_STATUS_ACTIVE) {
        setWarning('该教练当前不可购买，请更换教练或稍后再试');
      }
    }

    async function loadProfileAndActivePackage() {
      const [profileData, activePackageData] = await Promise.all([
        getProfile().catch(() => null),
        fetchActivePackage().catch(() => null),
      ]);
      setProfile(profileData);
      setActivePackage(activePackageData);
    }

    const isValidPackageId = packageId === -1 || packageId > 0;
    if (
      !isValidPackageId ||
      Number.isNaN(coachId) ||
      coachId <= 0
    ) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);
    setWarning(null);
    try {
      await Promise.all([
        loadPackageAndConfig(),
        loadProfileAndActivePackage(),
      ]);
    } catch (err) {
      const code = getErrorCode(err);
      if (code === 420001 || code === 420106) {
        setWarning('该套餐已下架或不可用，请重新选择');
      } else {
        setError(handleBusinessError(err));
      }
    } finally {
      setLoading(false);
    }
  }, [packageId, coachId]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const isMinor = useMemo(() => {
    if (!profile) return false;
    return typeof profile.age === 'number' && profile.age < 18;
  }, [profile]);

  const hoursError = useMemo(() => {
    if (!config) return null;
    if (hours < config.minHours) {
      return `最少购买 ${config.minHours} 节`;
    }
    if (hours > config.maxHours) {
      return `最多购买 ${config.maxHours} 节`;
    }
    return null;
  }, [hours, config]);

  const totalPrice = useMemo(() => {
    if (!detail) return '0';
    return (Number(detail.referencePrice) * hours).toFixed(2);
  }, [detail, hours]);

  const isConflict = useMemo(() => {
    if (!activePackage) return false;
    return (
      activePackage.status === 'active' && activePackage.coachId !== coachId
    );
  }, [activePackage, coachId]);

  const canSubmit = useMemo(() => {
    if (!detail || !config || submitting) return false;
    if (warning) return false;
    if (isConflict) return false;
    if (hoursError) return false;
    if (selectedStrokes.length === 0) return false;
    if (!Object.values(agreements).every(Boolean)) return false;
    if (isMinor && !guardianPhone) return false;
    return true;
  }, [
    detail,
    config,
    submitting,
    warning,
    isConflict,
    hoursError,
    selectedStrokes,
    agreements,
    isMinor,
    guardianPhone,
  ]);

  function navigateBack() {
    void Taro.navigateBack();
  }

  function navigateToHome() {
    void Taro.switchTab({ url: PAGE_PATHS.home });
  }

  function navigateToCoachList() {
    void Taro.switchTab({ url: PAGE_PATHS.coachList });
  }

  function navigateToCoachDetail() {
    if (!detail) return;
    void Taro.navigateTo({
      url: `${PAGE_PATHS.coachDetail}?id=${detail.coachId}`,
    });
  }

  function handleHoursChange(value: string) {
    const filtered = value.replace(/\D/g, '').replace(/^0+(?=\d)/, '');
    setHoursInput(filtered);
    const num = Number(filtered);
    if (filtered !== '' && !Number.isNaN(num)) {
      setHours(num);
    }
  }

  function handleHoursBlur() {
    if (!config) return;
    if (hoursInput === '') {
      setHours(config.minHours);
      setHoursInput(String(config.minHours));
      return;
    }
    const num = Math.min(
      Math.max(Number(hoursInput), config.minHours),
      config.maxHours
    );
    setHours(num);
    setHoursInput(String(num));
  }

  function adjustHours(delta: number) {
    if (!config) return;
    const next = hours + delta;
    if (next < config.minHours || next > config.maxHours) return;
    setHours(next);
    setHoursInput(String(next));
  }

  function toggleStroke(id: number) {
    setSelectedStrokes((prev) => {
      if (prev.includes(id)) {
        const next = prev.filter((item) => item !== id);
        if (next.length === 0) {
          void Taro.showToast({
            title: '至少选择一种泳姿',
            icon: 'none',
          });
          return prev;
        }
        return next;
      }
      return [...prev, id];
    });
  }

  function toggleAgreement(key: AgreementKey) {
    setAgreements((prev) => ({ ...prev, [key]: !prev[key] }));
  }

  function filterGuardianPhone(value: string) {
    return value.replace(/\D/g, '').slice(0, 11);
  }

  async function handleSubmit() {
    if (!canSubmit || !detail) return;

    if (!Object.values(agreements).every(Boolean)) {
      void Taro.showToast({
        title: '请先阅读并同意协议',
        icon: 'none',
      });
      return;
    }

    if (isMinor && !guardianPhone) {
      void Taro.showToast({
        title: '请填写监护人手机号',
        icon: 'none',
      });
      return;
    }

    if (isMinor && !/^1[3-9]\d{9}$/.test(guardianPhone)) {
      void Taro.showToast({
        title: '请填写正确的监护人手机号',
        icon: 'none',
      });
      return;
    }

    setSubmitting(true);
    try {
      const result = await createFormalOrder({
        packageId,
        coachId,
        hours,
        validDays,
        strokeIds: selectedStrokes,
        agreementVersions: AGREEMENT_VERSIONS,
        guardianPhone: isMinor ? guardianPhone : undefined,
      });

      void Taro.navigateTo({
        url: `${PAGE_PATHS.payment}?orderId=${result.orderId}`,
      });
    } catch (err) {
      void Taro.showToast({
        title: handleBusinessError(err),
        icon: 'none',
      });
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <CustomConfigSkeleton onBack={navigateBack} />;
  }

  if (error) {
    return (
      <View className='custom-config-error'>
        <StatusBarAndNavBar title='自定义套餐' onBack={navigateBack} />
        <View className='custom-config-error__content'>
          <View className='custom-config-error__illustration'>
            <Icon
              name='error-circle'
              className='custom-config-error__illustration-icon'
            />
          </View>
          <Text className='custom-config-error__text'>{error}</Text>
          <View className='custom-config-error__retry' onClick={loadData}>
            重新加载
          </View>
        </View>
      </View>
    );
  }

  if (!detail || !config) {
    return (
      <View className='custom-config-empty'>
        <StatusBarAndNavBar title='自定义套餐' onBack={navigateBack} />
        <View className='custom-config-empty__content'>
          {warning && (
            <View className='custom-config-empty__warning'>
              <Icon
                name='warning'
                className='custom-config-empty__warning-icon'
              />
              <View className='custom-config-empty__warning-text'>
                {warning}
              </View>
            </View>
          )}
          <View className='custom-config-empty__illustration'>
            <Icon
              name='empty'
              className='custom-config-empty__illustration-icon'
            />
          </View>
          <View className='custom-config-empty__title'>暂无配置信息</View>
          <Text
            className='custom-config-empty__action'
            onClick={navigateToHome}
          >
            返回首页
          </Text>
        </View>
      </View>
    );
  }

  return (
    <View className='custom-config'>
      <StatusBarAndNavBar title='自定义套餐' onBack={navigateBack} />

      <View className='custom-config__content'>
        {isConflict && <ConflictBanner onChangeCoach={navigateToCoachList} />}
        {warning && <CoachUnavailableBanner reason={warning} />}
        <SelectedCoachCard coach={detail} onClick={navigateToCoachDetail} />
        <HoursSelector
          value={hours}
          inputValue={hoursInput}
          min={config.minHours}
          max={config.maxHours}
          error={hoursError}
          onSelect={setHours}
          onInputChange={handleHoursChange}
          onInputBlur={handleHoursBlur}
          onAdjust={adjustHours}
        />
        <ValiditySelector
          value={validDays}
          options={config.allowedValidDays}
          onSelect={setValidDays}
        />
        <StrokeSelector
          values={selectedStrokes}
          options={strokeOptions}
          onToggle={toggleStroke}
        />
        <PriceBreakdown
          coachName={detail.name}
          unitPrice={detail.referencePrice}
          hours={hours}
          validDays={validDays}
          totalPrice={totalPrice}
        />
        {isMinor && (
          <GuardianSection
            value={guardianPhone}
            onChange={(value) => setGuardianPhone(filterGuardianPhone(value))}
          />
        )}
        <AgreementSection values={agreements} onToggle={toggleAgreement} />
      </View>

      <BottomSubmitBar
        totalPrice={totalPrice}
        disabled={!canSubmit}
        loading={submitting}
        onSubmit={handleSubmit}
      />
    </View>
  );
}
