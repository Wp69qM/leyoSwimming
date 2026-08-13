import { useState } from 'react'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Image, Button } from '@tarojs/components'
import { getProfile, type CoachProfile } from '@/api/profile'
import { logoutCoach } from '@/api/auth'
import { handleBusinessError } from '@/api/request'
import { useAuthStore } from '@/stores/authStore'
import { COACH_STATUS } from '@/constants'
import './index.scss'

const STATUS_TEXT: Record<number, string> = {
  [COACH_STATUS.UNDER_REVIEW]: '待审核',
  [COACH_STATUS.APPROVED]: '在职',
  [COACH_STATUS.REJECTED]: '已驳回',
  [COACH_STATUS.RESIGNED]: '已离职',
  [COACH_STATUS.RESIGNING]: '申请离职中',
  [COACH_STATUS.PENDING_ONBOARDING]: '待入驻',
}

const STATUS_CARD_TITLE: Record<number, string> = {
  [COACH_STATUS.UNDER_REVIEW]: '资料审核中',
  [COACH_STATUS.REJECTED]: '入驻申请被驳回',
  [COACH_STATUS.RESIGNED]: '已离职',
  [COACH_STATUS.RESIGNING]: '离职申请中',
}

const STATUS_TAG_CLASS: Record<number, string> = {
  [COACH_STATUS.UNDER_REVIEW]: 'mine__tag--warning',
  [COACH_STATUS.APPROVED]: 'mine__tag--success',
  [COACH_STATUS.REJECTED]: 'mine__tag--danger',
  [COACH_STATUS.RESIGNED]: 'mine__tag--danger',
  [COACH_STATUS.RESIGNING]: 'mine__tag--warning',
  [COACH_STATUS.PENDING_ONBOARDING]: 'mine__tag--default',
}

const STATUS_CARD_CLASS: Record<number, string> = {
  [COACH_STATUS.UNDER_REVIEW]: 'mine__status-card--info',
  [COACH_STATUS.REJECTED]: 'mine__status-card--danger',
  [COACH_STATUS.RESIGNED]: 'mine__status-card--danger',
  [COACH_STATUS.RESIGNING]: 'mine__status-card--warning',
}

type IconName = 'user' | 'tag' | 'wallet' | 'calendar' | 'file-text' | 'log-out' | 'refresh' | 'check-circle'

function MenuItem({
  icon,
  text,
  value,
  variant = 'default',
  onClick,
}: {
  icon: IconName
  text: string
  value?: string
  variant?: 'default' | 'danger' | 'primary'
  onClick: () => void
}) {
  return (
    <View className={`mine__menu-item mine__menu-item--${variant}`} onClick={onClick}>
      <View className={`mine__menu-icon mine__menu-icon--${icon}`} />
      <Text className='mine__menu-text'>{text}</Text>
      {value && <Text className='mine__menu-value'>{value}</Text>}
      <Text className='mine__menu-arrow'>›</Text>
    </View>
  )
}

