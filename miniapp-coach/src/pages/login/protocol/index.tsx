import Taro from '@tarojs/taro'
import { getPageQuery } from '@/utils/router'
import { ProtocolDrawer, type ProtocolTab } from './ProtocolDrawer'

export {
  ProtocolDrawer,
  type ProtocolTab,
  type ProtocolDrawerProps,
} from './ProtocolDrawer'

export default function ProtocolPage() {
  const { type } = getPageQuery()
  const initialTab: ProtocolTab = type === 'privacy' ? 'privacy' : 'terms'

  function handleClose() {
    Taro.navigateBack()
  }

  return (
    <ProtocolDrawer
      visible
      initialTab={initialTab}
      onClose={handleClose}
      onAgree={handleClose}
    />
  )
}
