import { useEffect, useRef, useState } from 'react';
import Taro from '@tarojs/taro';
import { View, Text, Input, Button, Image, Switch } from '@tarojs/components';
import { getProfile, updateProfile, uploadAvatar, type UserProfile, type UpdateProfileParams } from '@/api/profile';
import { sendSmsCode } from '@/api/common';
import { handleBusinessError, getErrorCode } from '@/api/request';
import { useAuthStore } from '@/stores/authStore';
import { useCountdown } from '@/hooks/useCountdown';
import './index.scss';

const SWIM_STROKES = [
  { value: 'breaststroke', label: '蛙泳' },
  { value: 'freestyle', label: '自由泳' },
  { value: 'backstroke', label: '仰泳' },
  { value: 'butterfly', label: '蝶泳' },
];

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
  const [hasSwimBasis, setHasSwimBasis] = useState(false);
  const [swimStrokes, setSwimStrokes] = useState<string[]>([]);
  const [swimYears, setSwimYears] = useState('');
  const [personalDesc, setPersonalDesc] = useState('');
  const [newPhone, setNewPhone] = useState('');
  const [oldCode, setOldCode] = useState('');
  const [newCode, setNewCode] = useState('');
  const [errorTip, setErrorTip] = useState('');

  const setUserInfo = useAuthStore((state) => state.setUserInfo);
  const oldTimer = useCountdown({ initialSeconds: 60 });
  const newTimer = useCountdown({ initialSeconds: 60 });
  const navigateTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const data = await getProfile();
        if (cancelled) return;
        setProfile(data);
        setName(data.name || '');
        setAvatarUrl(data.avatarUrl || '');
        setAge(data.age ? String(data.age) : '');
        setGender(data.gender || '');
        setGuardianName(data.guardianName || '');
        setGuardianPhone(data.guardianPhone || '');
        setHasSwimBasis(Boolean(data.hasSwimBasis));
        setSwimStrokes(data.swimStrokes || []);
        setSwimYears(data.swimYears ? String(data.swimYears) : '');
        setPersonalDesc(data.personalDesc || '');
      } catch (error) {
        if (cancelled) return;
        setErrorTip(handleBusinessError(error));
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

  function toggleStroke(value: string) {
    setSwimStrokes((prev) =>
      prev.includes(value) ? prev.filter((v) => v !== value) : [...prev, value]
    );
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
    } catch (error) {
      Taro.showToast({ title: handleBusinessError(error), icon: 'none' });
    }
  }

  async function handleSendOldCode() {
    if (!profile?.phone) return;
    try {
      await sendSmsCode({ phone: profile.phone, scene: 'change_phone_old' });
      oldTimer.start();
    } catch (error) {
      Taro.showToast({ title: handleBusinessError(error), icon: 'none' });
    }
  }

  async function handleSendNewCode() {
    if (!/^1[3-9]\d{9}$/.test(newPhone)) {
      Taro.showToast({ title: '请输入正确的新手机号', icon: 'none' });
      return;
    }
    try {
      await sendSmsCode({ phone: newPhone, scene: 'change_phone_new' });
      newTimer.start();
    } catch (error) {
      Taro.showToast({ title: handleBusinessError(error), icon: 'none' });
    }
  }

  function validate(): string | null {
    if (!name.trim()) return '请输入昵称';
    if (!/^[^\s]{1,32}$/.test(name.trim())) return '昵称长度不超过32个字符';
    const ageNum = Number(age);
    if (!age || ageNum < 3 || ageNum > 99) return '年龄需在 3-99 岁之间';
    if (!gender) return '请选择性别';
    if (ageNum < 18) {
      if (!guardianName.trim()) return '请输入监护人姓名';
      if (!/^1[3-9]\d{9}$/.test(guardianPhone)) return '请输入正确的监护人手机号';
    }
    if (hasSwimBasis && swimStrokes.length === 0) return '请至少选择一种会游的泳姿';
    if (newPhone && !/^1[3-9]\d{9}$/.test(newPhone)) return '请输入正确的新手机号';
    if (newPhone && (!oldCode || !newCode)) return '请填写新旧手机号的验证码';
    return null;
  }

  async function handleSubmit() {
    setErrorTip('');
    const error = validate();
    if (error) {
      setErrorTip(error);
      return;
    }
    setLoading(true);
    try {
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
      if (age && Number(age) < 18) {
        params.guardianName = guardianName.trim();
        params.guardianPhone = guardianPhone;
      }
      if (newPhone) {
        params.newPhone = newPhone;
        params.oldPhoneVerifyCode = oldCode;
        params.newPhoneVerifyCode = newCode;
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
        Taro.switchTab({ url: '/pages/index/index' });
      }, 800);
    } catch (error) {
      const code = getErrorCode(error);
      const message = handleBusinessError(error);
      setErrorTip(message);
      if (code === 440005) {
        Taro.showToast({ title: '昵称或描述包含敏感词', icon: 'none' });
      }
    } finally {
      setLoading(false);
    }
  }

  if (initialLoading) {
    return (
      <View className='profile-complete'>
        <Text className='profile-complete__loading'>加载中…</Text>
      </View>
    );
  }

  const ageNum = Number(age) || 0;

  return (
    <View className='profile-complete'>
      <Text className='profile-complete__title'>完善个人资料</Text>

      <View className='profile-complete__avatar' onClick={handleChooseAvatar}>
        {avatarUrl ? (
          <Image className='profile-complete__avatar-img' src={avatarUrl} mode='aspectFill' />
        ) : (
          <View className='profile-complete__avatar-placeholder'>
            <Text className='profile-complete__avatar-text'>点击上传头像</Text>
          </View>
        )}
      </View>

      <View className='profile-complete__field'>
        <Text className='profile-complete__label'>昵称</Text>
        <Input
          className='profile-complete__input'
          placeholder='请输入昵称'
          value={name}
          onInput={(e) => setName(e.detail.value)}
          maxlength={32}
        />
      </View>

      <View className='profile-complete__field'>
        <Text className='profile-complete__label'>年龄</Text>
        <Input
          className='profile-complete__input'
          type='number'
          placeholder='请输入年龄'
          value={age}
          onInput={(e) => setAge(e.detail.value.replace(/\D/g, '').slice(0, 2))}
        />
      </View>

      <View className='profile-complete__field'>
        <Text className='profile-complete__label'>性别</Text>
        <View className='profile-complete__radio-group'>
          <Text
            className={`profile-complete__radio ${gender === 'male' ? 'profile-complete__radio--active' : ''}`}
            onClick={() => setGender('male')}
          >
            男
          </Text>
          <Text
            className={`profile-complete__radio ${gender === 'female' ? 'profile-complete__radio--active' : ''}`}
            onClick={() => setGender('female')}
          >
            女
          </Text>
        </View>
      </View>

      {ageNum > 0 && ageNum < 18 && (
        <>
          <View className='profile-complete__field'>
            <Text className='profile-complete__label'>监护人姓名</Text>
            <Input
              className='profile-complete__input'
              placeholder='请输入监护人姓名'
              value={guardianName}
              onInput={(e) => setGuardianName(e.detail.value)}
            />
          </View>
          <View className='profile-complete__field'>
            <Text className='profile-complete__label'>监护人手机号</Text>
            <Input
              className='profile-complete__input'
              type='number'
              placeholder='请输入监护人手机号'
              value={guardianPhone}
              onInput={(e) => setGuardianPhone(e.detail.value.slice(0, 11))}
              maxlength={11}
            />
          </View>
        </>
      )}

      <View className='profile-complete__field profile-complete__field--row'>
        <Text className='profile-complete__label'>是否有游泳基础</Text>
        <Switch
          checked={hasSwimBasis}
          onChange={(e) => {
            setHasSwimBasis(e.detail.value);
            if (!e.detail.value) setSwimStrokes([]);
          }}
        />
      </View>

      {hasSwimBasis && (
        <View className='profile-complete__field'>
          <Text className='profile-complete__label'>会游哪些泳姿</Text>
          <View className='profile-complete__stroke-list'>
            {SWIM_STROKES.map((stroke) => (
              <View
                key={stroke.value}
                className={`profile-complete__stroke ${swimStrokes.includes(stroke.value) ? 'profile-complete__stroke--active' : ''}`}
                onClick={() => toggleStroke(stroke.value)}
              >
                <Text className='profile-complete__stroke-text'>{stroke.label}</Text>
              </View>
            ))}
          </View>
          <Input
            className='profile-complete__input profile-complete__input--mt'
            type='number'
            placeholder='游泳年限（选填）'
            value={swimYears}
            onInput={(e) => setSwimYears(e.detail.value.replace(/\D/g, '').slice(0, 2))}
          />
        </View>
      )}

      <View className='profile-complete__field'>
        <Text className='profile-complete__label'>个人描述</Text>
        <Input
          className='profile-complete__input'
          placeholder='简单介绍一下自己（选填）'
          value={personalDesc}
          onInput={(e) => setPersonalDesc(e.detail.value)}
          maxlength={512}
        />
      </View>

      <View className='profile-complete__section'>
        <Text className='profile-complete__section-title'>更换手机号（选填）</Text>
        <View className='profile-complete__field'>
          <Text className='profile-complete__label'>当前手机号</Text>
          <Input
            className='profile-complete__input'
            value={profile?.phone || ''}
            disabled
          />
        </View>
        <View className='profile-complete__field profile-complete__field--code'>
          <Input
            className='profile-complete__input'
            type='number'
            placeholder='旧手机号验证码'
            value={oldCode}
            onInput={(e) => setOldCode(e.detail.value.replace(/\D/g, '').slice(0, 6))}
            maxlength={6}
          />
          <Button
            className={`profile-complete__code-btn ${oldTimer.canSend ? 'profile-complete__code-btn--active' : ''}`}
            onClick={handleSendOldCode}
            disabled={!oldTimer.canSend}
          >
            {oldTimer.isRunning ? `${oldTimer.seconds}s` : '获取验证码'}
          </Button>
        </View>
        <View className='profile-complete__field profile-complete__field--code'>
          <Input
            className='profile-complete__input'
            type='number'
            placeholder='新手机号'
            value={newPhone}
            onInput={(e) => setNewPhone(e.detail.value.replace(/\D/g, '').slice(0, 11))}
            maxlength={11}
          />
        </View>
        <View className='profile-complete__field profile-complete__field--code'>
          <Input
            className='profile-complete__input'
            type='number'
            placeholder='新手机号验证码'
            value={newCode}
            onInput={(e) => setNewCode(e.detail.value.replace(/\D/g, '').slice(0, 6))}
            maxlength={6}
          />
          <Button
            className={`profile-complete__code-btn ${newTimer.canSend && /^1[3-9]\d{9}$/.test(newPhone) ? 'profile-complete__code-btn--active' : ''}`}
            onClick={handleSendNewCode}
            disabled={!newTimer.canSend || !/^1[3-9]\d{9}$/.test(newPhone)}
          >
            {newTimer.isRunning ? `${newTimer.seconds}s` : '获取验证码'}
          </Button>
        </View>
      </View>

      {errorTip && (
        <View className='profile-complete__error'>
          <Text className='profile-complete__error-text'>{errorTip}</Text>
        </View>
      )}

      <Button
        className={`profile-complete__submit ${loading ? 'profile-complete__submit--loading' : ''}`}
        onClick={handleSubmit}
        loading={loading}
        disabled={loading}
      >
        {loading ? '保存中…' : '保存并进入'}
      </Button>
    </View>
  );
}
