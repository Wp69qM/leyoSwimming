import { useEffect, useRef, useState } from 'react'
import Taro, { useDidShow, useShareAppMessage } from '@tarojs/taro'
import { View, Text, Input, Button, Image, Textarea } from '@tarojs/components'
import { getProfile, updateProfile, type CoachProfile, type UpdateProfileParams } from '@/api/profile'
import { uploadFile } from '@/api/common'
import { handleBusinessError } from '@/api/request'
import { COACH_STATUS } from '@/constants'
import './index.scss'

const STROKES = ['蛙泳', '自由泳', '仰泳', '蝶泳']

const STATUS_TEXT: Record<number, string> = {
  [COACH_STATUS.UNDER_REVIEW]: '待审核',
  [COACH_STATUS.APPROVED]: '在职',
  [COACH_STATUS.REJECTED]: '已驳回',
  [COACH_STATUS.RESIGNED]: '已离职',
  [COACH_STATUS.RESIGNING]: '申请离职中',
  [COACH_STATUS.PENDING_ONBOARDING]: '待入驻',
}

const STATUS_TAG_CLASS: Record<number, string> = {
  [COACH_STATUS.UNDER_REVIEW]: 'profile-edit__status-tag--warning',
  [COACH_STATUS.APPROVED]: 'profile-edit__status-tag--success',
  [COACH_STATUS.REJECTED]: 'profile-edit__status-tag--danger',
  [COACH_STATUS.RESIGNED]: 'profile-edit__status-tag--danger',
  [COACH_STATUS.RESIGNING]: 'profile-edit__status-tag--warning',
  [COACH_STATUS.PENDING_ONBOARDING]: 'profile-edit__status-tag--default',
}

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
  const [referencePrice, setReferencePrice] = useState<number | null>(null)

  const navigateBackTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  async function load(cancelledRef?: { cancelled: boolean }) {
    try {
      const data = await getProfile()
      if (cancelledRef?.cancelled) return
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
      setReferencePrice(data.referencePrice ?? null)
    } catch (err) {
      if (cancelledRef?.cancelled) return
      Taro.showToast({ title: handleBusinessError(err), icon: 'none' })
    } finally {
      if (!cancelledRef?.cancelled) setInitialLoading(false)
    }
  }

  useDidShow(() => {
    const cancelledRef = { cancelled: false }
    load(cancelledRef)
    return () => {
      cancelledRef.cancelled = true
    }
  })

  useEffect(() => {
    return () => {
      if (navigateBackTimerRef.current) {
        clearTimeout(navigateBackTimerRef.current)
        navigateBackTimerRef.current = null
      }
    }
  }, [])

  useShareAppMessage(() => {
    if (!profile) return {}
    return {
      title: `${profile.name || '教练'}的主页`,
      path: `/pages/coach/index?id=${profile.id}`,
      imageUrl: portraitUrl || '',
    }
  })

  function handleBack() {
    Taro.navigateBack().catch(() => {
      Taro.switchTab({ url: '/pages/mine/index' })
    })
  }

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
      navigateBackTimerRef.current = setTimeout(() => {
        Taro.navigateBack()
      }, 800)
    } catch (err) {
      Taro.showToast({ title: handleBusinessError(err), icon: 'none' })
    } finally {
      setSaving(false)
    }
  }

  function handleGoReferencePrice() {
    Taro.navigateTo({ url: '/pages/profile/reference-price/index' })
  }

  if (initialLoading) {
    return (
      <View className='profile-edit profile-edit--loading'>
        <Text className='profile-edit__loading-text'>加载中…</Text>
      </View>
    )
  }

  const status = profile?.status ?? COACH_STATUS.UNDER_REVIEW
  const showShare = status !== COACH_STATUS.RESIGNING
  const statusText = STATUS_TEXT[status] ?? '未知状态'

  return (
    <View className='profile-edit'>
      <View className='profile-edit__status-bar' />
      <View className='profile-edit__navbar'>
        <View className='profile-edit__navbar-back' onClick={handleBack}>
          <Text className='profile-edit__navbar-back-icon'>‹</Text>
        </View>
        <Text className='profile-edit__navbar-title'>个人主页</Text>
        {showShare ? (
          <Button className='profile-edit__navbar-share' openType='share'>
            <Text className='profile-edit__navbar-share-text'>分享</Text>
          </Button>
        ) : (
          <View className='profile-edit__navbar-placeholder' />
        )}
      </View>

      <View className='profile-edit__header'>
        <Image
          className='profile-edit__header-avatar'
          src={portraitUrl || ''}
          mode='aspectFill'
        />
        <Text className='profile-edit__header-name'>{name || '未设置姓名'}</Text>
        <Text className='profile-edit__header-years'>
          {teachingYears ? `任教 ${teachingYears} 年` : '暂无任教年限'}
        </Text>
        <View className={`profile-edit__status-tag ${STATUS_TAG_CLASS[status] || ''}`}>
          <Text>{statusText}</Text>
        </View>
      </View>

      <View className='profile-edit__section'>
        <Text className='profile-edit__section-title'>形象展示</Text>
        <View className='profile-edit__field profile-edit__field--top'>
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
          <Text className='profile-edit__label'>姓名/昵称</Text>
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
          <View className='profile-edit__input-wrap'>
            <Input
              className='profile-edit__input'
              value={teachingYears}
              onInput={(e) => setTeachingYears(e.detail.value)}
              placeholder='请输入任教年限'
              type='number'
            />
            <Text className='profile-edit__unit'>年</Text>
          </View>
        </View>

        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>实时状态标签</Text>
          <View className='profile-edit__readonly-wrap'>
            <View className={`profile-edit__status-tag ${STATUS_TAG_CLASS[status] || ''}`}>
              <Text>{statusText}</Text>
            </View>
            <Text className='profile-edit__readonly-hint'>系统自动更新</Text>
          </View>
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
          <View className='profile-edit__input-wrap'>
            <Input
              className='profile-edit__input'
              value={age}
              onInput={(e) => setAge(e.detail.value)}
              placeholder='请输入年龄'
              type='number'
            />
            <Text className='profile-edit__unit'>岁</Text>
          </View>
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
          <View className='profile-edit__uploader profile-edit__uploader--large' onClick={handleUploadWechatQr}>
            {wechatQrUrl ? (
              <Image className='profile-edit__uploader-img' src={wechatQrUrl} mode='aspectFill' />
            ) : (
              <Text className='profile-edit__uploader-placeholder'>+</Text>
            )}
          </View>
          <Text className='profile-edit__uploader-hint'>JPG/PNG，≤5MB，点击可预览/替换</Text>
        </View>
      </View>

      <View className='profile-edit__section'>
        <Text className='profile-edit__section-title'>实名与资质</Text>
        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>身份证号</Text>
          <Text className='profile-edit__readonly'>{profile?.idCardNoMasked || '未认证'}</Text>
        </View>
        {profile?.idCardFrontUrl && (
          <View className='profile-edit__field profile-edit__field--top'>
            <Text className='profile-edit__label'>身份证正面照</Text>
            <Image className='profile-edit__cert-img' src={profile.idCardFrontUrl} mode='aspectFit' />
          </View>
        )}
        {profile?.idCardBackUrl && (
          <View className='profile-edit__field profile-edit__field--top'>
            <Text className='profile-edit__label'>身份证反面照</Text>
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
        <View className='profile-edit__field'>
          <Text
            className='profile-edit__reapply-link'
            onClick={() =>
              Taro.showModal({
                title: '修改实名与资质',
                content: '修改实名与资质需重新提交入驻审核，是否继续？',
                confirmText: '继续',
                cancelText: '取消',
                success: (res) => {
                  if (res.confirm) {
                    Taro.navigateTo({ url: '/pages/onboarding/index/index' })
                  }
                },
              })
            }
          >
            修改实名与资质需重新提交审核
          </Text>
        </View>
      </View>

      <View className='profile-edit__section'>
        <Text className='profile-edit__section-title'>教学履历</Text>
        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>总学员数</Text>
          <Text className='profile-edit__readonly'>{profile?.totalStudents ?? 0} 人（系统统计）</Text>
        </View>
        <View className='profile-edit__field'>
          <Text className='profile-edit__label'>总课时数</Text>
          <Text className='profile-edit__readonly'>{profile?.totalHours ?? 0} 节（系统统计）</Text>
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

      <View className='profile-edit__section'>
        <Text className='profile-edit__section-title'>服务设置</Text>
        <View className='profile-edit__field profile-edit__field--arrow' onClick={handleGoReferencePrice}>
          <Text className='profile-edit__label'>参考单价</Text>
          <Text className='profile-edit__readonly'>
            {referencePrice ? `${referencePrice} 元/节` : '未设置'}
          </Text>
        </View>
      </View>

      <Button className='profile-edit__save' onClick={handleSave} disabled={saving}>
        {saving ? '保存中…' : '保存'}
      </Button>
    </View>
  )
}
