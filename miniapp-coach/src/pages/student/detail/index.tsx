import { useEffect, useMemo, useRef, useState } from 'react'
import Taro, { useRouter } from '@tarojs/taro'
import { View, Text, Textarea, Image } from '@tarojs/components'
import {
  fetchCoachStudentDetail,
  fetchCoachStudentPackageList,
  updateCoachStudent
} from '@/api/student'
import type {
  CoachStudentDetailData,
  CoachStudentPackageItem,
  CoachStudentSlice
} from '@/types/student'
import './index.scss'

const STROKE_OPTIONS = ['自由泳', '蛙泳', '仰泳', '蝶泳']
const LEVEL_OPTIONS = [
  { label: '初级', value: 0 },
  { label: '进阶', value: 1 },
  { label: '高级', value: 2 }
]

const MAX_BASICS_LENGTH = 200
const MAX_NOTES_LENGTH = 500

export default function StudentDetailPage() {
  const router = useRouter()
  const studentUserId = useMemo(() => {
    const raw = router.params.studentUserId
    return raw ? Number(raw) : 0
  }, [router.params.studentUserId])

  const [detail, setDetail] = useState<CoachStudentDetailData | null>(null)
  const [packages, setPackages] = useState<CoachStudentPackageItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [forbidden, setForbidden] = useState(false)
  const [saving, setSaving] = useState(false)

  const [slice, setSlice] = useState<CoachStudentSlice>({
    learningStrokes: null,
    swimLevel: null,
    basics: '',
    notes: ''
  })

  const latestRequestId = useRef(0)

  const loadDetail = async (isRetry = false) => {
    if (!studentUserId) return
    const requestId = ++latestRequestId.current
    setLoading(true)
    if (isRetry) {
      setError(false)
      setForbidden(false)
    }

    try {
      const [detailRes, packageRes] = await Promise.all([
        fetchCoachStudentDetail(studentUserId),
        fetchCoachStudentPackageList(studentUserId)
      ])
      if (requestId !== latestRequestId.current) return
      setDetail(detailRes)
      setPackages(packageRes.packages || [])
      setSlice({
        learningStrokes: detailRes.coachSlice.learningStrokes,
        swimLevel: detailRes.coachSlice.swimLevel,
        basics: detailRes.coachSlice.basics || '',
        notes: detailRes.coachSlice.notes || ''
      })
      setError(false)
      setForbidden(false)
    } catch (err) {
      if (requestId !== latestRequestId.current) return
      const code = (err as { code?: number }).code
      if (code === 410010) {
        setForbidden(true)
      } else {
        setError(true)
      }
    } finally {
      if (requestId === latestRequestId.current) {
        setLoading(false)
      }
    }
  }

  useEffect(() => {
    loadDetail()
  }, [studentUserId])

  const toggleStroke = (stroke: string) => {
    const current = slice.learningStrokes ? slice.learningStrokes.split('/') : []
    const next = current.includes(stroke)
      ? current.filter(s => s !== stroke)
      : [...current, stroke]
    setSlice(prev => ({ ...prev, learningStrokes: next.join('/') || null }))
  }

  const selectLevel = (level: number) => {
    setSlice(prev => ({ ...prev, swimLevel: level }))
  }

  const handleSave = async () => {
    if (!detail || saving) return
    setSaving(true)
    try {
      await updateCoachStudent({
        studentId: studentUserId,
        learningStrokes: slice.learningStrokes || undefined,
        swimLevel: slice.swimLevel ?? undefined,
        basics: slice.basics || undefined,
        notes: slice.notes || undefined,
        idempotencyKey: generateIdempotencyKey()
      })
      Taro.showToast({ title: '保存成功', icon: 'success' })
      setTimeout(() => {
        Taro.navigateBack()
      }, 1500)
    } catch (err) {
      const message = (err as { message?: string }).message || '保存失败，请重试'
      Taro.showToast({ title: message, icon: 'none' })
    } finally {
      setSaving(false)
    }
  }

  const handlePackageClick = (packageId: number) => {
    Taro.navigateTo({
      url: `/pages/student/package-detail/index?packageId=${packageId}&studentUserId=${studentUserId}`
    })
  }

  const renderAvatar = (name: string, avatarUrl: string | null) => {
    if (avatarUrl) {
      return <Image className='detail-avatar__img' src={avatarUrl} mode='aspectFill' />
    }
    const char = name ? name.charAt(0) : '?'
    return <Text className='detail-avatar__text'>{char}</Text>
  }

  const renderSkeleton = () => (
    <View className='student-detail__skeleton'>
      <View className='skeleton-block skeleton-block--header' />
      <View className='skeleton-block skeleton-block--section' />
      <View className='skeleton-block skeleton-block--section' />
      <View className='skeleton-block skeleton-block--section' />
    </View>
  )

  const renderError = () => (
    <View className='student-detail__error'>
      <Text className='student-detail__error-title'>网络异常，请重试</Text>
      <View className='student-detail__error-button' onClick={() => loadDetail(true)}>
        <Text className='student-detail__error-button-text'>重新加载</Text>
      </View>
    </View>
  )

  const renderForbidden = () => (
    <View className='student-detail__error'>
      <Text className='student-detail__error-title'>无权查看该学员</Text>
      <Text className='student-detail__error-desc'>该学员与当前教练无关联</Text>
      <View className='student-detail__error-button' onClick={() => Taro.navigateBack()}>
        <Text className='student-detail__error-button-text'>返回</Text>
      </View>
    </View>
  )

  if (loading) return renderSkeleton()
  if (forbidden) return renderForbidden()
  if (error) return renderError()
  if (!detail) return renderError()

  const { userProfile, summary } = detail

  return (
    <View className='student-detail'>
      <View className='student-detail__content'>
        {/* 学员基础信息区 */}
        <View className='detail-card detail-card--basic'>
          <View className='detail-basic'>
            <View className='detail-avatar'>{renderAvatar(userProfile.name, userProfile.avatarUrl)}</View>
            <View className='detail-basic__info'>
              <View className='detail-basic__name-row'>
                <Text className='detail-basic__name'>{userProfile.name}</Text>
                <Text className='detail-basic__meta'>
                  {formatGenderAge(userProfile.gender, userProfile.age)}
                </Text>
                {userProfile.isMinor && (
                  <View className='detail-basic__minor-tag'>
                    <Text className='detail-basic__minor-tag-text'>未成年</Text>
                  </View>
                )}
              </View>
              <View className='detail-basic__phone-row'>
                <Text className='detail-basic__phone-icon'>📱</Text>
                <Text className='detail-basic__phone'>
                  {userProfile.phoneMasked || '-'}
                </Text>
              </View>
            </View>
          </View>

          {/* 自主档案 */}
          <View className='detail-readonly'>
            <View className='detail-readonly__title'>
              <Text className='detail-readonly__icon'>📝</Text>
              <Text className='detail-readonly__text'>自主档案</Text>
              <View className='detail-readonly__badge'>
                <Text className='detail-readonly__badge-text'>只读</Text>
              </View>
            </View>
            <View className='detail-readonly__rows'>
              <View className='detail-readonly__row'>
                <Text className='detail-readonly__label'>有无游泳基础</Text>
                <Text className='detail-readonly__value'>
                  {userProfile.hasSwimBasis == null
                    ? '-'
                    : userProfile.hasSwimBasis
                      ? '有'
                      : '无'}
                </Text>
              </View>
              <View className='detail-readonly__row'>
                <Text className='detail-readonly__label'>会什么泳姿</Text>
                <Text className='detail-readonly__value'>
                  {userProfile.swimStrokes || '-'}
                </Text>
              </View>
              <View className='detail-readonly__row'>
                <Text className='detail-readonly__label'>游泳年限</Text>
                <Text className='detail-readonly__value'>
                  {userProfile.swimYears || '-'}
                </Text>
              </View>
              <View className='detail-readonly__row'>
                <Text className='detail-readonly__label'>个人描述</Text>
                <Text className='detail-readonly__value'>
                  {userProfile.personalDesc || '-'}
                </Text>
              </View>
              {userProfile.isMinor && (
                <>
                  <View className='detail-readonly__row'>
                    <Text className='detail-readonly__label'>监护人姓名</Text>
                    <Text className='detail-readonly__value'>
                      {userProfile.guardianName || '-'}
                    </Text>
                  </View>
                  <View className='detail-readonly__row'>
                    <Text className='detail-readonly__label'>监护人手机号</Text>
                    <Text className='detail-readonly__value'>
                      {userProfile.guardianPhoneMasked || '-'}
                    </Text>
                  </View>
                </>
              )}
            </View>
          </View>
        </View>

        {/* 关联数据摘要 */}
        <View className='detail-card detail-card--summary'>
          <View className='detail-summary__item'>
            <Text className='detail-summary__value'>{summary.totalHours}</Text>
            <Text className='detail-summary__label'>累计课时</Text>
          </View>
          <View className='detail-summary__divider' />
          <View className='detail-summary__item'>
            <Text className='detail-summary__value detail-summary__value--primary'>
              {summary.remainingHours}
            </Text>
            <Text className='detail-summary__label'>剩余课时</Text>
          </View>
          <View className='detail-summary__divider' />
          <View className='detail-summary__item'>
            <Text className='detail-summary__value detail-summary__value--small'>
              {summary.lastClassDate || '-'}
            </Text>
            <Text className='detail-summary__label'>最近上课</Text>
          </View>
        </View>

        {/* 教学信息编辑区 */}
        <View className='detail-card detail-card--edit'>
          <View className='detail-edit__title'>
            <Text className='detail-edit__icon'>🏊</Text>
            <Text className='detail-edit__text'>教学信息</Text>
          </View>

          <View className='detail-edit__field'>
            <Text className='detail-edit__label'>学习泳姿</Text>
            <View className='detail-edit__options'>
              {STROKE_OPTIONS.map(stroke => {
                const selected = slice.learningStrokes?.includes(stroke)
                return (
                  <View
                    key={stroke}
                    className={`detail-edit__option ${selected ? 'detail-edit__option--active' : ''}`}
                    onClick={() => toggleStroke(stroke)}
                  >
                    <Text
                      className={`detail-edit__option-text ${selected ? 'detail-edit__option-text--active' : ''}`}
                    >
                      {stroke}
                    </Text>
                  </View>
                )
              })}
            </View>
          </View>

          <View className='detail-edit__field'>
            <Text className='detail-edit__label'>游泳等级</Text>
            <View className='detail-edit__options'>
              {LEVEL_OPTIONS.map(level => {
                const selected = slice.swimLevel === level.value
                return (
                  <View
                    key={level.value}
                    className={`detail-edit__option ${selected ? 'detail-edit__option--active' : ''}`}
                    onClick={() => selectLevel(level.value)}
                  >
                    <Text
                      className={`detail-edit__option-text ${selected ? 'detail-edit__option-text--active' : ''}`}
                    >
                      {level.label}
                    </Text>
                  </View>
                )
              })}
            </View>
          </View>

          <View className='detail-edit__field'>
            <View className='detail-edit__label-row'>
              <Text className='detail-edit__label'>基础情况</Text>
              <Text className='detail-edit__count'>
                {(slice.basics || '').length}/{MAX_BASICS_LENGTH}
              </Text>
            </View>
            <Textarea
              className='detail-edit__textarea'
              value={slice.basics}
              onInput={e => setSlice(prev => ({ ...prev, basics: e.detail.value }))}
              maxlength={MAX_BASICS_LENGTH}
              placeholder='记录学员基础情况、注意事项等'
            />
          </View>

          <View className='detail-edit__field'>
            <View className='detail-edit__label-row'>
              <Text className='detail-edit__label'>沟通备注</Text>
              <Text className='detail-edit__count'>
                {(slice.notes || '').length}/{MAX_NOTES_LENGTH}
              </Text>
            </View>
            <Textarea
              className='detail-edit__textarea'
              value={slice.notes}
              onInput={e => setSlice(prev => ({ ...prev, notes: e.detail.value }))}
              maxlength={MAX_NOTES_LENGTH}
              placeholder='记录与学员/家长的沟通重点'
            />
          </View>
        </View>

        {/* 关联套餐 */}
        <View className='detail-card detail-card--packages'>
          <View className='detail-packages__title'>
            <Text className='detail-packages__icon'>📦</Text>
            <Text className='detail-packages__text'>关联套餐</Text>
          </View>
          {packages.length === 0 ? (
            <View className='detail-packages__empty'>
              <Text className='detail-packages__empty-text'>暂无关联套餐</Text>
            </View>
          ) : (
            <View className='detail-packages__list'>
              {packages.map(pkg => (
                <View
                  key={pkg.packageId}
                  className='detail-package-card'
                  onClick={() => handlePackageClick(pkg.packageId)}
                >
                  <View
                    className={`detail-package-card__icon detail-package-card__icon--${pkg.packageMode}`}
                  >
                    <Text className='detail-package-card__icon-text'>
                      {pkg.packageMode === 'experience' ? '体' : '课'}
                    </Text>
                  </View>
                  <View className='detail-package-card__info'>
                    <View className='detail-package-card__name-row'>
                      <Text className='detail-package-card__name'>{pkg.packageName}</Text>
                      <View className={`detail-package-card__status detail-package-card__status--${pkg.status}`}>
                        <Text className='detail-package-card__status-text'>{pkg.statusLabel}</Text>
                      </View>
                    </View>
                    <Text className='detail-package-card__meta'>
                      {formatPackageMeta(pkg)}
                    </Text>
                  </View>
                  <Text className='detail-package-card__arrow'>›</Text>
                </View>
              ))}
            </View>
          )}
        </View>
      </View>

      {/* 底部保存按钮 */}
      <View className='student-detail__footer'>
        <View
          className={`student-detail__save ${saving ? 'student-detail__save--disabled' : ''}`}
          onClick={handleSave}
        >
          <Text className='student-detail__save-text'>{saving ? '保存中...' : '保存'}</Text>
        </View>
      </View>
    </View>
  )
}

function formatGenderAge(gender: string | null, age: number | null): string {
  const parts: string[] = []
  if (gender) parts.push(gender)
  if (age != null) parts.push(`${age}岁`)
  return parts.join(' · ') || ''
}

function formatPackageMeta(pkg: CoachStudentPackageItem): string {
  const parts: string[] = []
  if (pkg.validStart) {
    parts.push(`购买于 ${pkg.validStart}`)
  }
  if (pkg.status === 'exhausted') {
    parts.push('已用完')
  } else {
    parts.push(`剩余${pkg.remainingHours}课时`)
  }
  return parts.join(' · ')
}

function generateIdempotencyKey(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}
