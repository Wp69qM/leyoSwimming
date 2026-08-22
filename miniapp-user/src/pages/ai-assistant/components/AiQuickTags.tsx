import { View, Text } from '@tarojs/components';
import { Icon } from '@/components/common/Icon';
import type { QuickTag } from '@/types/ai-assistant';

interface AiQuickTagsProps {
  tags: QuickTag[];
  onTagClick: (message: string) => void;
  onRefresh?: () => void;
}

const ICON_BACKGROUND_MAP: Record<string, string> = {
  swim: 'ai-quick-tags__icon--blue',
  crown: 'ai-quick-tags__icon--orange',
  female: 'ai-quick-tags__icon--purple',
  seedling: 'ai-quick-tags__icon--green',
  emotion: 'ai-quick-tags__icon--cyan',
  gift: 'ai-quick-tags__icon--red',
};

export function AiQuickTags({ tags, onTagClick, onRefresh }: AiQuickTagsProps) {
  return (
    <View className='ai-quick-tags'>
      <View className='ai-quick-tags__header'>
        <Text className='ai-quick-tags__title'>大家都在问</Text>
        {onRefresh && (
          <View className='ai-quick-tags__refresh' onClick={onRefresh}>
            <Icon name='refresh' className='ai-quick-tags__refresh-icon' />
            <Text className='ai-quick-tags__refresh-text'>换一批</Text>
          </View>
        )}
      </View>

      <View className='ai-quick-tags__grid'>
        {tags.map((tag) => (
          <View
            key={tag.id}
            className='ai-quick-tags__card'
            onClick={() => onTagClick(tag.message)}
          >
            <View
              className={`ai-quick-tags__icon ${ICON_BACKGROUND_MAP[tag.icon] ?? 'ai-quick-tags__icon--blue'}`}
            >
              <Icon
                name={tag.icon as never}
                className='ai-quick-tags__icon-inner'
              />
            </View>
            <View className='ai-quick-tags__info'>
              <Text className='ai-quick-tags__card-title'>{tag.title}</Text>
              <Text className='ai-quick-tags__card-subtitle'>
                {tag.subtitle}
              </Text>
            </View>
          </View>
        ))}
      </View>
    </View>
  );
}