export default function MinePage() {
  const [profile, setProfile] = useState<CoachProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const { accessToken, refreshToken, logout } = useAuthStore()

  async function loadProfile(cancelledRef?: { cancelled: boolean }) {
    try {
      const data = await getProfile()
      if (cancelledRef?.cancelled) return
      setProfile(data)
      setError(false)
    } catch (err) {
      if (cancelledRef?.cancelled) return
      setError(true)
      Taro.showToast({ title: handleBusinessError(err), icon: 'none' })
    } finally {
      if (!cancelledRef?.cancelled) setLoading(false)
    }
  }

  useDidShow(() => {
    if (!accessToken) {
      Taro.redirectTo({ url: '/pages/login/wechat/index' })
      return
    }
    const cancelledRef = { cancelled: false }
    setLoading(true)
    setError(false)
    loadProfile(cancelledRef)
    return () => {
      cancelledRef.cancelled = true
    }
  })

  function handleLogout() {
    Taro.showModal({
      title: '确认退出登录？',
      content: '退出后将清除本地登录状态',
      success: async (res) => {
        if (!res.confirm) return
        try {
          if (refreshToken) {
            await logoutCoach(refreshToken)
          }
        } catch (err) {
          Taro.showToast({ title: handleBusinessError(err), icon: 'none' })
        } finally {
          logout()
          Taro.redirectTo({ url: '/pages/login/wechat/index' })
        }
      },
    })
  }

  function handleRetry() {
    setLoading(true)
    setError(false)
    loadProfile()
  }

  function handleSettings() {
    Taro.showActionSheet({
      itemList: ['退出登录'],
      success: (res) => {
        if (res.tapIndex === 0) {
          handleLogout()
        }
      },
    })
  }

  if (loading) {
    return (
      <View className='mine mine--loading'>
        <Text className='mine__loading-text'>加载中…</Text>
      </View>
    )
  }

  if (error || !profile) {
    return (
      <View className='mine mine--error'>
        <Text className='mine__error-text'>加载失败</Text>
        <Button className='mine__retry' onClick={handleRetry}>
          点击重试
        </Button>
      </View>
    )
  }

  const status = profile.status
  const statusText = STATUS_TEXT[status] ?? '未知状态'

  const showProfileEdit = status === COACH_STATUS.APPROVED || status === COACH_STATUS.RESIGNING
  const showReferencePrice = showProfileEdit
  const showFullMenu = showProfileEdit
  const showResignApply = status === COACH_STATUS.APPROVED
  const showResignView = status === COACH_STATUS.RESIGNING
  const showReapply = status === COACH_STATUS.RESIGNED
  const showOnboarding =
    status === COACH_STATUS.UNDER_REVIEW ||
    status === COACH_STATUS.REJECTED ||
    status === COACH_STATUS.PENDING_ONBOARDING
  const showStatusCard =
    status === COACH_STATUS.UNDER_REVIEW ||
    status === COACH_STATUS.REJECTED ||
    status === COACH_STATUS.RESIGNED ||
    status === COACH_STATUS.RESIGNING

  function navigateTo(url: string) {
    Taro.navigateTo({ url }).catch(() => {
      Taro.switchTab({ url })
    })
  }

  const displayName = profile.name || '未设置昵称'
  const surname = displayName.charAt(0)
  const givenName = displayName.slice(1)

  return (
    <View className='mine'>
      <View className='mine__navbar'>
        <Text className='mine__navbar-title'>我的</Text>
        <View className='mine__navbar-settings' onClick={handleSettings} />
      </View>

      <View className='mine__card mine__profile' onClick={() => navigateTo('/pages/profile/edit/index')}>
        <View className='mine__avatar'>
          {profile.portraitUrl || profile.avatarUrl ? (
            <Image
              className='mine__avatar-img'
              src={profile.portraitUrl || profile.avatarUrl || ''}
              mode='aspectFill'
            />
          ) : (
            <Text className='mine__avatar-text'>{surname || '?'}</Text>
          )}
        </View>
        <View className='mine__info'>
          <View className='mine__name-row'>
            <Text className='mine__surname'>{surname}</Text>
            <Text className='mine__given-name'>{givenName}</Text>
          </View>
          <Text className={`mine__tag ${STATUS_TAG_CLASS[status] || 'mine__tag--default'}`}>{statusText}</Text>
        </View>
      </View>

      {showStatusCard && (
        <View className={`mine__status-card ${STATUS_CARD_CLASS[status] || ''}`}>
          <Text className='mine__status-title'>{STATUS_CARD_TITLE[status] ?? statusText}</Text>
          <Text className='mine__status-desc'>
            {status === COACH_STATUS.UNDER_REVIEW && '审核通常需 1-3 个工作日'}
            {status === COACH_STATUS.REJECTED && `原因：${profile.rejectionReason || '资料不符合要求'}`}
            {status === COACH_STATUS.RESIGNED && '教学资格已冻结，仅可进行重新入驻'}
            {status === COACH_STATUS.RESIGNING && '学员端购买入口已隐藏，可继续上课'}
          </Text>
          {status === COACH_STATUS.RESIGNING && (
            <Text
              className='mine__status-link'
              onClick={(e) => {
                e.stopPropagation()
                Taro.navigateTo({ url: '/pages/resignation/processing/index' })
              }}
            >
              查看离职申请
            </Text>
          )}
        </View>
      )}

      <View className={`mine__menu ${showStatusCard ? 'mine__menu--after-status' : ''}`}>
        {showOnboarding && (
          <MenuItem
            icon='file-text'
            text='入驻资料'
            onClick={() => Taro.navigateTo({ url: '/pages/onboarding/index/index' })}
          />
        )}
        {showProfileEdit && (
          <MenuItem icon='user' text='个人主页编辑' onClick={() => navigateTo('/pages/profile/edit/index')} />
        )}
        {showReferencePrice && (
          <MenuItem
            icon='tag'
            text='参考单价设置'
            value={profile.referencePrice ? `${profile.referencePrice} 元/节` : '未设置'}
            onClick={() => navigateTo('/pages/profile/reference-price/index')}
          />
        )}
        {showFullMenu && (
          <>
            <MenuItem icon='wallet' text='我的收入' onClick={() => Taro.showToast({ title: '功能开发中', icon: 'none' })} />
            <MenuItem icon='calendar' text='排班管理' onClick={() => Taro.showToast({ title: '功能开发中', icon: 'none' })} />
            <MenuItem icon='file-text' text='请假申请' onClick={() => Taro.showToast({ title: '功能开发中', icon: 'none' })} />
          </>
        )}
        {showResignApply && (
          <MenuItem
            icon='log-out'
            text='申请离职'
            variant='danger'
            onClick={() => Taro.navigateTo({ url: '/pages/resignation/apply/index' })}
          />
        )}
        {showResignView && (
          <MenuItem
            icon='check-circle'
            text='查看离职申请'
            variant='primary'
            onClick={() => Taro.navigateTo({ url: '/pages/resignation/processing/index' })}
          />
        )}
        {showReapply && (
          <MenuItem
            icon='refresh'
            text='重新入驻'
            variant='primary'
            onClick={() => Taro.navigateTo({ url: '/pages/onboarding/index/index' })}
          />
        )}
      </View>
    </View>
  )
}
