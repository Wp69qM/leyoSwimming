import { useEffect, useMemo, useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Button } from '@tarojs/components'
import { getResignationDetail } from '@/api/resignation'
import { handleBusinessError } from '@/api/request'
import { useAuthStore } from '@/stores/authStore'
import { COACH_STATUS } from '@/constants'
import type { ResignationTicket } from '@/types/resignation'
import './index.scss'

const PROGRESS_TEXT: Record<string, string> = {
  processing: '等待教练处理学员套餐',
  pending_audit: '已提交管理员审批'
}

export default function ResignationProcessingPage() {
  const [ticket, setTicket] = useState<ResignationTicket | null>(null)
  const [loading, setLoading] = useState(true)
  const [errorTip, setErrorTip] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const coachInfo = useAuthStore((state) => state.coachInfo)
  const restoreFromStorage = useAuthStore((state) => state.restoreFromStorage)

  const ticketId = useMemo(() => {
    const params = Taro.getCurrentInstance().router?.params
    return params?.ticketId ? Number(params.ticketId) : undefined
  }, [])

  useEffect(() => {
    restoreFromStorage()
  }, [restoreFromStorage])

  useEffect(() => {
    Taro.setNavigationBarTitle({ title: '离职处理' })
  }, [])

  useEffect(() => {
    if (!coachInfo) return

    if (coachInfo.status !== COACH_STATUS.RESIGNING) {
      setLoading(false)
      setErrorTip('当前无进行中的离职申请')
      return
    }

    async function loadDetail() {
      setLoading(true)
      setErrorTip('')
      try {
        const detail = await getResignationDetail({ ticketId })
        setTicket(detail)
      } catch (error) {
        setErrorTip(handleBusinessError(error))
      } finally {
        setLoading(false)
      }
    }

    void loadDetail()
  }, [coachInfo, ticketId, refreshKey])

  function handleRetry() {
    setRefreshKey((prev) => prev + 1)
  }

  function navigateToTicket() {
    const url = ticket?.ticketId
      ? `/pages/resignation/ticket/index?ticketId=${ticket.ticketId}`
      : '/pages/resignation/ticket/index'
    Taro.navigateTo({ url })
  }

  function handleContactService() {
    Taro.showToast({ title: '客服功能开发中', icon: 'none' })
  }

  const progressText = useMemo(() => {
    if (ticket?.status && PROGRESS_TEXT[ticket.status]) {
      return PROGRESS_TEXT[ticket.status]
    }
    return '处理中'
  }, [ticket])

  if (loading) {
    return (
      <View className='resignation-processing'>
        <View className='resignation-processing__loading'>
          <Text className='resignation-processing__loading-text'>加载中...</Text>
        </View>
      </View>
    )
  }

  if (errorTip) {
    return (
      <View className='resignation-processing'>
        <View className='resignation-processing__error'>
          <Text className='resignation-processing__error-text'>{errorTip}</Text>
          <Button className='resignation-processing__error-btn' onClick={handleRetry}>
            重新加载
          </Button>
        </View>
      </View>
    )
  }

  return (
    <View className='resignation-processing'>
      <View className='resignation-processing__status'>
        <View className='resignation-processing__icon'>
          <Text className='resignation-processing__icon-symbol'>⏳</Text>
        </View>
        <Text className='resignation-processing__title'>离职申请已提交，正在处理中</Text>
        <Text className='resignation-processing__subtitle'>请耐心等待管理员审批</Text>
      </View>

      <View className='resignation-processing__card'>
        <View className='resignation-processing__card-header'>
          <Text className='resignation-processing__card-title'>工单进度</Text>
          <View className='resignation-processing__status-tag'>
            <Text className='resignation-processing__status-tag-text'>处理中</Text>
          </View>
        </View>
        <Text className='resignation-processing__ticket-no'>
          工单号：{ticket?.ticketNo ?? '--'}
        </Text>
        <Text className='resignation-processing__current-progress'>{progressText}</Text>
        <Text className='resignation-processing__meta'>
          更新时间：{ticket?.updatedAt ?? '--'}
        </Text>
        <Text className='resignation-processing__meta'>
          {ticket?.estimatedProcessTime ?? '预计 1-3 个工作日内完成审批'}
        </Text>
      </View>

      <View className='resignation-processing__tips'>
        <Text className='resignation-processing__tips-title'>温馨提示</Text>
        <Text className='resignation-processing__tips-content'>
          审批期间你可继续查看历史学员资料；审批通过后账号将转为已离职状态，如需恢复教学资格请重新提交入驻资料。
        </Text>
      </View>

      <View className='resignation-processing__actions'>
        <Button className='resignation-processing__primary-btn' onClick={navigateToTicket}>
          查看离职工单
        </Button>
        <Text className='resignation-processing__link' onClick={handleContactService}>
          联系客服 / 帮助
        </Text>
      </View>
    </View>
  )
}
