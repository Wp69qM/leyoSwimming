import { useEffect, useMemo, useRef, useState } from 'react'
import Taro, { useRouter } from '@tarojs/taro'
import { View, Text, Image } from '@tarojs/components'
import { fetchCoachPackageDetail } from '@/api/student'
import type {
  CoachPackageDetailData,
  CoachPackageUsageRecord
} from '@/types/student'
import './index.scss'

const MODE_LABELS: Record<string, string> = {
  standard: '正价课',
  experience: '体验课'
}

const STATUS_THEME: Record<string, { bg: string; text: string }> = {
  active: { bg: '#F6FFED', text: '#52C41A' },
  exhausted: { bg: '#F5F7FA', text: '#8C8C8C' },
  expired: { bg: '#FFF7E6', text: '#FA8C16' },
  refunded: { bg: '#FFF1F0', text: '#FF4D4F' },
  frozen: { bg: '#FFF1F0', text: '#FF4D4F' }
}

const RECORD_THEME: Record<string, { bg: string; text: string; icon: string }> = {
  completed: { bg: '#F6FFED', text: '#52C41A', icon: '✓' },
  cancelled: { bg: '#FFF1F0', text: '#FF4D4F', icon: '✕' },
  booked: { bg: '#E6F7FF', text: '#1890FF', icon: '◷' },
  confirmed: { bg: '#E6F7FF', text: '#1890FF', icon: '◷' },
  teaching: { bg: '#E6F7FF', text: '#1890FF', icon: '◷' }
}

