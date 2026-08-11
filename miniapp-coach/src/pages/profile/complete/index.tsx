import { useEffect, useRef, useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Input, Button, Image } from '@tarojs/components'
import { getProfile, updateProfile, uploadAvatar, type CoachProfile, type UpdateProfileParams } from '@/api/profile'
import { sendSmsCode } from '@/api/common'
import { handleBusinessError, getErrorCode } from '@/api/request'
import { useAuthStore } from '@/stores/authStore'
import { useCountdown } from '@/hooks/useCountdown'
import './index.scss'

export default function ProfileCompletePage() {
  const [loading, setLoading] = useState(false)
  const [initialLoading, setInitialLoading] = useState(true)
  const [profile, setProfile] = useState<CoachProfile | null>(null)

  const [name, setName] = useState('')
  const [avatarUrl, setAvatarUrl] = useState('')
  const [age, setAge] = useState('')
  const [gender, setGender] = useState<'male' | 'female' | ''>('')
  const [teachingYears, setTeachingYears] = useState('')
  const [personalDesc, setPersonalDesc] = useState('')
  const [newPhone, setNewPhone] = useState('')
  const [oldCode, setOldCode] = useState('')
  const [newCode, setNewCode] = useState('')
  const [errorTip, setErrorTip] = useState('')

  const setCoachInfo = useAuthStore((state) => state.setCoachInfo)
  const coachInfo = useAuthStore((state) => state.coachInfo)
  const oldTimer = useCountdown({ initialSeconds: 60 })
  const newTimer = useCountdown({ initialSeconds: 60 })
  const navigateTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const data = await getProfile()
        if (cancelled) return
        setProfile(data)
        setName(data.name || '')
        setAvatarUrl(data.avatarUrl || '')
        setAge(data.age ? String(data.age) : '')
        setGender(data.gender || '')
        setTeachingYears(data.teachingYears ? String(data.teachingYears) : '')
        setPersonalDesc(data.personalDesc || '')
      } catch (error) {
        if (cancelled) return
        setErrorTip(handleBusinessError(error))
      } finally {
        if (!cancelled) setInitialLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
      if (navigateTimerRef.current) {
        clearTimeout(navigateTimerRef.current)
      }
    }
  }, [])

  async function handleChooseAvatar() {
    try {
      const res = await Taro.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
      })
      if (!res.tempFilePaths || res.tempFilePaths.length === 0) return
      const tempPath = res.tempFilePaths[0]
      const url = await uploadAvatar(tempPath)
      setAvatarUrl(url)
    } catch (error) {
      Taro.showToast({ title: handleBusinessError(error), icon: 'none' })
    }
  }

  async function handleSendOldCode() {
    if (!profile?.phone) return
    try {
      await sendSmsCode({ phone: profile.phone, appType: 'coach', scene: 'change_phone_old' })
      oldTimer.start()
    } catch (error) {
      Taro.showToast({ title: handleBusinessError(error), icon: 'none' })
    }
  }

  async function handleSendNewCode() {
    if (!/^1[3-9]\d{9}$/.test(newPhone)) {
      Taro.showToast({ title: '请输入正确的新手机号', icon: 'none' })
      return
    }
    try {
      await sendSmsCode({ phone: newPhone, appType: 'coach', scene: 'change_phone_new' })
      newTimer.start()
    } catch (error) {
      Taro.showToast({ title: handleBusinessError(error), icon: 'none' })
    }
  }

  function validate(): string | null {
    if (!name.trim()) return '请输入昵称'
    if (!/^[^\s]{1,32}$/.test(name.trim())) return '昵称长度不超过32个字符'
    const ageNum = Number(age)
    if (!age || ageNum < 3 || ageNum > 99) return '年龄需在 3-99 岁之间'
    if (!gender) return '请选择性别'
    if (newPhone && !/^1[3-9]\d{9}$/.test(newPhone)) return '请输入正确的新手机号'
    if (newPhone && (!oldCode || !newCode)) return '请填写新旧手机号的验证码'
    return null
  }

  async function handleSubmit() {
    setErrorTip('')
    const error = validate()
    if (error) {
      setErrorTip(error)
      return
    }
    setLoading(true)
    try {
      const params: UpdateProfileParams = {
        name: name.trim(),
        age: Number(age),
        gender: gender as 'male' | 'female',
        avatarUrl: avatarUrl || undefined,
        teachingYears: teachingYears ? Number(teachingYears) : undefined,
        personalDesc: personalDesc.trim() || undefined,
        idempotencyKey: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
      }
      if (newPhone) {
        params.newPhone = newPhone
        params.oldPhoneVerifyCode = oldCode
        params.newPhoneVerifyCode = newCode
      }
      const data = await updateProfile(params)
      if (coachInfo) {
        setCoachInfo({
          ...coachInfo,
          name: data.name,
          phone: data.phone,
          avatar: data.avatarUrl || coachInfo.avatar,
        })
      }
      Taro.showToast({ title: '保存成功', icon: 'success' })
      navigateTimerRef.current = setTimeout(() => {
        Taro.switchTab({ url: '/pages/index/index' })
      }, 800)
    } catch (error) {
      const code = getErrorCode(error)
      const message = handleBusinessError(error)
      setErrorTip(message)
      if (code === 440005) {
        Taro.showToast({ title: '昵称或描述包含敏感词', icon: 'none' })
      }
    } finally {
      setLoading(false)
    }
  }

  if (initialLoading) {
    return (
      <View className='profile-complete'>
        <Text className='profile-complete__loading'>加载中…</Text>
      </View>
    )
  }

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

      <View className='profile-complete__field'>
        <Text className='profile-complete__label'>执教年限（选填）</Text>
        <Input
          className='profile-complete__input'
          type='number'
          placeholder='请输入执教年限'
          value={teachingYears}
          onInput={(e) => setTeachingYears(e.detail.value.replace(/\D/g, '').slice(0, 2))}
        />
      </View>

      <View className='profile-complete__field'>
        <Text className='profile-complete__label'>个人简介（选填）</Text>
        <Input
          className='profile-complete__input'
          placeholder='简单介绍一下自己'
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
  )
}
