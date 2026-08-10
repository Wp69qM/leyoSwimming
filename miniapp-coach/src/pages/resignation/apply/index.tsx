import { useEffect, useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Textarea, Button } from '@tarojs/components'
import { applyResignation, getResignationDetail } from '@/api/resignation'
import { handleBusinessError } from '@/api/request'
import { useAuthStore } from '@/stores/authStore'
import { COACH_STATUS } from '@/constants'
import './index.scss'

const MAX_REASON_LENGTH = 200

export default function ResignationApplyPage() {
  const [reason, setReason] = useState('')
  const [totalPackages, setTotalPackages] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)
  const [checking, setChecking] = useState(true)
  const [errorTip, setErrorTip] = useState('')
  const coachInfo = useAuthStore((state) => state.coachInfo)
  const restoreFromStorage = useAuthStore((state) => state.restoreFromStorage)

  useEffect(() => {
    restoreFromStorage()
  }, [restoreFromStorage])

  useEffect(() => {
    Taro.setNavigationBarTitle({ title: '申请离职' })
  }, [])

  useEffect(() => {
    if (!coachInfo) return

    if (coachInfo.status !== COACH_STATUS.APPROVED) {
      if (coachInfo.status === COACH_STATUS.RESIGNING) {
        Taro.redirectTo({ url: '/pages/resignation/processing/index' })
      }
      return
    }

    async function loadDraft() {
      try {
        const detail = await getResignationDetail({})
        if (detail && detail.totalPackages > 0) {
          setTotalPackages(detail.totalPackages)
        }
      } catch {
        // 无草稿或接口异常时不阻塞页面，仍允许提交申请
      } finally {
        setChecking(false)
      }
    }

    void loadDraft()
  }, [coachInfo])

  function handleReasonChange(value: string) {
    if (value.length <= MAX_REASON_LENGTH) {
      setReason(value)
    }
  }

  function navigateToTicket(ticketId?: number) {
    const url = ticketId
      ? `/pages/resignation/ticket/index?ticketId=${ticketId}`
      : '/pages/resignation/ticket/index'
    Taro.navigateTo({ url })
  }

  function navigateToProcessing(ticketId?: number) {
    const url = ticketId
      ? `/pages/resignation/processing/index?ticketId=${ticketId}`
      : '/pages/resignation/processing/index'
    Taro.redirectTo({ url })
  }

  function handlePackageEntryClick() {
    if (checking) return
    navigateToTicket()
  }

  async function handleSubmit() {
    if (coachInfo?.status !== COACH_STATUS.APPROVED) {
      setErrorTip('当前状态不可申请离职')
      return
    }

    const confirm = await Taro.showModal({
      title: '确认提交离职申请？',
      content: '提交后学员端将不再展示您的购买入口，且需处理名下学员套餐。',
      confirmText: '确认提交',
      confirmColor: '#FF4D4F',
      cancelText: '再想想'
    })

    if (!confirm.confirm) return

    setLoading(true)
    setErrorTip('')

    try {
      const result = await applyResignation({ reason: reason.trim() || undefined })
      Taro.showToast({ title: '申请已提交', icon: 'success' })
      navigateToProcessing(result.ticketId)
    } catch (error) {
      setErrorTip(handleBusinessError(error))
    } finally {
      setLoading(false)
    }
  }

  const isApproved = coachInfo?.status === COACH_STATUS.APPROVED
  const canSubmit = isApproved && !loading && !checking

  if (!coachInfo || checking) {
    return (
      <View className='resignation-apply'>
        <View className='resignation-apply__loading'>
          <Text className='resignation-apply__loading-text'>加载中...</Text>
        </View>
      </View>
    )
  }

  if (!isApproved) {
    return (
      <View className='resignation-apply'>
        <View className='resignation-apply__error'>
          <Text className='resignation-apply__error-title'>当前状态不可申请离职</Text>
          <Text className='resignation-apply__error-desc'>仅已通过入驻的教练可申请离职</Text>
        </View>
      </View>
    )
  }

  return (
    <View className='resignation-apply'>
      <View className='resignation-apply__card resignation-apply__risk'>
        <Text className='resignation-apply__risk-title'>离职影响说明</Text>
        <View className='resignation-apply__risk-list'>
          <Text className='resignation-apply__risk-item'>1. 提交后学员端不再展示您的购买入口</Text>
          <Text className='resignation-apply__risk-item'>2. 需处理名下 active 学员套餐</Text>
          <Text className='resignation-apply__risk-item'>3. 审批通过后教学资格将冻结</Text>
        </View>
      </View>

      <View className='resignation-apply__card resignation-apply__reason'>
        <Text className='resignation-apply__reason-label'>离职原因（选填）</Text>
        <Textarea
          className='resignation-apply__reason-input'
          placeholder='请填写离职原因，方便我们改进服务'
          value={reason}
          onInput={(e) => handleReasonChange(e.detail.value)}
          maxlength={MAX_REASON_LENGTH}
          autoHeight
        />
        <Text className='resignation-apply__reason-count'>
          {reason.length}/{MAX_REASON_LENGTH}
        </Text>
      </View>

      <View className='resignation-apply__card resignation-apply__entry' onClick={handlePackageEntryClick}>
        <Text className='resignation-apply__entry-label'>待处理学员套餐</Text>
        <View className='resignation-apply__entry-right'>
          {totalPackages !== null && totalPackages > 0 ? (
            <Text className='resignation-apply__entry-count'>{totalPackages} 份</Text>
          ) : null}
          <Text className='resignation-apply__entry-arrow'>&gt;</Text>
        </View>
      </View>

      {errorTip ? (
        <View className='resignation-apply__error-tip'>
          <Text className='resignation-apply__error-tip-text'>{errorTip}</Text>
        </View>
      ) : null}

      <View className='resignation-apply__footer'>
        <Button
          className={`resignation-apply__submit ${!canSubmit ? 'resignation-apply__submit--disabled' : ''}`}
          onClick={handleSubmit}
          disabled={!canSubmit}
          loading={loading}
        >
          {loading ? '提交中...' : '提交离职申请'}
        </Button>
      </View>
    </View>
  )
}
