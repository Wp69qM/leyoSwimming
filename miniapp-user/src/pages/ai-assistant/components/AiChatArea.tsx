import { useRef, useEffect } from 'react';
import { View, Image, ScrollView } from '@tarojs/components';
import { Icon } from '@/components/common/Icon';
import type { ChatMessage } from '@/types/ai-assistant';
import { AiMessageBubble } from './AiMessageBubble';

interface AiChatAreaProps {
  welcomeMessage: string;
  messages: ChatMessage[];
  isLoading: boolean;
  error: string | null;
  onRetry: () => void;
  onSuggestedQuestionClick: (question: string) => void;
}

export function AiChatArea({
  welcomeMessage,
  messages,
  isLoading,
  error,
  onRetry,
  onSuggestedQuestionClick,
}: AiChatAreaProps) {
  const scrollRef = useRef<typeof ScrollView>(null);

  useEffect(() => {
    // Scroll to bottom when messages change or loading state changes
    // In Taro, we rely on scrollIntoView via scroll-with-animation
  }, [messages, isLoading]);

  const hasMessages = messages.length > 0;

  return (
    <ScrollView
      ref={scrollRef}
      className='ai-chat-area'
      scrollY
      scrollWithAnimation
      enhanced
      showScrollbar={false}
      scrollIntoView={
        hasMessages
          ? `msg-${messages[messages.length - 1]?.id ?? 'bottom'}`
          : ''
      }
    >
      {!hasMessages && (
        <View className='ai-chat-area__welcome'>
          <View className='ai-chat-area__avatar-wrap'>
            <Image
              className='ai-chat-area__avatar'
              src={require('@/assets/calicat/images/leyo-avatar-large.jpg')}
              mode='aspectFill'
            />
            <View className='ai-chat-area__online-dot' />
          </View>
          <View className='ai-chat-area__welcome-title'>
            你好，我是 leyo 👋
          </View>
          <View className='ai-chat-area__welcome-subtitle'>
            你的专属游泳教练助理，有什么可以帮你的？
          </View>
        </View>
      )}

      {hasMessages && (
        <View className='ai-chat-area__messages'>
          <AiMessageBubble
            message={{ role: 'assistant', content: welcomeMessage }}
            onSuggestedQuestionClick={onSuggestedQuestionClick}
          />
          {messages.map((message) => (
            <View
              key={message.id ?? `msg-${message.content}`}
              id={`msg-${message.id ?? 'bottom'}`}
            >
              <AiMessageBubble
                message={message}
                onSuggestedQuestionClick={onSuggestedQuestionClick}
              />
            </View>
          ))}
        </View>
      )}

      {isLoading && (
        <View className='ai-chat-area__loading'>
          <Image
            className='ai-chat-area__loading-avatar'
            src={require('@/assets/calicat/images/leyo-avatar-small.jpg')}
            mode='aspectFill'
          />
          <View className='ai-chat-area__loading-bubble'>
            <View className='ai-chat-area__loading-dot' />
            <View className='ai-chat-area__loading-dot' />
            <View className='ai-chat-area__loading-dot' />
          </View>
        </View>
      )}

      {error && (
        <View className='ai-chat-area__error'>
          <Icon name='error-circle' className='ai-chat-area__error-icon' />
          <View className='ai-chat-area__error-text'>{error}</View>
          <View className='ai-chat-area__error-button' onClick={onRetry}>
            <View className='ai-chat-area__error-button-text'>重试</View>
          </View>
        </View>
      )}

      <View className='ai-chat-area__bottom-spacer' />
    </ScrollView>
  );
}
