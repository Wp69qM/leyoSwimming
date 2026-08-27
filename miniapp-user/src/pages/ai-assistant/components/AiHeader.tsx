import { View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { Icon } from '@/components/common/Icon';

interface AiHeaderProps {
  isLoggedIn: boolean;
  onNewSession: () => void;
  onOpenHistory: () => void;
  onClose: () => void;
}

export function AiHeader({
  isLoggedIn,
  onNewSession,
  onOpenHistory,
  onClose,
}: AiHeaderProps) {
  const statusBarHeight = Taro.getSystemInfoSync().statusBarHeight || 20;

  return (
    <View className='ai-header' style={{ paddingTop: `${statusBarHeight}px` }}>
      <View className='ai-header__inner'>
        <View className='ai-header__button' onClick={onNewSession}>
          <Icon name='plus' className='ai-header__button-icon' />
          <View className='ai-header__button-text'>新会话</View>
        </View>

        <View className='ai-header__title-wrap'>
          <View className='ai-header__title'>leyo</View>
        </View>

        <View className='ai-header__actions'>
          {isLoggedIn && (
            <View className='ai-header__button' onClick={onOpenHistory}>
              <Icon name='history' className='ai-header__button-icon' />
              <View className='ai-header__button-text'>历史</View>
            </View>
          )}
          <View
            className='ai-header__button ai-header__button--icon-only'
            onClick={onClose}
          >
            <Icon name='close' className='ai-header__button-icon' />
          </View>
        </View>
      </View>
    </View>
  );
}
