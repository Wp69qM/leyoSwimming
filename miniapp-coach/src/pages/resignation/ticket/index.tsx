import { useEffect, useMemo, useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Input, Button } from '@tarojs/components'
import {
  getResignationDetail,
  submitPackageAction,
  submitResignationTicket
} from '@/api/resignation'
import { handleBusinessError } from '@/api/request'
import { getPageQuery } from '@/utils/router'
import type { CoachPackage, PackageAction, PackageMode, ResignationTicket, ResignationTicketStatus } from '@/types/resignation'
import './index.scss'

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20

const ACTION_VALUES: PackageAction[] = ['transfer', 'refund', 'continue']

const ACTION_LABELS: Record<PackageAction, string> = {
  transfer: '转新教练',
  refund: '全额退款',
  continue: '继续上完'
}

const ACTION_PICKER_PLACEHOLDER = '请选择处理方式'

const PACKAGE_MODE_LABELS: Record<PackageMode, string> = {
  standard: '正价课',
  experience: '体验课',
  custom: '自定义套餐'
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
  const [actionSheetOpen, setActionSheetOpen] = useState(false)
  const [actionSheetPkg, setActionSheetPkg] = useState<CoachPackage | null>(null)

  const ticketId = useMemo(() => {
    const params = getPageQuery()
    return params.ticketId ? Number(params.ticketId) : undefined
  }, [])

  const isReadOnly = !ticket || ticket.status !== 'processing'

  useEffect(() => {
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
  }, [ticketId, refreshKey])

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

  function handleBack() {
    Taro.navigateBack().catch(() => {
      Taro.switchTab({ url: '/pages/mine/index' })
    })
  }

  function handleOpenActionSheet(pkg: CoachPackage) {
    if (isReadOnly) return
    setActionSheetPkg(pkg)
    setActionSheetOpen(true)
  }

  function handleSelectAction(action: PackageAction) {
    if (!actionSheetPkg) return
    setActions((prev) => ({
      ...prev,
      [actionSheetPkg.id]: {
        action,
        targetCoachId: undefined,
        targetCoachName: undefined
      }
    }))
    setActionSheetOpen(false)
    setActionSheetPkg(null)
  }

  function handleCloseActionSheet() {
    setActionSheetOpen(false)
    setActionSheetPkg(null)
  }

  function handleTargetCoachChange(pkgId: number, value: string) {
    setActions((prev) => ({
      ...prev,
      [pkgId]: { ...(prev[pkgId] ?? {}), targetCoachName: value }
    }))
  }

  function handleTargetCoachConfirm(pkgId: number) {
    const state = actions[pkgId]
    if (!state?.targetCoachName?.trim()) return

    const targetCoachId = Number(state.targetCoachName.trim())
    if (!Number.isInteger(targetCoachId) || targetCoachId <= 0) {
      Taro.showToast({ title: '请输入正确的教练编号', icon: 'none' })
      return
    }

    setActions((prev) => ({
      ...prev,
      [pkgId]: { ...(prev[pkgId] ?? {}), targetCoachId }
    }))
  }

  async function handleSubmit() {
    if (submitting) return
    if (!ticket) return
    if (ticket.packages.length > 0 && !allProcessed) {
      Taro.showToast({ title: '请先处理完所有学员套餐', icon: 'none' })
      return
    }

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
      // 批量提交各套餐处理方式
      if (ticket.packages.length > 0) {
        await Promise.all(
          ticket.packages.map((pkg) => {
            const state = actions[pkg.id]
            if (!state?.action) return Promise.resolve()
            return submitPackageAction({
              ticketId: ticket.ticketId,
              packageId: pkg.id,
              action: state.action,
              targetCoachId: state.targetCoachId
            })
          })
        )
      }

      await submitResignationTicket({ ticketId: ticket.ticketId })
      Taro.showToast({ title: '工单已提交，等待审批', icon: 'success' })
      navigateToProcessing(ticket.ticketId)
    } catch (error) {
      setErrorTip(handleBusinessError(error))
      setSubmitting(false)
    }
  }

  function renderProgress() {
    if (!ticket || ticket.status === 'none') {
      return <View className='resignation-ticket__progress--empty' />
    }
    const statusStepMap: Record<ResignationTicketStatus, number> = {
      processing: 1,
      pending_audit: 2,
      approved: 3,
      rejected: 3,
      none: 0
    }
    const currentStep = statusStepMap[ticket.status] ?? 1
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
            <View
              className={`resignation-ticket__progress-label ${
                index === currentStep ? 'resignation-ticket__progress-label--active' : ''
              }`}
            >
              {step}
            </View>
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

    function renderActionEditor() {
      return (
        <>
          <View
            className={`resignation-ticket__selector ${
              state.action ? 'resignation-ticket__selector--selected' : ''
            }`}
            onClick={() => handleOpenActionSheet(pkg)}
          >
            <View className='resignation-ticket__selector-inner'>
              <View
                className={`resignation-ticket__selector-text ${
                  state.action ? '' : 'resignation-ticket__selector-text--placeholder'
                }`}
              >
                {state.action ? ACTION_LABELS[state.action] : ACTION_PICKER_PLACEHOLDER}
              </View>
              <Text className='resignation-ticket__selector-arrow'>▼</Text>
            </View>
          </View>

          {isTransfer ? (
            <View className='resignation-ticket__transfer'>
              <View className='resignation-ticket__transfer-label'>转给教练</View>
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
        </>
      )
    }

    function renderActionReadOnly() {
      const action = state.action
      if (!action) {
        return (
          <View className='resignation-ticket__result'>
            <View className='resignation-ticket__result-label'>处理方式</View>
            <View className='resignation-ticket__result-value resignation-ticket__result-value--placeholder'>
              未处理
            </View>
          </View>
        )
      }
      return (
        <View className='resignation-ticket__result'>
          <View className='resignation-ticket__result-row'>
            <View className='resignation-ticket__result-label'>处理方式</View>
            <View
              className={`resignation-ticket__result-tag resignation-ticket__result-tag--${action}`}
            >
              {ACTION_LABELS[action]}
            </View>
          </View>
          {action === 'transfer' ? (
            <View className='resignation-ticket__result-row'>
              <View className='resignation-ticket__result-label'>新教练</View>
              <View className='resignation-ticket__result-value'>
                {pkg.targetCoachName ?? state.targetCoachName ?? '-'}
                {pkg.targetCoachPhone ? ` (${pkg.targetCoachPhone})` : ''}
              </View>
            </View>
          ) : null}
        </View>
      )
    }

    return (
      <View key={pkg.id} className='resignation-ticket__card'>
        <View className='resignation-ticket__card-header'>
          <View className='resignation-ticket__student-name'>{pkg.studentName}</View>
          <View
            className={`resignation-ticket__status-tag ${
              pkg.status === 'active'
                ? 'resignation-ticket__status-tag--active'
                : 'resignation-ticket__status-tag--frozen'
            }`}
          >
            <View
              className={`resignation-ticket__status-text ${
                pkg.status === 'active'
                  ? 'resignation-ticket__status-text--active'
                  : 'resignation-ticket__status-text--frozen'
              }`}
            >
              {pkg.status === 'active' ? '使用中' : '已冻结'}
            </View>
          </View>
        </View>
        <View className='resignation-ticket__package-info'>
          <View className='resignation-ticket__mode-tag'>
            {PACKAGE_MODE_LABELS[pkg.packageMode]}
          </View>
          {pkg.packageName} · 剩余 {pkg.lessonCount} 课时
        </View>

        {isReadOnly ? renderActionReadOnly() : renderActionEditor()}
      </View>
    )
  }

  function renderEmpty() {
    return (
      <View className='resignation-ticket__empty'>
        <View className='resignation-ticket__empty-title'>暂无待处理套餐</View>
        <View className='resignation-ticket__empty-desc'>您名下没有未完结的学员套餐</View>
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
          <View className='resignation-ticket__loading-text'>加载中...</View>
        </View>
      </View>
    )
  }

  if (errorTip) {
    return (
      <View className='resignation-ticket'>
        {renderProgress()}
        <View className='resignation-ticket__error'>
          <View className='resignation-ticket__error-text'>{errorTip}</View>
          <Button className='resignation-ticket__error-btn' onClick={handleRetry}>
            重新加载
          </Button>
        </View>
      </View>
    )
  }

  const editable = ticket?.status === 'processing'
  const showEmpty = !ticket || ticket.packages.length === 0
  const submitDisabled = ticket ? ticket.packages.length > 0 && !allProcessed : false

  const STATUS_TEXT: Record<string, string> = {
    processing: '处理中：请选择各套餐的处理方式并提交审批',
    pending_audit: '已提交：等待管理员审批',
    approved: '已通过：离职申请已获批',
    rejected: '已驳回：请重新提交离职申请',
    none: '暂无进行中的离职工单'
  }

  function renderStatusBanner() {
    if (!ticket) return null
    const text = STATUS_TEXT[ticket.status] ?? STATUS_TEXT.none
    return (
      <View className={`resignation-ticket__status-banner resignation-ticket__status-banner--${ticket.status}`}>
        <View className='resignation-ticket__status-banner-text'>{text}</View>
        {!editable && ticket.status !== 'none' ? (
          <View className='resignation-ticket__status-banner-link' onClick={() => navigateToProcessing(ticket.ticketId)}>
            查看进度 ›
          </View>
        ) : null}
      </View>
    )
  }

  return (
    <View className='resignation-ticket'>
      <View
        className='resignation-ticket__status-bar'
        style={{ height: `${STATUS_BAR_HEIGHT}px` }}
      />
      <View className='resignation-ticket__navbar'>
        <View className='resignation-ticket__navbar-back' onClick={handleBack}>
          <Text className='resignation-ticket__navbar-back-icon'>‹</Text>
        </View>
        <View className='resignation-ticket__navbar-title'>离职工单处理</View>
      </View>

      <View className='resignation-ticket__body'>
        {renderStatusBanner()}
        {renderProgress()}

        {showEmpty ? (
          renderEmpty()
        ) : (
          <>
            <View className='resignation-ticket__section-title'>套餐清单</View>
            <View className='resignation-ticket__list'>
              {ticket?.packages.map(renderPackageCard)}
            </View>

            {errorTip ? (
              <View className='resignation-ticket__error-tip'>
                <View className='resignation-ticket__error-tip-text'>{errorTip}</View>
              </View>
            ) : null}
          </>
        )}
      </View>

      {editable ? (
        <View className='resignation-ticket__footer'>
          <View className='resignation-ticket__footer-inner'>
            <View className='resignation-ticket__footer-info'>
              <View className='resignation-ticket__footer-count'>
                已处理 {processedCount}/{ticket?.packages.length ?? 0} 份
              </View>
            </View>
            <Button
              className={`resignation-ticket__submit ${
                submitDisabled || submitting ? 'resignation-ticket__submit--disabled' : ''
              }`}
              onClick={handleSubmit}
            >
              {submitting ? '提交中...' : '提交审批'}
            </Button>
          </View>
        </View>
      ) : null}

      {actionSheetOpen ? (
        <View className='resignation-ticket__action-sheet-mask' onClick={handleCloseActionSheet}>
          <View className='resignation-ticket__action-sheet' onClick={(e) => e.stopPropagation()}>
            <View className='resignation-ticket__action-sheet-title'>选择处理方式</View>
            {ACTION_VALUES.map((action) => (
              <View
                key={action}
                className='resignation-ticket__action-sheet-item'
                onClick={() => handleSelectAction(action)}
              >
                {ACTION_LABELS[action]}
              </View>
            ))}
            <View className='resignation-ticket__action-sheet-cancel' onClick={handleCloseActionSheet}>
              取消
            </View>
          </View>
        </View>
      ) : null}
    </View>
  )
}
