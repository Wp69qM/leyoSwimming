import { useEffect, useRef, useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Button, Image } from '@tarojs/components'
import { getApplicationDetail, type CoachApplication } from '@/api/onboarding'
import { handleBusinessError } from '@/api/request'
import { maskPhone } from '@/utils/phone'
import './index.scss'

const SUCCESS_SVG = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTIwIiBoZWlnaHQ9IjEyMCIgdmlld0JveD0iMCAwIDEyMCAxMjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGNpcmNsZSBjeD0iNjAiIGN5PSI2MCIgcj0iNTIiIGZpbGw9IiNFNkY3RkYiIHN0cm9rZT0iIzE4OTBGRiIgc3Ryb2tlLXdpZHRoPSIzIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiLz48cG9seWxpbmUgcG9pbnRzPSIzOCw2MiA1NCw3OCA4Miw0NCIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjMTg5MEZGIiBzdHJva2Utd2lkdGg9IjQiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIvPjwvc3ZnPg=='

export default function CoachOnboardingSuccessPage() {
  const [countdown, setCountdown] = useState(2)
  const [detail, setDetail] = useState<CoachApplication | null>(null)
  const isActiveRef = useRef(true)

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const data = await getApplicationDetail()
        if (cancelled) return
        setDetail(data)
      } catch (error) {
        if (cancelled) return
        // 摘要卡非关键，失败不影响主流程
        console.error(handleBusinessError(error))
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    const timer = setInterval(() => {
      if (!isActiveRef.current) {
        clearInterval(timer)
        return
      }
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer)
          isActiveRef.current = false
          Taro.redirectTo({ url: '/pages/onboarding/pending/index' })
          return 0
        }
        return prev - 1
      })
    }, 1000)
    return () => {
      isActiveRef.current = false
      clearInterval(timer)
    }
  }, [])

  function handleNavigate() {
    isActiveRef.current = false
    Taro.redirectTo({ url: '/pages/onboarding/pending/index' })
  }

  const isReapply = detail?.entryType === 'reapply'
  const portraitUrl = detail?.certificates?.find((c) => c.certType === 'PORTRAIT')?.imageUrl

  return (
    <View className='coach-onboarding-success'>
      <View className='coach-onboarding-success__illustration'>
        <Image
          className='coach-onboarding-success__img'
          src={SUCCESS_SVG}
          mode='aspectFit'
        />
      </View>
      <Text className='coach-onboarding-success__title'>
        {isReapply ? '重新入驻申请已提交' : '提交成功'}
      </Text>
      <Text className='coach-onboarding-success__subtitle'>提交成功，等待审核</Text>
      {isReapply && (
        <Text className='coach-onboarding-success__history-tip'>
          历史评分仅对老学员可见
        </Text>
      )}
      <Text className='coach-onboarding-success__countdown'>
        {countdown} 秒后自动跳转等待审核页
      </Text>

      <View className='coach-onboarding-success__card'>
        <View className='coach-onboarding-success__card-header'>
          <Text className='coach-onboarding-success__card-title'>已提交资料</Text>
          <Text className='coach-onboarding-success__status-tag'>审核中</Text>
        </View>
        <View className='coach-onboarding-success__profile'>
          <Image
            className='coach-onboarding-success__avatar'
            src={portraitUrl || SUCCESS_SVG}
            mode='aspectFill'
          />
          <View className='coach-onboarding-success__profile-info'>
            <Text className='coach-onboarding-success__name'>{detail?.name || '-'}</Text>
            <Text className='coach-onboarding-success__phone'>
              {maskPhone(detail?.phone || '')}
            </Text>
          </View>
        </View>
        <View className='coach-onboarding-success__meta'>
          <Text className='coach-onboarding-success__meta-item'>
            任教年限：
            {detail?.teachingYears != null ? `${detail.teachingYears} 年` : '-'}
          </Text>
          <Text className='coach-onboarding-success__meta-item'>
            参考单价：
            {detail?.referencePrice != null
              ? `${Number(detail.referencePrice).toFixed(2)} 元/节`
              : '-'}
          </Text>
        </View>
        <Text className='coach-onboarding-success__card-tip'>
          审核结果将通过服务通知推送
        </Text>
      </View>

      <Button
        className='coach-onboarding-success__btn'
        onClick={handleNavigate}
      >
        查看审核进度
      </Button>
    </View>
  )
}
