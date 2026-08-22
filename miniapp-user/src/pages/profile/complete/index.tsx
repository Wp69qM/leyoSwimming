import { useEffect, useRef, useState } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Input, Button, Image, Textarea } from '@tarojs/components';
import {
  getProfile,
  updateProfile,
  uploadAvatar,
  type UserProfile,
  type UpdateProfileParams,
} from '@/api/profile';
import { handleBusinessError, getErrorCode } from '@/api/request';
import { useAuthStore } from '@/stores/authStore';
import './index.scss';

const SWIM_BASIS_OPTIONS = [
  { value: 'yes', label: '是' },
  { value: 'no', label: '否' },
];

const SWIM_STROKES = [
  { value: 'breaststroke', label: '蛙泳' },
  { value: 'freestyle', label: '自由泳' },
  { value: 'backstroke', label: '仰泳' },
  { value: 'butterfly', label: '蝶泳' },
];

const GENDER_OPTIONS = [
  { value: 'male', label: '男' },
  { value: 'female', label: '女' },
];

interface FieldErrors {
  avatarUrl?: string;
  phone?: string;
  name?: string;
  age?: string;
  gender?: string;
  guardianName?: string;
  guardianPhone?: string;
  swimStrokes?: string;
}

const SYSTEM_INFO = Taro.getSystemInfoSync();
const STATUS_BAR_HEIGHT = SYSTEM_INFO.statusBarHeight || 0;
const NAV_BAR_HEIGHT = 44;

