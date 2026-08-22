import { View, Text, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { Icon } from '@/components/common/Icon';
import type { Recommendation } from '@/types/ai-assistant';

interface AiRecommendationCardProps {
  item: Recommendation;
}

export function AiRecommendationCard({ item }: AiRecommendationCardProps) {
  function handleClick() {
    if (item.type === 'coach') {
      void Taro.navigateTo({
        url: `/pages/coach/detail/index?id=${item.coachId}`,
      });
      return;
    }

    if (item.type === 'package') {
      void Taro.navigateTo({
        url: `/pages/package/detail/index?templateId=${item.id}`,
      });
      return;
    }

    void Taro.navigateTo({
      url: `/pages/package/detail/index?mode=custom&coachId=${item.coachId}&hours=${item.hours}&classSize=${encodeURIComponent(item.teachingType)}&stroke=${encodeURIComponent(item.stroke)}`,
    });
  }

  if (item.type === 'coach') {
    return (
      <View className='ai-rec-card ai-rec-card--coach' onClick={handleClick}>
        <View className='ai-rec-card__header'>
          <View className='ai-rec-card__tag ai-rec-card__tag--coach'>
            <Icon name='coach' className='ai-rec-card__tag-icon' />
            <Text className='ai-rec-card__tag-text'>教练推荐</Text>
          </View>
          <View className='ai-rec-card__rating'>
            <Icon name='star-fill' className='ai-rec-card__star' />
            <Text className='ai-rec-card__rating-text'>{item.rating}</Text>
          </View>
        </View>

        <View className='ai-rec-card__body'>
          <Image
            className='ai-rec-card__avatar'
            src={item.avatarUrl ?? ''}
            mode='aspectFill'
          />
          <View className='ai-rec-card__info'>
            <Text className='ai-rec-card__name'>{item.name}</Text>
            <View className='ai-rec-card__meta-row'>
              <Text className='ai-rec-card__meta'>
                {item.teachingYears}年教龄
              </Text>
              <Text className='ai-rec-card__meta ai-rec-card__meta--divider'>
                |
              </Text>
              <Text className='ai-rec-card__meta'>
                参考 ¥{item.referencePrice}/课时
              </Text>
            </View>
            <View className='ai-rec-card__skills'>
              {item.teachingStrokes.map((stroke) => (
                <View key={stroke} className='ai-rec-card__skill-tag'>
                  <Text className='ai-rec-card__skill-tag-text'>{stroke}</Text>
                </View>
              ))}
            </View>
          </View>
        </View>

        <View className='ai-rec-card__reason ai-rec-card__reason--coach'>
          <Text className='ai-rec-card__reason-text'>{item.reason}</Text>
        </View>

        <View className='ai-rec-card__action'>
          <Text className='ai-rec-card__action-text'>查看教练详情</Text>
        </View>
      </View>
    );
  }

  if (item.type === 'package') {
    return (
      <View className='ai-rec-card ai-rec-card--package' onClick={handleClick}>
        <View className='ai-rec-card__header'>
          <View className='ai-rec-card__tag ai-rec-card__tag--package'>
            <Icon name='package' className='ai-rec-card__tag-icon' />
            <Text className='ai-rec-card__tag-text'>套餐推荐</Text>
          </View>
          <Text className='ai-rec-card__price'>¥{formatPrice(item.price)}</Text>
        </View>

        <Text className='ai-rec-card__package-name'>{item.name}</Text>

        <View className='ai-rec-card__meta-row ai-rec-card__meta-row--package'>
          <Icon name='time' className='ai-rec-card__meta-icon' />
          <Text className='ai-rec-card__meta'>有效期 {item.validDays} 天</Text>
          <Text className='ai-rec-card__meta ai-rec-card__meta--divider'>
            |
          </Text>
          <Icon name='user' className='ai-rec-card__meta-icon' />
          <Text className='ai-rec-card__meta'>{item.teachingType}</Text>
          <Text className='ai-rec-card__meta ai-rec-card__meta--divider'>
            |
          </Text>
          <Text className='ai-rec-card__meta'>{item.totalHours} 课时</Text>
        </View>

        <Text className='ai-rec-card__meta ai-rec-card__meta--stroke'>
          适合泳姿：{item.stroke}
        </Text>

        <View className='ai-rec-card__reason ai-rec-card__reason--package'>
          <Text className='ai-rec-card__reason-text'>{item.reason}</Text>
        </View>

        <View className='ai-rec-card__action'>
          <Text className='ai-rec-card__action-text'>查看套餐详情</Text>
        </View>
      </View>
    );
  }

  return (
    <View className='ai-rec-card ai-rec-card--custom' onClick={handleClick}>
      <View className='ai-rec-card__header'>
        <View className='ai-rec-card__tag ai-rec-card__tag--package'>
          <Icon name='package' className='ai-rec-card__tag-icon' />
          <Text className='ai-rec-card__tag-text'>自定义套餐</Text>
        </View>
        <Text className='ai-rec-card__price'>
          ¥{formatPrice(item.estimatedTotalPrice)}
        </Text>
      </View>

      <Text className='ai-rec-card__package-name'>
        {item.coachName} · {item.hours} 课时定制方案
      </Text>

      <View className='ai-rec-card__meta-row ai-rec-card__meta-row--package'>
        <Icon name='user' className='ai-rec-card__meta-icon' />
        <Text className='ai-rec-card__meta'>{item.teachingType}</Text>
        <Text className='ai-rec-card__meta ai-rec-card__meta--divider'>|</Text>
        <Text className='ai-rec-card__meta'>
          参考 ¥{item.referencePrice}/课时
        </Text>
      </View>

      <Text className='ai-rec-card__meta ai-rec-card__meta--stroke'>
        适合泳姿：{item.stroke}
      </Text>

      <View className='ai-rec-card__reason ai-rec-card__reason--package'>
        <Text className='ai-rec-card__reason-text'>{item.reason}</Text>
      </View>

      <View className='ai-rec-card__action'>
        <Text className='ai-rec-card__action-text'>查看套餐详情</Text>
      </View>
    </View>
  );
}

function formatPrice(price: string): string {
  const num = Number(price);
  if (Number.isNaN(num)) return price;
  return num.toLocaleString('zh-CN');
}
