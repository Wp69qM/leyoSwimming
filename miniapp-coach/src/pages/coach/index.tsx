import Taro from '@tarojs/taro'
import { View } from '@tarojs/components'
import './index.scss'

export default function CoachPage() {
  const params = Taro.getCurrentInstance().router?.params
  return (
    <View className='coach'>
      <View className='coach__title'>教练主页</View>
      <View className='coach__id'>教练 ID: {params?.id || '-'}</View>
    </View>
  )
}
