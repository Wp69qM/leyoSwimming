import { View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { Icon } from '@/components/common/Icon';
import type { SessionListItem } from '@/types/ai-assistant';

interface AiHistoryDrawerProps {
  visible: boolean;
  sessions: SessionListItem[];
  currentSessionId: string | null;
  onClose: () => void;
  onSelectSession: (sessionId: string) => void;
}

export function AiHistoryDrawer({
  visible,
  sessions,
  currentSessionId,
  onClose,
  onSelectSession,
}: AiHistoryDrawerProps) {
  const statusBarHeight = Taro.getSystemInfoSync().statusBarHeight || 20;

  if (!visible) return null;

  return (
    <View className='ai-history-drawer'>
      <View className='ai-history-drawer__mask' onClick={onClose} />
      <View
        className='ai-history-drawer__panel'
        style={{ paddingTop: `${statusBarHeight}px` }}
      >
        <View className='ai-history-drawer__header'>
          <View className='ai-history-drawer__title-wrap'>
            <Icon name='history' className='ai-history-drawer__title-icon' />
            <View className='ai-history-drawer__title'>历史会话</View>
          </View>
          <View className='ai-history-drawer__close' onClick={onClose}>
            <Icon name='close' className='ai-history-drawer__close-icon' />
          </View>
        </View>

        <View className='ai-history-drawer__list'>
          {sessions?.length === 0 && (
            <View className='ai-history-drawer__empty'>
              <View className='ai-history-drawer__empty-text'>暂无历史会话</View>
            </View>
          )}
          {sessions?.map((session) => {
            const isCurrent = session.sessionId === currentSessionId;
            return (
              <View
                key={session.sessionId}
                className={`ai-history-drawer__item ${isCurrent ? 'ai-history-drawer__item--current' : ''}`}
                onClick={() => onSelectSession(session.sessionId)}
              >
                <View className='ai-history-drawer__item-main'>
                  <View className='ai-history-drawer__item-title'>
                    {session.title}
                  </View>
                  <View className='ai-history-drawer__item-time'>
                    {session.lastMessageTime}
                  </View>
                </View>
                <View className='ai-history-drawer__item-right'>
                  {isCurrent && (
                    <View className='ai-history-drawer__current-tag'>
                      <View className='ai-history-drawer__current-tag-text'>
                        当前
                      </View>
                    </View>
                  )}
                  <Icon
                    name='chevron-right'
                    className='ai-history-drawer__arrow'
                  />
                </View>
              </View>
            )
          })}
        </View>
      </View>
    </View>
  );
}
