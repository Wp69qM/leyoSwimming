import Taro from '@tarojs/taro'
import { View, Text } from '@tarojs/components'
import './index.scss'

export default function CoachPage() {
  const params = Taro.getCurrentInstance().router?.params
  return (
    <View className='coach'>
      <Text className='coach__title'>教练主页</Text>
      <Text className='coach__id'>教练 ID: {params?.id || '-'}</Text>
    </View>
  )
}
