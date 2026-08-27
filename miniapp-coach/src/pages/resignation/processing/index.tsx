import { useEffect, useMemo, useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Button } from '@tarojs/components'
import { getResignationDetail } from '@/api/resignation'
import { handleBusinessError } from '@/api/request'
import type { ResignationTicket } from '@/types/resignation'
import './index.scss'

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20

const PROGRESS_TEXT: Record<string, string> = {
  processing: '等待教练处理学员套餐',
  pending_audit: '已提交管理员审批'
}

export default function ResignationProcessingPage() {
  const [ticket, setTicket] = useState<ResignationTicket | null>(null)
  const [loading, setLoading] = useState(true)
  const [errorTip, setErrorTip] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)

  const ticketId = useMemo(() => {
    const params = Taro.getCurrentInstance().router?.params
    return params?.ticketId ? Number(params.ticketId) : undefined
  }, [])

  useEffect(() => {
    async function loadDetail() {
      setLoading(true)
      setErrorTip('')
      try {
        const detail = await getResignationDetail({ ticketId })
        if (!detail || detail.status === 'none') {
          setErrorTip('当前无进行中的离职申请')
          setTicket(null)
        } else {
          setTicket(detail)
        }
      } catch (error) {
        setErrorTip(handleBusinessError(error))
      } finally {
        setLoading(false)
      }
    }

    void loadDetail()
  }, [ticketId, refreshKey])

  function handleRetry() {
    setRefreshKey((prev) => prev + 1)
  }

  function handleBack() {
    Taro.navigateBack().catch(() => {
      Taro.switchTab({ url: '/pages/mine/index' })
    })
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
        <View
          className='resignation-processing__status-bar'
          style={{ height: `${STATUS_BAR_HEIGHT}px` }}
        />
        <View className='resignation-processing__navbar'>
          <View className='resignation-processing__navbar-title'>离职处理</View>
        </View>
        <View className='resignation-processing__body'>
          <View className='resignation-processing__loading'>
            <View className='resignation-processing__loading-text'>加载中...</View>
          </View>
        </View>
      </View>
    )
  }

  if (errorTip) {
    return (
      <View className='resignation-processing'>
        <View
          className='resignation-processing__status-bar'
          style={{ height: `${STATUS_BAR_HEIGHT}px` }}
        />
        <View className='resignation-processing__navbar'>
          <View className='resignation-processing__navbar-back' onClick={handleBack}>
            <Text className='resignation-processing__navbar-back-icon'>‹</Text>
          </View>
          <View className='resignation-processing__navbar-title'>离职处理</View>
        </View>
        <View className='resignation-processing__body'>
          <View className='resignation-processing__error'>
            <View className='resignation-processing__error-text'>{errorTip}</View>
            <Button className='resignation-processing__error-btn' onClick={handleRetry}>
              重新加载
            </Button>
          </View>
        </View>
      </View>
    )
  }

  return (
    <View className='resignation-processing'>
      <View
        className='resignation-processing__status-bar'
        style={{ height: `${STATUS_BAR_HEIGHT}px` }}
      />
      <View className='resignation-processing__navbar'>
        <View className='resignation-processing__navbar-back' onClick={handleBack}>
          <Text className='resignation-processing__navbar-back-icon'>‹</Text>
        </View>
        <View className='resignation-processing__navbar-title'>离职处理</View>
      </View>
      <View className='resignation-processing__body'>
        <View className='resignation-processing__status'>
          <View className='resignation-processing__icon'>
            <Text className='resignation-processing__icon-symbol'>⏳</Text>
          </View>
          <View className='resignation-processing__title'>离职申请已提交，正在处理中</View>
          <View className='resignation-processing__subtitle'>请耐心等待管理员审批</View>
        </View>

        <View className='resignation-processing__card'>
          <View className='resignation-processing__card-header'>
            <View className='resignation-processing__card-title'>工单进度</View>
            <View className='resignation-processing__status-tag'>
              <View className='resignation-processing__status-tag-text'>处理中</View>
            </View>
          </View>
          <View className='resignation-processing__ticket-no'>
            工单号：{ticket?.ticketNo ?? '--'}
          </View>
          <View className='resignation-processing__current-progress'>{progressText}</View>
          <View className='resignation-processing__meta'>
            更新时间：{ticket?.updatedAt ?? '--'}
          </View>
          <View className='resignation-processing__meta'>
            {ticket?.estimatedProcessTime ?? '预计 1-3 个工作日内完成审批'}
          </View>
        </View>

        <View className='resignation-processing__tips'>
          <View className='resignation-processing__tips-title'>温馨提示</View>
          <View className='resignation-processing__tips-content'>
            审批期间你可继续查看历史学员资料；审批通过后账号将转为已离职状态，如需恢复教学资格请重新提交入驻资料。
          </View>
        </View>

        <View className='resignation-processing__actions'>
          <Button className='resignation-processing__primary-btn' onClick={navigateToTicket}>
            查看离职工单
          </Button>
          <View className='resignation-processing__link' onClick={handleContactService}>
            联系客服 / 帮助
          </View>
        </View>
      </View>
    </View>
  )
}
