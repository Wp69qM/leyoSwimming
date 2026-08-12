import { useEffect, useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Input, Button, Image, Textarea } from '@tarojs/components'
import { getProfile, updateProfile, type CoachProfile, type UpdateProfileParams } from '@/api/profile'
import { uploadFile } from '@/api/common'
import { handleBusinessError } from '@/api/request'
import './index.scss'

const STROKES = ['蛙泳', '自由泳', '仰泳', '蝶泳']

function generateIdempotencyKey(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export default function ProfileEditPage() {
  const [initialLoading, setInitialLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [profile, setProfile] = useState<CoachProfile | null>(null)

  const [name, setName] = useState('')
  const [age, setAge] = useState('')
  const [gender, setGender] = useState<'male' | 'female' | ''>('')
  const [email, setEmail] = useState('')
  const [portraitUrl, setPortraitUrl] = useState('')
  const [wechatQrUrl, setWechatQrUrl] = useState('')
  const [teachingYears, setTeachingYears] = useState('')
  const [teachingStrokes, setTeachingStrokes] = useState<string[]>([])
  const [bio, setBio] = useState('')

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const data = await getProfile()
        if (cancelled) return
        setProfile(data)
        setName(data.name || '')
        setAge(data.age ? String(data.age) : '')
        setGender(data.gender || '')
        setEmail(data.email || '')
        setPortraitUrl(data.portraitUrl || '')
        setWechatQrUrl(data.wechatQrUrl || '')
        setTeachingYears(data.teachingYears ? String(data.teachingYears) : '')
        setTeachingStrokes(data.teachingStrokes || [])
        setBio(data.bio || '')
      } catch (err) {
        if (cancelled) return
        Taro.showToast({ title: handleBusinessError(err), icon: 'none' })
      } finally {
        if (!cancelled) setInitialLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [])

  async function handleUploadPortrait() {
    try {
      const res = await Taro.chooseImage({ count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'] })
      if (!res.tempFilePaths || res.tempFilePaths.length === 0) return
      const url = await uploadFile(res.tempFilePaths[0])
      setPortraitUrl(url)
    } catch (error) {
      Taro.showToast({ title: handleBusinessError(error), icon: 'none' })
    }
  }

  async function handleUploadWechatQr() {
    try {
      const res = await Taro.chooseImage({ count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'] })
      if (!res.tempFilePaths || res.tempFilePaths.length === 0) return
      const url = await uploadFile(res.tempFilePaths[0])
      setWechatQrUrl(url)
    } catch (error) {
      Taro.showToast({ title: handleBusinessError(error), icon: 'none' })
    }
  }

  function toggleStroke(stroke: string) {
    setTeachingStrokes((prev) =>
      prev.includes(stroke) ? prev.filter((s) => s !== stroke) : [...prev, stroke]
    )
  }

  function validate(): string | null {
    if (!name.trim()) return '姓名不能为空'
    if (name.length > 32) return '姓名不能超过 32 个字符'
    if (!gender) return '请选择性别'
    const ageNum = Number(age)
    if (!age || Number.isNaN(ageNum) || ageNum < 18 || ageNum > 80) return '年龄需在 18-80 之间'
    if (email && !/^\S+@\S+\.\S+$/.test(email)) return '邮箱格式不正确'
    if (teachingYears) {
      const yearsNum = Number(teachingYears)
      if (Number.isNaN(yearsNum) || yearsNum < 0 || yearsNum > 60) return '任教年限需在 0-60 之间'
    }
    if (bio && (bio.length < 10 || bio.length > 500)) return '个人简介需在 10-500 个字符之间'
    return null
  }

  async function handleSave() {
    const error = validate()
    if (error) {
      Taro.showToast({ title: error, icon: 'none' })
      return
    }

    setSaving(true)
    try {
      const params: UpdateProfileParams = {
        name: name.trim(),
        age: Number(age),
        gender: gender as 'male' | 'female',
        email: email.trim() || undefined,
        portraitUrl: portraitUrl || undefined,
        wechatQrUrl: wechatQrUrl || undefined,
        teachingYears: teachingYears ? Number(teachingYears) : undefined,
        teachingStrokes: teachingStrokes.length > 0 ? teachingStrokes : undefined,
        bio: bio.trim() || undefined,
        idempotencyKey: generateIdempotencyKey(),
      }
      await updateProfile(params)
      Taro.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => {
        Taro.navigateBack()
      }, 800)
    } catch (err) {
      Taro.showToast({ title: handleBusinessError(err), icon: 'none' })
    } finally {
      setSaving(false)
    }
  }

  if (initialLoading) {
    return (
      <View className='profile-edit profile-edit--loading'>
        <Text className='profile-edit__loading-text'>加载中…</Text>
      </View>
    )
  }

  return (
    <View className='profile-edit'>
      <View className='profile-edit__section'>
        <Text className='profile-edit__section-title'>形象展示</Text>
        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>个人形象照</Text>
          <View className='profile-edit__uploader' onClick={handleUploadPortrait}>
            {portraitUrl ? (
              <Image className='profile-edit__uploader-img' src={portraitUrl} mode='aspectFill' />
            ) : (
              <Text className='profile-edit__uploader-placeholder'>+</Text>
            )}
          </View>
        </View>

        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>姓名</Text>
          <Input
            className='profile-edit__input'
            value={name}
            onInput={(e) => setName(e.detail.value)}
            placeholder='请输入姓名'
            maxLength={32}
          />
        </View>

        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>任教年限</Text>
          <Input
            className='profile-edit__input'
            value={teachingYears}
            onInput={(e) => setTeachingYears(e.detail.value)}
            placeholder='请输入任教年限'
            type='number'
          />
        </View>
      </View>

      <View className='profile-edit__section'>
        <Text className='profile-edit__section-title'>基础信息</Text>
        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>性别</Text>
          <View className='profile-edit__options'>
            <View
              className={`profile-edit__option ${gender === 'male' ? 'profile-edit__option--active' : ''}`}
              onClick={() => setGender('male')}
            >
              <Text>男</Text>
            </View>
            <View
              className={`profile-edit__option ${gender === 'female' ? 'profile-edit__option--active' : ''}`}
              onClick={() => setGender('female')}
            >
              <Text>女</Text>
            </View>
          </View>
        </View>

        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>年龄</Text>
          <Input
            className='profile-edit__input'
            value={age}
            onInput={(e) => setAge(e.detail.value)}
            placeholder='请输入年龄'
            type='number'
          />
        </View>

        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>邮箱</Text>
          <Input
            className='profile-edit__input'
            value={email}
            onInput={(e) => setEmail(e.detail.value)}
            placeholder='请输入邮箱'
          />
        </View>

        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>手机号</Text>
          <Text className='profile-edit__readonly'>{profile?.phone || '未设置'}</Text>
        </View>

        <View className='profile-edit__field profile-edit__field--top'>
          <Text className='profile-edit__label'>微信二维码</Text>
          <View className='profile-edit__uploader' onClick={handleUploadWechatQr}>
            {wechatQrUrl ? (
              <Image className='profile-edit__uploader-img' src={wechatQrUrl} mode='aspectFill' />
            ) : (
              <Text className='profile-edit__uploader-placeholder'>+</Text>
            )}
          </View>
        </View>
      </View>

      <View className='profile-edit__section'>
        <Text className='profile-edit__section-title'>实名与资质（只读）</Text>
        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>身份证号</Text>
          <Text className='profile-edit__readonly'>{profile?.idCardNoMasked || '未认证'}</Text>
        </View>
        {profile?.idCardFrontUrl && (
          <View className='profile-edit__field profile-edit__field--top'>
            <Text className='profile-edit__label'>身份证正面</Text>
            <Image className='profile-edit__cert-img' src={profile.idCardFrontUrl} mode='aspectFit' />
          </View>
        )}
        {profile?.idCardBackUrl && (
          <View className='profile-edit__field profile-edit__field--top'>
            <Text className='profile-edit__label'>身份证反面</Text>
            <Image className='profile-edit__cert-img' src={profile.idCardBackUrl} mode='aspectFit' />
          </View>
        )}
        {profile?.coachCertUrls && profile.coachCertUrls.length > 0 && (
          <View className='profile-edit__field profile-edit__field--top'>
            <Text className='profile-edit__label'>教练资格证</Text>
            <View className='profile-edit__cert-list'>
              {profile.coachCertUrls.map((url, idx) => (
                <Image key={idx} className='profile-edit__cert-img' src={url} mode='aspectFit' />
              ))}
            </View>
          </View>
        )}
        {profile?.healthCertUrl && (
          <View className='profile-edit__field profile-edit__field--top'>
            <Text className='profile-edit__label'>健康证</Text>
            <Image className='profile-edit__cert-img' src={profile.healthCertUrl} mode='aspectFit' />
          </View>
        )}
      </View>

      <View className='profile-edit__section'>
        <Text className='profile-edit__section-title'>教学履历</Text>
        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>总学员数</Text>
          <Text className='profile-edit__readonly'>{profile?.totalStudents ?? 0} 人</Text>
        </View>
        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>总课时数</Text>
          <Text className='profile-edit__readonly'>{profile?.totalHours ?? 0} 节</Text>
        </View>
        <View className='profile-edit__field profile-edit__field--top'>
          <Text className='profile-edit__label'>擅长泳姿</Text>
          <View className='profile-edit__strokes'>
            {STROKES.map((stroke) => (
              <View
                key={stroke}
                className={`profile-edit__stroke ${teachingStrokes.includes(stroke) ? 'profile-edit__stroke--active' : ''}`}
                onClick={() => toggleStroke(stroke)}
              >
                <Text>{stroke}</Text>
              </View>
            ))}
          </View>
        </View>
        <View className='profile-edit__field profile-edit__field--top'>
          <Text className='profile-edit__label'>个人简介</Text>
          <Textarea
            className='profile-edit__textarea'
            value={bio}
            onInput={(e) => setBio(e.detail.value)}
            placeholder='请输入个人简介，10-500 字'
            maxlength={500}
          />
          <Text className='profile-edit__count'>{bio.length}/500</Text>
        </View>
      </View>

      <Button className='profile-edit__save' onClick={handleSave} disabled={saving}>
        {saving ? '保存中…' : '保存'}
      </Button>
    </View>
  )
}
