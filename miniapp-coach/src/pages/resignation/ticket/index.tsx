import { useEffect, useMemo, useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import {
  getResignationDetail,
  submitPackageAction,
  submitResignationTicket
} from '@/api/resignation'
import { handleBusinessError } from '@/api/request'
import { useAuthStore } from '@/stores/authStore'
import { COACH_STATUS } from '@/constants'
import type { CoachPackage, PackageAction, ResignationTicket } from '@/types/resignation'
import './index.scss'

const ACTION_OPTIONS: { value: PackageAction; label: string }[] = [
  { value: 'transfer', label: '转新教练' },
  { value: 'refund', label: '全额退款' },
  { value: 'continue', label: '继续上完' }
]

const ACTION_LABELS: Record<PackageAction, string> = {
  transfer: '转新教练',
  refund: '全额退款',
  continue: '继续上完'
}

const STEPS = ['提交申请', '处理套餐', '提交工单', '审批结果']

interface PackageActionState {
  action?: PackageAction
  targetCoachId?: number
  targetCoachName?: string
}

export default function ResignationTicketPage() {
  const [ticket, setTicket] = useState<ResignationTicket | null>(null)
  const [actions, setActions] = useState<Record<number, PackageActionState>>({})
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [errorTip, setErrorTip] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const coachInfo = useAuthStore((state) => state.coachInfo)
  const restoreFromStorage = useAuthStore((state) => state.restoreFromStorage)

  const ticketId = useMemo(() => {
    const params = Taro.getCurrentInstance().router?.params
    return params?.ticketId ? Number(params.ticketId) : undefined
  }, [])

  const isReadOnly = !ticket || ticket.status !== 'processing'

  useEffect(() => {
    restoreFromStorage()
  }, [restoreFromStorage])

  useEffect(() => {
    Taro.setNavigationBarTitle({ title: '离职工单处理' })
  }, [])

  useEffect(() => {
    if (!coachInfo) return

    async function loadDetail() {
      setLoading(true)
      setErrorTip('')
      try {
        const detail = await getResignationDetail({ ticketId })
        setTicket(detail)
        const initialActions: Record<number, PackageActionState> = {}
        detail.packages.forEach((pkg) => {
          initialActions[pkg.id] = {
            action: pkg.action,
            targetCoachId: pkg.targetCoachId,
            targetCoachName: pkg.targetCoachId ? String(pkg.targetCoachId) : undefined
          }
        })
        setActions(initialActions)
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

  const processedCount = useMemo(() => {
    if (!ticket) return 0
    return ticket.packages.filter((pkg) => {
      const state = actions[pkg.id]
      if (!state?.action) return false
      if (state.action === 'transfer') {
        return typeof state.targetCoachId === 'number' && state.targetCoachId > 0
      }
      return true
    }).length
  }, [ticket, actions])

  const allProcessed =
    (ticket?.packages.length ?? 0) === 0 || processedCount === (ticket?.packages.length ?? 0)

  function navigateToProcessing(targetTicketId: number) {
    Taro.redirectTo({
      url: `/pages/resignation/processing/index?ticketId=${targetTicketId}`
    })
  }

  function navigateToApply() {
    Taro.navigateBack({}).catch(() => {
      Taro.redirectTo({ url: '/pages/resignation/apply/index' })
    })
  }

  async function handleSelectAction(pkg: CoachPackage) {
    if (isReadOnly) return

    const { tapIndex } = await Taro.showActionSheet({
      itemList: ACTION_OPTIONS.map((item) => item.label)
    })

    const selected = ACTION_OPTIONS[tapIndex]
    if (!selected) return

    const actionState: PackageActionState = { action: selected.value }
    if (selected.value !== 'transfer') {
      try {
        await persistAction(pkg.id, selected.value)
      } catch {
        return
      }
    }
    setActions((prev) => ({ ...prev, [pkg.id]: actionState }))
  }

  async function persistAction(packageId: number, action: PackageAction, targetCoachId?: number) {
    if (!ticket) return
    try {
      await submitPackageAction({ ticketId: ticket.ticketId, packageId, action, targetCoachId })
    } catch (error) {
      Taro.showToast({ title: handleBusinessError(error), icon: 'none' })
      throw error
    }
  }

  function handleTargetCoachChange(pkgId: number, value: string) {
    setActions((prev) => ({
      ...prev,
      [pkgId]: { ...prev[pkgId], targetCoachName: value }
    }))
  }

  async function handleTargetCoachConfirm(pkgId: number) {
    const state = actions[pkgId]
    if (!state?.targetCoachName?.trim()) return

    const targetCoachId = Number(state.targetCoachName.trim())
    if (!Number.isInteger(targetCoachId) || targetCoachId <= 0) {
      Taro.showToast({ title: '请输入正确的教练编号', icon: 'none' })
      return
    }

    try {
      await persistAction(pkgId, 'transfer', targetCoachId)
      setActions((prev) => ({
        ...prev,
        [pkgId]: { ...prev[pkgId], targetCoachId }
      }))
    } catch {
      // error already toasted
    }
  }

  async function handleSubmit() {
    if (!ticket || !allProcessed) return

    const confirm = await Taro.showModal({
      title: '确认提交审批？',
      content: '提交后工单将流转至管理员审批，不可自行撤销。',
      confirmText: '确认提交',
      confirmColor: '#FF4D4F',
      cancelText: '再想想'
    })

    if (!confirm.confirm) return

    setSubmitting(true)
    setErrorTip('')

    try {
      await submitResignationTicket({ ticketId: ticket.ticketId })
      Taro.showToast({ title: '工单已提交，等待审批', icon: 'success' })
      navigateToProcessing(ticket.ticketId)
    } catch (error) {
      setErrorTip(handleBusinessError(error))
      setSubmitting(false)
    }
  }

  function renderProgress() {
    const statusStepMap: Record<ResignationTicketStatus, number> = {
      processing: 1,
      pending_audit: 2,
      approved: 3,
      rejected: 3
    }
    const currentStep = ticket ? statusStepMap[ticket.status] ?? 1 : 1
    return (
      <View className='resignation-ticket__progress'>
        {STEPS.map((step, index) => (
          <View key={step} className='resignation-ticket__progress-step'>
            <View
              className={`resignation-ticket__progress-dot ${
                index <= currentStep ? 'resignation-ticket__progress-dot--active' : ''
              }`}
            >
              {index < currentStep ? (
                <Text className='resignation-ticket__progress-check'>✓</Text>
              ) : (
                <Text className='resignation-ticket__progress-number'>{index + 1}</Text>
              )}
            </View>
            <Text
              className={`resignation-ticket__progress-label ${
                index === currentStep ? 'resignation-ticket__progress-label--active' : ''
              }`}
            >
              {step}
            </Text>
            {index < STEPS.length - 1 ? (
              <View
                className={`resignation-ticket__progress-line ${
                  index < currentStep ? 'resignation-ticket__progress-line--active' : ''
                }`}
              />
            ) : null}
          </View>
        ))}
      </View>
    )
  }

  function renderPackageCard(pkg: CoachPackage) {
    const state = actions[pkg.id] ?? {}
    const isTransfer = state.action === 'transfer'

    return (
      <View key={pkg.id} className='resignation-ticket__card'>
        <View className='resignation-ticket__card-header'>
          <Text className='resignation-ticket__student-name'>{pkg.studentName}</Text>
          <View
            className={`resignation-ticket__status-tag ${
              pkg.status === 'active'
                ? 'resignation-ticket__status-tag--active'
                : 'resignation-ticket__status-tag--frozen'
            }`}
          >
            <Text
              className={`resignation-ticket__status-text ${
                pkg.status === 'active'
                  ? 'resignation-ticket__status-text--active'
                  : 'resignation-ticket__status-text--frozen'
              }`}
            >
              {pkg.status === 'active' ? '使用中' : '已冻结'}
            </Text>
          </View>
        </View>
        <Text className='resignation-ticket__package-info'>
          {pkg.packageName} · 剩余 {pkg.lessonCount} 课时
        </Text>

        <View
          className={`resignation-ticket__selector ${
            state.action ? 'resignation-ticket__selector--selected' : ''
          } ${isReadOnly ? 'resignation-ticket__selector--readonly' : ''}`}
          onClick={() => handleSelectAction(pkg)}
        >
          <Text
            className={`resignation-ticket__selector-text ${
              state.action ? '' : 'resignation-ticket__selector-text--placeholder'
            }`}
          >
            {state.action ? ACTION_LABELS[state.action] : '请选择处理方式'}
          </Text>
          {!isReadOnly ? <Text className='resignation-ticket__selector-arrow'>▼</Text> : null}
        </View>

        {isTransfer && !isReadOnly ? (
          <View className='resignation-ticket__transfer'>
            <Text className='resignation-ticket__transfer-label'>转给教练</Text>
            <Input
              className='resignation-ticket__transfer-input'
              type='number'
              placeholder='请输入教练编号'
              value={state.targetCoachName ?? ''}
              onInput={(e) => handleTargetCoachChange(pkg.id, e.detail.value)}
              onBlur={() => handleTargetCoachConfirm(pkg.id)}
            />
          </View>
        ) : null}
      </View>
    )
  }

  function renderEmpty() {
    return (
      <View className='resignation-ticket__empty'>
        <Text className='resignation-ticket__empty-title'>暂无待处理套餐</Text>
        <Text className='resignation-ticket__empty-desc'>您名下没有未完结的学员套餐</Text>
        <Button className='resignation-ticket__empty-btn' onClick={navigateToApply}>
          返回
        </Button>
      </View>
    )
  }

  if (loading) {
    return (
      <View className='resignation-ticket'>
        {renderProgress()}
        <View className='resignation-ticket__loading'>
          <Text className='resignation-ticket__loading-text'>加载中...</Text>
        </View>
      </View>
    )
  }

  if (errorTip) {
    return (
      <View className='resignation-ticket'>
        {renderProgress()}
        <View className='resignation-ticket__error'>
          <Text className='resignation-ticket__error-text'>{errorTip}</Text>
          <Button className='resignation-ticket__error-btn' onClick={handleRetry}>
            重新加载
          </Button>
        </View>
      </View>
    )
  }

  const editable = coachInfo?.status === COACH_STATUS.RESIGNING && ticket?.status === 'processing'
  const showEmpty = !ticket || ticket.packages.length === 0

  return (
    <View className='resignation-ticket'>
      {renderProgress()}

      {showEmpty ? (
        renderEmpty()
      ) : (
        <>
          <Text className='resignation-ticket__section-title'>套餐清单</Text>
          <View className='resignation-ticket__list'>
            {ticket?.packages.map(renderPackageCard)}
          </View>

          {errorTip ? (
            <View className='resignation-ticket__error-tip'>
              <Text className='resignation-ticket__error-tip-text'>{errorTip}</Text>
            </View>
          ) : null}
        </>
      )}

      {editable ? (
        <View className='resignation-ticket__footer'>
          <View className='resignation-ticket__footer-info'>
            <Text className='resignation-ticket__footer-count'>
              已处理 {processedCount}/{ticket?.packages.length ?? 0} 份
            </Text>
          </View>
          <Button
            className={`resignation-ticket__submit ${
              !allProcessed || submitting ? 'resignation-ticket__submit--disabled' : ''
            }`}
            onClick={handleSubmit}
            disabled={!allProcessed || submitting}
            loading={submitting}
          >
            {submitting ? '提交中...' : '提交审批'}
          </Button>
        </View>
      ) : null}
    </View>
  )
}
