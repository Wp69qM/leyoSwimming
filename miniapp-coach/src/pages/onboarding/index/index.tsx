import { useEffect, useRef, useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Input, Button, Image, Textarea } from '@tarojs/components'
import {
  getApplicationDetail,
  saveDraft,
  submitApplication,
  type CoachApplication,
  type CoachCertificate,
} from '@/api/onboarding'
import { uploadFile } from '@/api/common'
import { handleBusinessError } from '@/api/request'
import { COACH_STATUS } from '@/constants'
import './index.scss'

const CERT_TYPES: { key: string; label: string; multiple: boolean }[] = [
  { key: 'ID_CARD_FRONT', label: '身份证正面照', multiple: false },
  { key: 'ID_CARD_BACK', label: '身份证反面照', multiple: false },
  { key: 'COACH_CERT', label: '教练资格证', multiple: true },
  { key: 'HEALTH_CERT', label: '健康证', multiple: false },
  { key: 'PORTRAIT', label: '个人形象照', multiple: false },
]

const STROKE_OPTIONS = ['蛙泳', '自由泳', '仰泳', '蝶泳']

export default function CoachOnboardingPage() {
  const [loading, setLoading] = useState(false)
  const [initialLoading, setInitialLoading] = useState(true)
  const [detail, setDetail] = useState<CoachApplication | null>(null)
  const [showRejection, setShowRejection] = useState(true)

  const [name, setName] = useState('')
  const [age, setAge] = useState('')
  const [gender, setGender] = useState<'male' | 'female' | ''>('')
  const [email, setEmail] = useState('')
  const [wechatQrUrl, setWechatQrUrl] = useState('')
  const [idCardNo, setIdCardNo] = useState('')
  const [teachingYears, setTeachingYears] = useState('')
  const [totalStudents, setTotalStudents] = useState('')
  const [totalHours, setTotalHours] = useState('')
  const [teachingStrokes, setTeachingStrokes] = useState<string[]>([])
  const [bio, setBio] = useState('')
  const [referencePrice, setReferencePrice] = useState('')
  const [certificates, setCertificates] = useState<CoachCertificate[]>([])
  const [errorTip, setErrorTip] = useState('')

  const navigateTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const data = await getApplicationDetail()
        if (cancelled) return
        handleStatusRedirect(data)
        setDetail(data)
        fillForm(data)
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
      const timerId = navigateTimerRef.current
      if (timerId) {
        clearTimeout(timerId)
      }
    }
  }, [])

  function handleStatusRedirect(data: CoachApplication) {
    if (data.status === COACH_STATUS.UNDER_REVIEW && data.submittedAt) {
      Taro.redirectTo({ url: '/pages/onboarding/pending/index' })
      return
    }
    if (data.status === COACH_STATUS.APPROVED) {
      Taro.switchTab({ url: '/pages/index/index' })
      return
    }
    if (data.status === COACH_STATUS.RESIGNING) {
      Taro.showToast({ title: '当前状态不可修改入驻资料', icon: 'none' })
      return
    }
  }

  function fillForm(data: CoachApplication) {
    setName(data.name || '')
    setAge(data.age ? String(data.age) : '')
    setGender(data.gender || '')
    setEmail(data.email || '')
    setWechatQrUrl(data.wechatQrUrl || '')
    setIdCardNo(data.idCardNo || '')
    setTeachingYears(data.teachingYears ? String(data.teachingYears) : '')
    setTotalStudents(data.totalStudents ? String(data.totalStudents) : '')
    setTotalHours(data.totalHours ? String(data.totalHours) : '')
    setTeachingStrokes(data.teachingStrokes || [])
    setBio(data.bio || '')
    setReferencePrice(data.referencePrice ? String(data.referencePrice) : '')
    setCertificates(data.certificates || [])
  }

  function getTitle(): string {
    if (detail?.entryType === 'reapply') return '重新入驻资料'
    return '入驻资料'
  }

  function getCertImages(certType: string): string[] {
    return certificates
      .filter((c) => c.certType === certType)
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
      .map((c) => c.imageUrl)
  }

  async function handleUpload(certType: string, multiple: boolean) {
    if (isFormDisabled) return
    try {
      const res = await Taro.chooseImage({
        count: multiple ? 9 : 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
      })
      if (!res.tempFilePaths || res.tempFilePaths.length === 0) return

      const uploadResults = await Promise.allSettled(
        res.tempFilePaths.map((tempPath) => uploadFile(tempPath))
      )

      const urls: string[] = []
      let failedCount = 0
      uploadResults.forEach((result) => {
        if (result.status === 'fulfilled') {
          urls.push(result.value)
        } else {
          failedCount += 1
        }
      })

      if (urls.length > 0) {
        setCertificates((prev) => {
          const filtered = prev.filter((c) => c.certType !== certType)
          const added = urls.map((url, index) => ({
            certType,
            imageUrl: url,
            sortOrder: index,
          }))
          return [...filtered, ...added]
        })
      }

      if (failedCount > 0) {
        Taro.showToast({
          title: `${failedCount} 张图片上传失败，请重新尝试`,
          icon: 'none',
        })
      }
    } catch (error) {
      Taro.showToast({ title: handleBusinessError(error), icon: 'none' })
    }
  }

  function removeCert(certType: string, imageUrl: string) {
    if (isFormDisabled) return
    setCertificates((prev) => prev.filter((c) => !(c.certType === certType && c.imageUrl === imageUrl)))
  }

  async function handleUploadWechatQr() {
    if (isFormDisabled) return
    try {
      const res = await Taro.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
      })
      if (res.tempFilePaths?.[0]) {
        const url = await uploadFile(res.tempFilePaths[0])
        setWechatQrUrl(url)
      }
    } catch (error) {
      Taro.showToast({ title: handleBusinessError(error), icon: 'none' })
    }
  }

  function toggleStroke(stroke: string) {
    if (isFormDisabled) return
    setTeachingStrokes((prev) =>
      prev.includes(stroke) ? prev.filter((s) => s !== stroke) : [...prev, stroke]
    )
  }

  function validate(isSubmit: boolean): string | null {
    if (!name.trim()) return '请输入姓名'
    if (isSubmit) {
      if (!/^[\s\S]{1,32}$/.test(name.trim())) return '姓名长度不能超过 32 个字符'
      if (!gender) return '请选择性别'
      const ageNum = Number(age)
      if (!age || ageNum < 18 || ageNum > 80) return '年龄需在 18-80 岁之间'
      if (!email.trim() || !/^\S+@\S+\.\S+$/.test(email.trim())) return '请输入正确的邮箱'
      if (!wechatQrUrl.trim()) return '请上传微信二维码'
      if (!/^\d{17}[\dXx]$/.test(idCardNo.trim())) return '请输入 18 位有效身份证号'
      const years = Number(teachingYears)
      if (teachingYears === '' || years < 0 || years > 60) return '任教年限需在 0-60 之间'
      const students = Number(totalStudents)
      if (totalStudents === '' || students < 0 || students > 99999) return '总学员数需在 0-99999 之间'
      const hours = Number(totalHours)
      if (totalHours === '' || hours < 0 || hours > 99999) return '总课时数需在 0-99999 之间'
      if (!bio.trim() || bio.trim().length < 10 || bio.trim().length > 500) {
        return '个人简介需在 10-500 字符之间'
      }
      const price = Number(referencePrice)
      if (referencePrice === '' || price < 50 || price > 2000) return '参考单价需在 50-2000 之间'
      for (const cert of CERT_TYPES) {
        const has = certificates.some((c) => c.certType === cert.key)
        if (!has) return `请上传${cert.label}`
      }
    }
    return null
  }

  function buildParams(): {
    name: string
    gender: 'male' | 'female'
    age: number
    email: string
    wechatQrUrl: string
    idCardNo: string
    teachingYears: number
    totalStudents: number
    totalHours: number
    bio: string
    referencePrice: number
    certificates: CoachCertificate[]
    idempotencyKey: string
  } {
    return {
      name: name.trim(),
      gender: gender as 'male' | 'female',
      age: Number(age),
      email: email.trim(),
      wechatQrUrl: wechatQrUrl.trim(),
      idCardNo: idCardNo.trim(),
      teachingYears: Number(teachingYears),
      totalStudents: Number(totalStudents),
      totalHours: Number(totalHours),
      bio: bio.trim(),
      referencePrice: Number(referencePrice),
      certificates,
      idempotencyKey: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
    }
  }

  async function handleSaveDraft() {
    setErrorTip('')
    const error = validate(false)
    if (error) {
      setErrorTip(error)
      return
    }
    setLoading(true)
    try {
      const params = buildParams()
      await saveDraft({
        applicationId: detail?.applicationId,
        ...params,
      })
      Taro.showToast({ title: '草稿已保存', icon: 'success' })
    } catch (err) {
      setErrorTip(handleBusinessError(err))
    } finally {
      setLoading(false)
    }
  }

  async function handleSubmit() {
    setErrorTip('')
    const error = validate(true)
    if (error) {
      setErrorTip(error)
      return
    }
    setLoading(true)
    try {
      const params = buildParams()
      await submitApplication(params)
      Taro.redirectTo({ url: '/pages/onboarding/success/index' })
    } catch (err) {
      setErrorTip(handleBusinessError(err))
    } finally {
      setLoading(false)
    }
  }

  const isFormDisabled = detail?.status === COACH_STATUS.RESIGNING

  if (initialLoading) {
    return (
      <View className='coach-onboarding'>
        <Text className='coach-onboarding__loading'>加载中…</Text>
      </View>
    )
  }

  return (
    <View className='coach-onboarding'>
      <View className='coach-onboarding__navbar'>
        <Text className='coach-onboarding__title'>{getTitle()}</Text>
      </View>

      {detail?.status === COACH_STATUS.REJECTED && showRejection && detail.promptMessage && (
        <View className='coach-onboarding__rejection'>
          <View className='coach-onboarding__rejection-content'>
            <Text className='coach-onboarding__rejection-title'>审核未通过</Text>
            <Text className='coach-onboarding__rejection-reason'>{detail.promptMessage}</Text>
          </View>
          <Text className='coach-onboarding__rejection-close' onClick={() => setShowRejection(false)}>
            ✕
          </Text>
        </View>
      )}

      <View className='coach-onboarding__card'>
        <Text className='coach-onboarding__card-title'>基础信息</Text>
        <View className='coach-onboarding__field'>
          <Text className='coach-onboarding__label'>姓名/昵称</Text>
          <Input
            className='coach-onboarding__input'
            placeholder='请输入姓名'
            value={name}
            onInput={(e) => setName(e.detail.value)}
            maxlength={32}
            disabled={isFormDisabled}
          />
        </View>
        <View className='coach-onboarding__field'>
          <Text className='coach-onboarding__label'>手机号</Text>
          <Text className='coach-onboarding__readonly'>{detail?.phone || ''}</Text>
        </View>
        <View className='coach-onboarding__field'>
          <Text className='coach-onboarding__label'>性别</Text>
          <View className='coach-onboarding__radio-group'>
            <Text
              className={`coach-onboarding__radio ${gender === 'male' ? 'coach-onboarding__radio--active' : ''}`}
              onClick={() => !isFormDisabled && setGender('male')}
            >
              男
            </Text>
            <Text
              className={`coach-onboarding__radio ${gender === 'female' ? 'coach-onboarding__radio--active' : ''}`}
              onClick={() => !isFormDisabled && setGender('female')}
            >
              女
            </Text>
          </View>
        </View>
        <View className='coach-onboarding__field coach-onboarding__field--column'>
          <Text className='coach-onboarding__label'>微信二维码</Text>
          <View className='coach-onboarding__upload-list'>
            {wechatQrUrl ? (
              <View className='coach-onboarding__upload-item'>
                <Image className='coach-onboarding__upload-img' src={wechatQrUrl} mode='aspectFill' />
                <Text
                  className='coach-onboarding__upload-remove'
                  onClick={() => !isFormDisabled && setWechatQrUrl('')}
                >
                  ✕
                </Text>
              </View>
            ) : (
              <View
                className='coach-onboarding__upload-placeholder'
                onClick={handleUploadWechatQr}
              >
                <Text className='coach-onboarding__upload-plus'>+</Text>
              </View>
            )}
          </View>
        </View>
      </View>

      <View className='coach-onboarding__card'>
        <Text className='coach-onboarding__card-title'>实名与资质</Text>
        <View className='coach-onboarding__field'>
          <Text className='coach-onboarding__label'>身份证号</Text>
          <Input
            className='coach-onboarding__input'
            placeholder='请输入 18 位身份证号'
            value={idCardNo}
            onInput={(e) => setIdCardNo(e.detail.value.trim())}
            maxlength={18}
            disabled={isFormDisabled}
          />
        </View>
        {CERT_TYPES.map((cert) => (
          <View key={cert.key} className='coach-onboarding__upload-field'>
            <Text className='coach-onboarding__label'>{cert.label}</Text>
            <View className='coach-onboarding__upload-list'>
              {getCertImages(cert.key).map((url) => (
                <View key={url} className='coach-onboarding__upload-item'>
                  <Image className='coach-onboarding__upload-img' src={url} mode='aspectFill' />
                  <Text
                    className='coach-onboarding__upload-remove'
                    onClick={() => removeCert(cert.key, url)}
                  >
                    ✕
                  </Text>
                </View>
              ))}
              {(cert.multiple || getCertImages(cert.key).length === 0) && (
                <View
                  className='coach-onboarding__upload-placeholder'
                  onClick={() => handleUpload(cert.key, cert.multiple)}
                >
                  <Text className='coach-onboarding__upload-plus'>+</Text>
                </View>
              )}
            </View>
          </View>
        ))}
      </View>

      <View className='coach-onboarding__card'>
        <Text className='coach-onboarding__card-title'>教学履历</Text>
        <View className='coach-onboarding__field'>
          <Text className='coach-onboarding__label'>任教年限</Text>
          <Input
            className='coach-onboarding__input'
            type='number'
            placeholder='请输入任教年限'
            value={teachingYears}
            onInput={(e) => setTeachingYears(e.detail.value.replace(/\D/g, '').slice(0, 2))}
            disabled={isFormDisabled}
          />
          <Text className='coach-onboarding__unit'>年</Text>
        </View>
        <View className='coach-onboarding__field'>
          <Text className='coach-onboarding__label'>总学员数</Text>
          <Input
            className='coach-onboarding__input'
            type='number'
            placeholder='请输入总学员数'
            value={totalStudents}
            onInput={(e) => setTotalStudents(e.detail.value.replace(/\D/g, '').slice(0, 5))}
            disabled={isFormDisabled}
          />
          <Text className='coach-onboarding__unit'>人</Text>
        </View>
        <View className='coach-onboarding__field'>
          <Text className='coach-onboarding__label'>总课时数</Text>
          <Input
            className='coach-onboarding__input'
            type='number'
            placeholder='请输入总课时数'
            value={totalHours}
            onInput={(e) => setTotalHours(e.detail.value.replace(/\D/g, '').slice(0, 5))}
            disabled={isFormDisabled}
          />
          <Text className='coach-onboarding__unit'>节</Text>
        </View>
        <View className='coach-onboarding__field coach-onboarding__field--column'>
          <Text className='coach-onboarding__label'>擅长泳姿（可选）</Text>
          <View className='coach-onboarding__stroke-list'>
            {STROKE_OPTIONS.map((stroke) => (
              <Text
                key={stroke}
                className={`coach-onboarding__stroke-tag ${teachingStrokes.includes(stroke) ? 'coach-onboarding__stroke-tag--active' : ''}`}
                onClick={() => toggleStroke(stroke)}
              >
                {stroke}
              </Text>
            ))}
          </View>
        </View>
        <View className='coach-onboarding__field coach-onboarding__field--column'>
          <Text className='coach-onboarding__label'>个人简介</Text>
          <Textarea
            className='coach-onboarding__textarea'
            placeholder='请输入个人简介'
            value={bio}
            onInput={(e) => setBio(e.detail.value)}
            maxlength={500}
            disabled={isFormDisabled}
          />
          <Text className='coach-onboarding__char-count'>{bio.length}/500</Text>
        </View>
      </View>

      <View className='coach-onboarding__card'>
        <Text className='coach-onboarding__card-title'>服务设置</Text>
        <View className='coach-onboarding__field'>
          <Text className='coach-onboarding__label'>参考单价</Text>
          <Input
            className='coach-onboarding__input'
            type='digit'
            placeholder='请输入参考单价'
            value={referencePrice}
            onInput={(e) => setReferencePrice(e.detail.value)}
            disabled={isFormDisabled}
          />
          <Text className='coach-onboarding__unit'>元/节</Text>
        </View>
        <Text className='coach-onboarding__hint'>参考单价范围 50-2000 元/节</Text>
      </View>

      {errorTip && (
        <View className='coach-onboarding__error'>
          <Text className='coach-onboarding__error-text'>{errorTip}</Text>
        </View>
      )}

      <View className='coach-onboarding__actions'>
        <Button
          className='coach-onboarding__btn-draft'
          onClick={handleSaveDraft}
          disabled={loading || isFormDisabled}
        >
          保存草稿
        </Button>
        <Button
          className={`coach-onboarding__btn-submit ${loading ? 'coach-onboarding__btn-submit--loading' : ''}`}
          onClick={handleSubmit}
          loading={loading}
          disabled={loading || isFormDisabled}
        >
          {detail?.status === COACH_STATUS.REJECTED ? '重新提交' : '提交审核'}
        </Button>
      </View>
    </View>
  )
}
