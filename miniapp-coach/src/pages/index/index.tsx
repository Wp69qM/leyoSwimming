import { useEffect } from 'react'
import Taro from '@tarojs/taro'
import { View } from '@tarojs/components'
import { useAuthStore } from '@/stores/authStore'
import './index.scss'

export default function Index() {
  useEffect(() => {
    const { restoreFromStorage } = useAuthStore.getState()
    restoreFromStorage()

    const { isLoggedIn } = useAuthStore.getState()
    if (!isLoggedIn) {
      Taro.redirectTo({ url: '/pages/login/wechat/index' })
    }
  }, [])

  return (
    <View className="index">
      <View className="index__title">leyoSwimming 教练端</View>
      <View className="index__subtitle">欢迎使用教练端小程序</View>
    </View>
  )
}
