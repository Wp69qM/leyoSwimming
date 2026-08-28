import { View, Image } from '@tarojs/components';
import { Icon } from '@/components/common/Icon';
import type { ChatMessage } from '@/types/ai-assistant';
import { AiRecommendationCard } from './AiRecommendationCard';

interface AiMessageBubbleProps {
  message: ChatMessage;
  onSuggestedQuestionClick?: (question: string) => void;
}

function renderInlineMarkdown(text: string): JSX.Element[] {
  const parts = text.split(/(\*\*[^*]+\*\*)/g);
  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return (
        <View key={index} className='ai-message__markdown-bold'>
          {part.slice(2, -2)}
        </View>
      );
    }
    return (
      <View key={index} className='ai-message__markdown-inline'>
        {part}
      </View>
    );
  });
}

function AiMarkdownContent({ content }: { content: string }) {
  const lines = content.split('\n');
  return (
    <View className='ai-message__markdown'>
      {lines.map((line, index) => {
        const trimmed = line.trim();
        if (trimmed === '') {
          return <View key={index} className='ai-message__markdown-empty' />;
        }
        if (trimmed.startsWith('- ') || trimmed.startsWith('* ')) {
          return (
            <View key={index} className='ai-message__markdown-list'>
              <View className='ai-message__markdown-bullet'>•</View>
              <View className='ai-message__markdown-list-content'>
                {renderInlineMarkdown(trimmed.slice(2))}
              </View>
            </View>
          );
        }
        const olMatch = trimmed.match(/^(\d+)\.\s+(.*)$/);
        if (olMatch) {
          return (
            <View key={index} className='ai-message__markdown-list'>
              <View className='ai-message__markdown-bullet'>{olMatch[1]}.</View>
              <View className='ai-message__markdown-list-content'>
                {renderInlineMarkdown(olMatch[2])}
              </View>
            </View>
          );
        }
        return (
          <View key={index} className='ai-message__markdown-paragraph'>
            {renderInlineMarkdown(trimmed)}
          </View>
        );
      })}
    </View>
  );
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
          src={require('@/assets/calicat/icons/AI助理.png')}
          mode='aspectFill'
        />
      )}

      <View className='ai-message__content'>
        <View
          className={`ai-message__bubble ${isUser ? 'ai-message__bubble--user' : 'ai-message__bubble--assistant'}`}
        >
          {isUser ? (
            <View className='ai-message__text'>{message.content}</View>
          ) : (
            <AiMarkdownContent content={message.content} />
          )}
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
                  <View className='ai-message__suggested-text'>{question}</View>
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
