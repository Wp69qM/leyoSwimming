import { View, Text, Image } from '@tarojs/components';
import { Icon } from '@/components/common/Icon';
import type { ChatMessage } from '@/types/ai-assistant';
import { AiRecommendationCard } from './AiRecommendationCard';

interface AiMessageBubbleProps {
  message: ChatMessage;
  onSuggestedQuestionClick?: (question: string) => void;
}

export function AiMessageBubble({
  message,
  onSuggestedQuestionClick,
}: AiMessageBubbleProps) {
  const isUser = message.role === 'user';

  return (
    <View
      className={`ai-message ${isUser ? 'ai-message--user' : 'ai-message--assistant'}`}
    >
      {!isUser && (
        <Image
          className='ai-message__avatar ai-message__avatar--assistant'
          src={require('@/assets/calicat/images/leyo-avatar-small.jpg')}
          mode='aspectFill'
        />
      )}

      <View className='ai-message__content'>
        <View
          className={`ai-message__bubble ${isUser ? 'ai-message__bubble--user' : 'ai-message__bubble--assistant'}`}
        >
          <Text className='ai-message__text'>{message.content}</Text>
        </View>

        {!isUser &&
          message.recommendations &&
          message.recommendations.length > 0 && (
            <View className='ai-message__recommendations'>
              {message.recommendations.map((item, index) => (
                <AiRecommendationCard
                  key={`${item.type}-${index}`}
                  item={item}
                />
              ))}
            </View>
          )}

        {!isUser &&
          message.suggestedQuestions &&
          message.suggestedQuestions.length > 0 && (
            <View className='ai-message__suggested'>
              {message.suggestedQuestions.map((question, index) => (
                <View
                  key={index}
                  className='ai-message__suggested-item'
                  onClick={() => onSuggestedQuestionClick?.(question)}
                >
                  <Icon
                    name='suggested'
                    className='ai-message__suggested-icon'
                  />
                  <Text className='ai-message__suggested-text'>{question}</Text>
                </View>
              ))}
            </View>
          )}
      </View>

      {isUser && (
        <View className='ai-message__avatar ai-message__avatar--user'>
          <Icon name='user' className='ai-message__avatar-icon' />
        </View>
      )}
    </View>
  );
}
