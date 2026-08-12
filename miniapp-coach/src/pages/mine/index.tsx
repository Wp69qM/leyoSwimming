import { useEffect, useState } from 'react'
import Taro from '@tarojs/taro'
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

const STATUS_TAG_CLASS: Record<number, string> = {
  [COACH_STATUS.UNDER_REVIEW]: 'mine__tag--warning',
  [COACH_STATUS.APPROVED]: 'mine__tag--success',
  [COACH_STATUS.REJECTED]: 'mine__tag--danger',
  [COACH_STATUS.RESIGNED]: 'mine__tag--danger',
  [COACH_STATUS.RESIGNING]: 'mine__tag--warning',
  [COACH_STATUS.PENDING_ONBOARDING]: 'mine__tag--default',
}

export default function MinePage() {
  const [profile, setProfile] = useState<CoachProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const { accessToken, refreshToken, logout } = useAuthStore()

  useEffect(() => {
    if (!accessToken) {
      Taro.redirectTo({ url: '/pages/login/wechat/index' })
      return
    }
    let cancelled = false
    async function load() {
      try {
        const data = await getProfile()
        if (cancelled) return
        setProfile(data)
        setError(false)
      } catch (err) {
        if (cancelled) return
        setError(true)
        Taro.showToast({ title: handleBusinessError(err), icon: 'none' })
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [accessToken])

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

  async function loadProfile() {
    try {
      const data = await getProfile()
      setProfile(data)
      setError(false)
    } catch (err) {
      setError(true)
      Taro.showToast({ title: handleBusinessError(err), icon: 'none' })
    } finally {
      setLoading(false)
    }
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
  const showOnboarding = status === COACH_STATUS.UNDER_REVIEW || status === COACH_STATUS.REJECTED || status === COACH_STATUS.PENDING_ONBOARDING
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

  return (
    <View className='mine'>
      <View className='mine__header'>
        <Text className='mine__header-title'>我的</Text>
      </View>

      <View className='mine__card mine__profile' onClick={() => navigateTo('/pages/profile/edit/index')}>
        <View className='mine__avatar'>
          {profile.portraitUrl || profile.avatarUrl ? (
            <Image className='mine__avatar-img' src={profile.portraitUrl || profile.avatarUrl || ''} mode='aspectFill' />
          ) : (
            <Text className='mine__avatar-text'>{profile.name?.charAt(0) || '?'}</Text>
          )}
        </View>
        <View className='mine__info'>
          <Text className='mine__name'>{profile.name || '未设置昵称'}</Text>
          <Text className={`mine__tag ${STATUS_TAG_CLASS[status] || 'mine__tag--default'}`}>{statusText}</Text>
        </View>
      </View>

      {showStatusCard && (
        <View className={`mine__status-card ${STATUS_TAG_CLASS[status].replace('mine__tag', 'mine__status')}`}>
          <Text className='mine__status-title'>{statusText}</Text>
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

      <View className='mine__menu'>
        {showOnboarding && (
          <View className='mine__menu-item' onClick={() => Taro.navigateTo({ url: '/pages/onboarding/index/index' })}>
            <Text className='mine__menu-text'>入驻资料</Text>
            <Text className='mine__menu-arrow'>›</Text>
          </View>
        )}
        {showProfileEdit && (
          <View className='mine__menu-item' onClick={() => navigateTo('/pages/profile/edit/index')}>
            <Text className='mine__menu-text'>个人主页编辑</Text>
            <Text className='mine__menu-arrow'>›</Text>
          </View>
        )}
        {showReferencePrice && (
          <View className='mine__menu-item' onClick={() => navigateTo('/pages/profile/reference-price/index')}>
            <Text className='mine__menu-text'>参考单价设置</Text>
            <Text className='mine__menu-value'>
              {profile.referencePrice ? `${profile.referencePrice} 元/节` : '未设置'}
            </Text>
            <Text className='mine__menu-arrow'>›</Text>
          </View>
        )}
        {showFullMenu && (
          <>
            <View className='mine__menu-item' onClick={() => Taro.showToast({ title: '功能开发中', icon: 'none' })}>
              <Text className='mine__menu-text'>我的收入</Text>
              <Text className='mine__menu-arrow'>›</Text>
            </View>
            <View className='mine__menu-item' onClick={() => Taro.showToast({ title: '功能开发中', icon: 'none' })}>
              <Text className='mine__menu-text'>排班管理</Text>
              <Text className='mine__menu-arrow'>›</Text>
            </View>
            <View className='mine__menu-item' onClick={() => Taro.showToast({ title: '功能开发中', icon: 'none' })}>
              <Text className='mine__menu-text'>请假申请</Text>
              <Text className='mine__menu-arrow'>›</Text>
            </View>
          </>
        )}
        {showResignApply && (
          <View className='mine__menu-item mine__menu-item--danger' onClick={() => Taro.navigateTo({ url: '/pages/resignation/apply/index' })}>
            <Text className='mine__menu-text'>申请离职</Text>
            <Text className='mine__menu-arrow'>›</Text>
          </View>
        )}
        {showResignView && (
          <View className='mine__menu-item mine__menu-item--primary' onClick={() => Taro.navigateTo({ url: '/pages/resignation/processing/index' })}>
            <Text className='mine__menu-text'>查看离职申请</Text>
            <Text className='mine__menu-arrow'>›</Text>
          </View>
        )}
        {showReapply && (
          <View className='mine__menu-item mine__menu-item--primary' onClick={() => Taro.navigateTo({ url: '/pages/onboarding/index/index' })}>
            <Text className='mine__menu-text'>重新入驻</Text>
            <Text className='mine__menu-arrow'>›</Text>
          </View>
        )}
      </View>

      <Button className='mine__logout' onClick={handleLogout}>
        退出登录
      </Button>
    </View>
  )
}