export default function ProfileCompletePage() {
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [profile, setProfile] = useState<UserProfile | null>(null);

  const [name, setName] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [age, setAge] = useState('');
  const [gender, setGender] = useState<'male' | 'female' | ''>('');
  const [guardianName, setGuardianName] = useState('');
  const [guardianPhone, setGuardianPhone] = useState('');
  const [swimBasisLevel, setSwimBasisLevel] = useState<'yes' | 'no' | ''>('');
  const [swimStrokes, setSwimStrokes] = useState<string[]>([]);
  const [swimYears, setSwimYears] = useState('');
  const [personalDesc, setPersonalDesc] = useState('');
  const [errors, setErrors] = useState<FieldErrors>({});
  const [globalError, setGlobalError] = useState('');
  const [isEditMode, setIsEditMode] = useState(false);

  const setUserInfo = useAuthStore((state) => state.setUserInfo);
  const userInfo = useAuthStore((state) => state.userInfo);
  const navigateTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    Taro.setNavigationBarTitle({
      title: userInfo?.profileCompleted ? '编辑资料' : '完善资料',
    });
  }, [userInfo?.profileCompleted]);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const data = await getProfile();
        if (cancelled) return;
        setProfile(data);
        setIsEditMode(!!data.profileCompleted);
        setName(data.name || '');
        setAvatarUrl(data.avatarUrl || '');
        setAge(data.age ? String(data.age) : '');
        setGender(data.gender || '');
        setGuardianName(data.guardianName || '');
        setGuardianPhone(data.guardianPhone || '');
        setSwimBasisLevel(
          data.hasSwimBasis === undefined
            ? ''
            : data.hasSwimBasis
              ? 'yes'
              : 'no'
        );
        setSwimStrokes(data.swimStrokes || []);
        setSwimYears(data.swimYears ? String(data.swimYears) : '');
        setPersonalDesc(data.personalDesc || '');
      } catch (error) {
        if (cancelled) return;
        setGlobalError(handleBusinessError(error));
      } finally {
        if (!cancelled) setInitialLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
      if (navigateTimerRef.current) {
        clearTimeout(navigateTimerRef.current);
      }
    };
  }, []);

  function clearFieldError(field: keyof FieldErrors) {
    setErrors((prev) => {
      if (!prev[field]) return prev;
      const next = { ...prev };
      delete next[field];
      return next;
    });
  }

  function toggleStroke(value: string) {
    setSwimStrokes((prev) =>
      prev.includes(value) ? prev.filter((v) => v !== value) : [...prev, value]
    );
    clearFieldError('swimStrokes');
  }

  async function handleChooseAvatar() {
    try {
      const res = await Taro.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
      });
      if (!res.tempFilePaths || res.tempFilePaths.length === 0) return;
      const tempPath = res.tempFilePaths[0];
      const url = await uploadAvatar(tempPath);
      setAvatarUrl(url);
      clearFieldError('avatarUrl');
    } catch (error) {
      Taro.showToast({ title: handleBusinessError(error), icon: 'none' });
    }
  }

  const phoneNumber = profile?.phone || '';

  function validate(): FieldErrors | null {
    const next: FieldErrors = {};
    if (!avatarUrl) next.avatarUrl = '请上传头像';
    if (!/^1[3-9]\d{9}$/.test(phoneNumber)) next.phone = '请输入正确的手机号';
    if (!name.trim()) next.name = '姓名不能为空';
    else if (!/^[^\s]{1,32}$/.test(name.trim()))
      next.name = '姓名长度不超过32个字符';
    const ageNum = Number(age);
    if (!age || ageNum < 3 || ageNum > 99) next.age = '请输入正确的年龄';
    if (!gender) next.gender = '请选择性别';
    if (ageNum < 18) {
      if (!guardianName.trim()) next.guardianName = '请输入监护人姓名';
      if (!/^1[3-9]\d{9}$/.test(guardianPhone))
        next.guardianPhone = '请输入正确的监护人手机号';
    }
    if (swimBasisLevel === 'yes' && swimStrokes.length === 0)
      next.swimStrokes = '请至少选择一种会游的泳姿';
    return Object.keys(next).length > 0 ? next : null;
  }

  const isFormValid = !validate();

  async function handleSubmit() {
    setGlobalError('');
    const fieldErrors = validate();
    if (fieldErrors) {
      setErrors(fieldErrors);
      return;
    }
    setLoading(true);
    try {
      const hasSwimBasis = swimBasisLevel === 'yes';
      const params: UpdateProfileParams = {
        name: name.trim(),
        age: Number(age),
        gender: gender as 'male' | 'female',
        hasSwimBasis,
        swimStrokes: hasSwimBasis ? swimStrokes : undefined,
        swimYears: hasSwimBasis && swimYears ? Number(swimYears) : undefined,
        personalDesc: personalDesc.trim() || undefined,
        avatarUrl: avatarUrl || undefined,
        idempotencyKey: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
      };
      if (profile?.phone) {
        params.phone = profile.phone;
      }
      if (Number(age) < 18) {
        params.guardianName = guardianName.trim();
        params.guardianPhone = guardianPhone;
      }
      const data = await updateProfile(params);
      setUserInfo({
        userId: data.id,
        phone: data.phone,
        avatarUrl: data.avatarUrl,
        nickName: data.name,
        profileCompleted: true,
      });
      Taro.showToast({ title: '保存成功', icon: 'success' });
      navigateTimerRef.current = setTimeout(() => {
        if (isEditMode) {
          Taro.navigateBack();
        } else {
          Taro.switchTab({ url: '/pages/index/index' });
        }
      }, 800);
    } catch (err) {
      const code = getErrorCode(err);
      const message = handleBusinessError(err);
      setGlobalError(message);
      if (code === 440005) {
        Taro.showToast({ title: '姓名或描述包含敏感词', icon: 'none' });
      }
    } finally {
      setLoading(false);
    }
  }

  function handleBack() {
    Taro.navigateBack();
  }

  if (initialLoading) {
    return (
      <View className='profile-complete'>
        <View
          className='profile-complete__navbar'
          style={{ paddingTop: `${STATUS_BAR_HEIGHT}px` }}
        >
          <View
            className='profile-complete__navbar-inner'
            style={{ height: `${NAV_BAR_HEIGHT}px` }}
          >
            <Text className='profile-complete__navbar-title'>
              {isEditMode ? '编辑资料' : '完善资料'}
            </Text>
          </View>
        </View>
        <Text className='profile-complete__loading'>加载中…</Text>
      </View>
    );
  }

  const ageNum = Number(age) || 0;
  const showSwimDetail = swimBasisLevel === 'yes';
  const pageTitle = isEditMode ? '编辑资料' : '完善资料';

  return (
    <View className='profile-complete'>
      <View
        className='profile-complete__navbar'
        style={{ paddingTop: `${STATUS_BAR_HEIGHT}px` }}
      >
        <View
          className='profile-complete__navbar-inner'
          style={{ height: `${NAV_BAR_HEIGHT}px` }}
        >
          {isEditMode && (
            <View
              className='profile-complete__navbar-back'
              onClick={handleBack}
            >
              <Text className='profile-complete__navbar-back-icon'>‹</Text>
            </View>
          )}
          <Text className='profile-complete__navbar-title'>{pageTitle}</Text>
        </View>
      </View>

      <View className='profile-complete__body'>
        <View className='profile-complete__avatar-wrap'>
          <View
            className={`profile-complete__avatar ${errors.avatarUrl ? 'profile-complete__avatar--error' : ''}`}
            onClick={handleChooseAvatar}
          >
            {avatarUrl ? (
              <Image
                className='profile-complete__avatar-img'
                src={avatarUrl}
                mode='aspectFill'
              />
            ) : (
              <View className='profile-complete__avatar-placeholder' />
            )}
            <View className='profile-complete__camera'>
              <View className='profile-complete__camera-icon' />
            </View>
          </View>
          <Text
            className={`profile-complete__avatar-tip ${errors.avatarUrl ? 'profile-complete__avatar-tip--error' : ''}`}
          >
            {avatarUrl ? '点击更换头像' : '点击上传头像'}
          </Text>
        </View>

        <View className='profile-complete__card'>
          <View className='profile-complete__field'>
            <Text className='profile-complete__label'>
              手机号<Text className='profile-complete__required'>*</Text>
            </Text>
            <Input
              className={`profile-complete__input ${errors.phone ? 'profile-complete__input--error' : ''}`}
              type='number'
              placeholder='登录时绑定的手机号'
              value={profile?.phone || ''}
              onInput={(e) => {
                clearFieldError('phone');
                if (profile) {
                  setProfile({
                    ...profile,
                    phone: e.detail.value.replace(/\D/g, '').slice(0, 11),
                  });
                }
              }}
              maxlength={11}
            />
            {errors.phone && (
              <Text className='profile-complete__field-error'>
                {errors.phone}
              </Text>
            )}
          </View>

          <View className='profile-complete__field'>
            <Text className='profile-complete__label'>
              姓名<Text className='profile-complete__required'>*</Text>
            </Text>
            <Input
              className={`profile-complete__input ${errors.name ? 'profile-complete__input--error' : ''}`}
              placeholder='请输入真实姓名'
              value={name}
              onInput={(e) => {
                setName(e.detail.value);
                clearFieldError('name');
              }}
              maxlength={32}
            />
            {errors.name && (
              <Text className='profile-complete__field-error'>
                {errors.name}
              </Text>
            )}
          </View>

          <View className='profile-complete__field'>
            <Text className='profile-complete__label'>
              年龄<Text className='profile-complete__required'>*</Text>
            </Text>
            <Input
              className={`profile-complete__input ${errors.age ? 'profile-complete__input--error' : ''}`}
              type='number'
              placeholder='请输入年龄（3-99）'
              value={age}
              onInput={(e) => {
                setAge(e.detail.value.replace(/\D/g, '').slice(0, 2));
                clearFieldError('age');
              }}
              maxlength={2}
            />
            {errors.age && (
              <Text className='profile-complete__field-error'>
                {errors.age}
              </Text>
            )}
          </View>

          <View className='profile-complete__field'>
            <Text className='profile-complete__label'>
              性别<Text className='profile-complete__required'>*</Text>
            </Text>
            <View className='profile-complete__radio-group'>
              {GENDER_OPTIONS.map((option) => (
                <View
                  key={option.value}
                  className={`profile-complete__radio ${gender === option.value ? 'profile-complete__radio--active' : ''} ${errors.gender ? 'profile-complete__radio--error' : ''}`}
                  onClick={() => {
                    setGender(option.value as 'male' | 'female');
                    clearFieldError('gender');
                  }}
                >
                  <Text className='profile-complete__radio-text'>
                    {option.label}
                  </Text>
                </View>
              ))}
            </View>
            {errors.gender && (
              <Text className='profile-complete__field-error'>
                {errors.gender}
              </Text>
            )}
          </View>

          {ageNum > 0 && ageNum < 18 && (
            <>
              <View className='profile-complete__guardian-header'>
                <View className='profile-complete__guardian-icon' />
                <Text className='profile-complete__guardian-title'>
                  监护人信息（未成年人必填）
                </Text>
              </View>
              <View className='profile-complete__field'>
                <Text className='profile-complete__label'>
                  监护人姓名
                  <Text className='profile-complete__required'>*</Text>
                </Text>
                <Input
                  className={`profile-complete__input ${errors.guardianName ? 'profile-complete__input--error' : ''}`}
                  placeholder='请输入监护人真实姓名'
                  value={guardianName}
                  onInput={(e) => {
                    setGuardianName(e.detail.value);
                    clearFieldError('guardianName');
                  }}
                  maxlength={32}
                />
                {errors.guardianName && (
                  <Text className='profile-complete__field-error'>
                    {errors.guardianName}
                  </Text>
                )}
              </View>
              <View className='profile-complete__field'>
                <Text className='profile-complete__label'>
                  监护人手机号
                  <Text className='profile-complete__required'>*</Text>
                </Text>
                <Input
                  className={`profile-complete__input ${errors.guardianPhone ? 'profile-complete__input--error' : ''}`}
                  type='number'
                  placeholder='请输入监护人手机号'
                  value={guardianPhone}
                  onInput={(e) => {
                    setGuardianPhone(
                      e.detail.value.replace(/\D/g, '').slice(0, 11)
                    );
                    clearFieldError('guardianPhone');
                  }}
                  maxlength={11}
                />
                {errors.guardianPhone && (
                  <Text className='profile-complete__field-error'>
                    {errors.guardianPhone}
                  </Text>
                )}
              </View>
            </>
          )}

          <View className='profile-complete__field'>
            <Text className='profile-complete__label'>是否有游泳基础</Text>
            <View className='profile-complete__radio-group'>
              {SWIM_BASIS_OPTIONS.map((option) => (
                <View
                  key={option.value}
                  className={`profile-complete__radio ${swimBasisLevel === option.value ? 'profile-complete__radio--active' : ''}`}
                  onClick={() => {
                    setSwimBasisLevel(option.value as 'yes' | 'no');
                    if (option.value === 'no') {
                      setSwimStrokes([]);
                      setSwimYears('');
                    }
                  }}
                >
                  <Text className='profile-complete__radio-text'>
                    {option.label}
                  </Text>
                </View>
              ))}
            </View>
          </View>

          {showSwimDetail && (
            <View className='profile-complete__field'>
              <Text className='profile-complete__label'>会游哪些泳姿</Text>
              <View className='profile-complete__stroke-list'>
                {SWIM_STROKES.map((stroke) => (
                  <View
                    key={stroke.value}
                    className={`profile-complete__stroke ${swimStrokes.includes(stroke.value) ? 'profile-complete__stroke--active' : ''} ${errors.swimStrokes ? 'profile-complete__stroke--error' : ''}`}
                    onClick={() => toggleStroke(stroke.value)}
                  >
                    <Text className='profile-complete__stroke-text'>
                      {stroke.label}
                    </Text>
                  </View>
                ))}
              </View>
              {errors.swimStrokes && (
                <Text className='profile-complete__field-error'>
                  {errors.swimStrokes}
                </Text>
              )}
              <Input
                className='profile-complete__input profile-complete__input--mt'
                type='number'
                placeholder='请输入游泳年限（年）'
                value={swimYears}
                onInput={(e) =>
                  setSwimYears(e.detail.value.replace(/\D/g, '').slice(0, 2))
                }
              />
            </View>
          )}

          <View className='profile-complete__field profile-complete__field--last'>
            <Text className='profile-complete__label'>
              个人描述
              <Text className='profile-complete__label-note'>（选填）</Text>
            </Text>
            <Textarea
              className='profile-complete__textarea'
              placeholder='可填写游泳目标、身体状况、特殊需求等，方便教练备课'
              value={personalDesc}
              onInput={(e) => setPersonalDesc(e.detail.value)}
              maxlength={200}
            />
            <Text className='profile-complete__counter'>
              {personalDesc.length}/200
            </Text>
          </View>
        </View>
      </View>

      <View className='profile-complete__footer'>
        {globalError && (
          <View className='profile-complete__error'>
            <Text className='profile-complete__error-text'>{globalError}</Text>
          </View>
        )}
        <Button
          className={`profile-complete__submit ${loading ? 'profile-complete__submit--loading' : ''} ${!isFormValid ? 'profile-complete__submit--disabled' : ''}`}
          onClick={handleSubmit}
          loading={loading}
          disabled={loading || !isFormValid}
        >
          {loading ? '保存中…' : isEditMode ? '保存' : '保存并进入首页'}
        </Button>
      </View>
    </View>
  );
}
