import { useEffect, useRef, useState } from 'react'
import Taro from '@tarojs/taro'
import { View, Text, Button, Image } from '@tarojs/components'
import './index.scss'

const ILLUSTRATION_URL = 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=A+flat+minimal+illustration+of+a+swimming+coach+with+a+checkmark+and+clipboard%2C+blue+and+teal+gradient%2C+celebration%2C+clean+white+background&image_size=square'

export default function CoachOnboardingSuccessPage() {
  const [countdown, setCountdown] = useState(2)
  const isActiveRef = useRef(true)

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

  return (
    <View className='coach-onboarding-success'>
      <View className='coach-onboarding-success__illustration'>
        <Image
          className='coach-onboarding-success__img'
          src={ILLUSTRATION_URL}
          mode='aspectFill'
        />
      </View>
      <Text className='coach-onboarding-success__title'>提交成功</Text>
      <Text className='coach-onboarding-success__subtitle'>提交成功，等待审核</Text>
      <Text className='coach-onboarding-success__countdown'>
        {countdown} 秒后自动跳转等待审核页
      </Text>

      <View className='coach-onboarding-success__card'>
        <View className='coach-onboarding-success__card-header'>
          <Text className='coach-onboarding-success__card-title'>已提交资料</Text>
          <Text className='coach-onboarding-success__status-tag'>审核中</Text>
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
