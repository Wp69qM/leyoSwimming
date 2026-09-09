import { View } from '@tarojs/components'
import { getPageQuery } from '@/utils/router'
import './index.scss'

export default function CoachPage() {
  const params = getPageQuery()
  return (
    <View className='coach'>
      <View className='coach__title'>教练主页</View>
      <View className='coach__id'>教练 ID: {params.id || '-'}</View>
    </View>
  )
}