export default function StudentPackageDetailPage() {
  const router = useRouter()
  const packageId = useMemo(() => {
    const raw = router.params.packageId
    return raw ? Number(raw) : 0
  }, [router.params.packageId])

  const [detail, setDetail] = useState<CoachPackageDetailData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [forbidden, setForbidden] = useState(false)

  const latestRequestId = useRef(0)

  const loadDetail = async (isRetry = false) => {
    if (!packageId) return
    const requestId = ++latestRequestId.current
    setLoading(true)
    if (isRetry) {
      setError(false)
      setForbidden(false)
    }

    try {
      const res = await fetchCoachPackageDetail({ packageId })
      if (requestId !== latestRequestId.current) return
      setDetail(res)
      setError(false)
      setForbidden(false)
    } catch (err) {
      if (requestId !== latestRequestId.current) return
      const code = (err as { code?: number }).code
      if (code === 410005 || code === 420001) {
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
  }, [packageId])

  const handleStudentClick = () => {
    if (!detail) return
    Taro.navigateTo({
      url: `/pages/student/detail/index?studentUserId=${detail.student.userId}`
    })
  }

  const renderSkeleton = () => (
    <View className='package-detail__skeleton'>
      <View className='skeleton-block skeleton-block--card' />
      <View className='skeleton-block skeleton-block--summary' />
      <View className='skeleton-block skeleton-block--student' />
      <View className='skeleton-block skeleton-block--records' />
    </View>
  )

  const renderError = () => (
    <View className='package-detail__error'>
      <Text className='package-detail__error-title'>网络异常，请重试</Text>
      <View className='package-detail__error-button' onClick={() => loadDetail(true)}>
        <Text className='package-detail__error-button-text'>重新加载</Text>
      </View>
    </View>
  )

  const renderForbidden = () => (
    <View className='package-detail__error'>
      <Text className='package-detail__error-title'>无权查看该套餐</Text>
      <Text className='package-detail__error-desc'>该套餐与当前教练无关联</Text>
      <View className='package-detail__error-button' onClick={() => Taro.navigateBack()}>
        <Text className='package-detail__error-button-text'>返回</Text>
      </View>
    </View>
  )

  if (loading) return renderSkeleton()
  if (forbidden) return renderForbidden()
  if (error) return renderError()
  if (!detail) return renderError()

  const theme = STATUS_THEME[detail.status] || STATUS_THEME.active
  const modeLabel = MODE_LABELS[detail.packageMode] || detail.packageMode

  return (
    <View className='package-detail'>
      <View className='package-detail__content'>
        {/* 套餐主信息卡 */}
        <View className='pd-card pd-card--main'>
          <View className='pd-main__header'>
            <View className='pd-main__title-row'>
              <View
                className={`pd-badge pd-badge--mode pd-badge--${detail.packageMode}`}
              >
                <Text className='pd-badge__text'>{modeLabel}</Text>
              </View>
              <Text className='pd-main__name'>{detail.packageName}</Text>
            </View>
            <View
              className='pd-badge pd-badge--status'
              style={{ backgroundColor: theme.bg }}
            >
              <Text className='pd-badge__text' style={{ color: theme.text }}>
                {detail.statusLabel}
              </Text>
            </View>
          </View>

          <View className='pd-info-list'>
            <View className='pd-info-row'>
              <Text className='pd-info-row__label'>有效期</Text>
              <Text className='pd-info-row__value'>
                {formatValidRange(detail.validStart, detail.validEnd)}
              </Text>
            </View>
            <View className='pd-info-row'>
              <Text className='pd-info-row__label'>教学类型</Text>
              <Text className='pd-info-row__value'>{detail.teachingType || '-'}</Text>
            </View>
            {detail.strokeNames && (
              <View className='pd-info-row'>
                <Text className='pd-info-row__label'>泳姿</Text>
                <Text className='pd-info-row__value'>{detail.strokeNames}</Text>
              </View>
            )}
            <View className='pd-info-row'>
              <Text className='pd-info-row__label'>每节课时长</Text>
              <Text className='pd-info-row__value'>
                {detail.durationMinutes ? `${detail.durationMinutes} 分钟/节` : '-'}
              </Text>
            </View>
            <View className='pd-info-row'>
              <Text className='pd-info-row__label'>课时</Text>
              <Text className='pd-info-row__value'>
                剩余 {detail.availableHours} 课时 · 共 {detail.totalHours} 课时
              </Text>
            </View>
          </View>
        </View>

        {/* 套餐统计摘要 */}
        <View className='pd-card pd-card--summary'>
          <View className='pd-summary__item'>
            <Text className='pd-summary__value'>{detail.totalHours}</Text>
            <Text className='pd-summary__label'>总课时</Text>
          </View>
          <View className='pd-summary__divider' />
          <View className='pd-summary__item'>
            <Text className='pd-summary__value pd-summary__value--orange'>
              {detail.consumedHours}
            </Text>
            <Text className='pd-summary__label'>已用课时</Text>
          </View>
          <View className='pd-summary__divider' />
          <View className='pd-summary__item'>
            <Text className='pd-summary__value pd-summary__value--primary'>
              {detail.availableHours}
            </Text>
            <Text className='pd-summary__label'>剩余课时</Text>
          </View>
        </View>

        {/* 学员迷你卡 */}
        <View className='pd-card pd-card--student' onClick={handleStudentClick}>
          <View className='pd-student__avatar'>
            {detail.student.avatarUrl ? (
              <Image
                className='pd-student__avatar-img'
                src={detail.student.avatarUrl}
                mode='aspectFill'
              />
            ) : (
              <Text className='pd-student__avatar-text'>
                {detail.student.name ? detail.student.name.charAt(0) : '?'}
              </Text>
            )}
          </View>
          <View className='pd-student__info'>
            <Text className='pd-student__name'>{detail.student.name}</Text>
            <Text className='pd-student__meta'>
              {formatStudentMeta(detail.student.age, detail.student.gender)}
            </Text>
          </View>
          <Text className='pd-student__arrow'>›</Text>
        </View>

        {/* 使用记录列表 */}
        <View className='pd-card pd-card--records'>
          <View className='pd-section-title'>
            <Text className='pd-section-title__icon'>📋</Text>
            <Text className='pd-section-title__text'>使用记录</Text>
          </View>
          {detail.usageRecords.length === 0 ? (
            <View className='pd-records__empty'>
              <Text className='pd-records__empty-text'>暂无使用记录</Text>
            </View>
          ) : (
            <View className='pd-records__list'>
              {detail.usageRecords.map(record => (
                <UsageRecordItem key={record.bookingId} record={record} />
              ))}
            </View>
          )}
        </View>

        {/* 套餐规则说明区 */}
        <View className='pd-card pd-card--rules'>
          <View className='pd-section-title'>
            <Text className='pd-section-title__icon'>📖</Text>
            <Text className='pd-section-title__text'>套餐规则</Text>
          </View>
          <View className='pd-info-list'>
            <View className='pd-info-row'>
              <Text className='pd-info-row__label'>购买时间</Text>
              <Text className='pd-info-row__value'>{detail.validStart || '-'}</Text>
            </View>
            <View className='pd-info-row'>
              <Text className='pd-info-row__label'>退款规则</Text>
              <Text className='pd-info-row__value'>
                {detail.packageMode === 'experience'
                  ? '体验课不支持退款。'
                  : '已使用课时超过 50% 不支持退款；未超过可申请按剩余课时比例退款。'}
              </Text>
            </View>
            <View className='pd-info-row'>
              <Text className='pd-info-row__label'>过期规则</Text>
              <Text className='pd-info-row__value'>
                套餐有效期至 {detail.validEnd || '-'}，过期后剩余课时自动作废。
              </Text>
            </View>
            <View className='pd-info-row'>
              <Text className='pd-info-row__label'>冻结规则</Text>
              <Text className='pd-info-row__value'>
                套餐冻结期间不计入有效期，每次冻结最长 30 天。
              </Text>
            </View>
          </View>
        </View>

        <View className='package-detail__bottom-space' />
      </View>
    </View>
  )
}

function UsageRecordItem({ record }: { record: CoachPackageUsageRecord }) {
  const theme = RECORD_THEME[record.status] || RECORD_THEME.booked
  const metaParts = [`消耗 ${record.consumedHours} 课时`]
  if (record.cancelReason) {
    metaParts.push(record.cancelReason)
  }

  return (
    <View className='pd-record-item'>
      <View className='pd-record-item__icon' style={{ backgroundColor: theme.bg }}>
        <Text className='pd-record-item__icon-text' style={{ color: theme.text }}>
          {theme.icon}
        </Text>
      </View>
      <View className='pd-record-item__info'>
        <Text className='pd-record-item__time'>{record.startTime}</Text>
        <Text className='pd-record-item__meta'>{metaParts.join(' · ')}</Text>
      </View>
      <View className='pd-badge pd-badge--status' style={{ backgroundColor: theme.bg }}>
        <Text className='pd-badge__text' style={{ color: theme.text }}>
          {record.statusLabel}
        </Text>
      </View>
    </View>
  )
}

function formatValidRange(start: string | null, end: string | null): string {
  if (!start && !end) return '-'
  return `${start || ''} 至 ${end || ''}`
}

function formatStudentMeta(age: number | null, gender: string | null): string {
  const parts: string[] = []
  if (age != null) parts.push(`${age} 岁`)
  if (gender) parts.push(gender)
  return parts.join(' · ') || '-'
}
