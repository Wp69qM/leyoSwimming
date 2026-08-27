import { View, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { Icon } from '@/components/common/Icon';
import { getTeachingTypeLabel } from '@/constants/teachingType';
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
            <View className='ai-rec-card__tag-text'>教练推荐</View>
          </View>
          <View className='ai-rec-card__rating'>
            <Icon name='star-fill' className='ai-rec-card__star' />
            <View className='ai-rec-card__rating-text'>{item.rating}</View>
          </View>
        </View>

        <View className='ai-rec-card__body'>
          <Image
            className='ai-rec-card__avatar'
            src={item.avatarUrl ?? ''}
            mode='aspectFill'
          />
          <View className='ai-rec-card__info'>
            <View className='ai-rec-card__name'>{item.name}</View>
            <View className='ai-rec-card__meta-row'>
              <View className='ai-rec-card__meta'>
                {item.teachingYears}年教龄
              </View>
              <View className='ai-rec-card__meta ai-rec-card__meta--divider'>
                |
              </View>
              <View className='ai-rec-card__meta'>
                参考 ¥{item.referencePrice}/课时
              </View>
            </View>
            <View className='ai-rec-card__skills'>
              {item.teachingStrokes.map((stroke) => (
                <View key={stroke} className='ai-rec-card__skill-tag'>
                  <View className='ai-rec-card__skill-tag-text'>{stroke}</View>
                </View>
              ))}
            </View>
          </View>
        </View>

        <View className='ai-rec-card__reason ai-rec-card__reason--coach'>
          <View className='ai-rec-card__reason-text'>{item.reason}</View>
        </View>

        <View className='ai-rec-card__action'>
          <View className='ai-rec-card__action-text'>查看教练详情</View>
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
            <View className='ai-rec-card__tag-text'>套餐推荐</View>
          </View>
          <View className='ai-rec-card__price'>¥{formatPrice(item.price)}</View>
        </View>

        <View className='ai-rec-card__package-name'>{item.name}</View>

        <View className='ai-rec-card__meta-row ai-rec-card__meta-row--package'>
          <Icon name='time' className='ai-rec-card__meta-icon' />
          <View className='ai-rec-card__meta'>有效期 {item.validDays} 天</View>
          <View className='ai-rec-card__meta ai-rec-card__meta--divider'>
            |
          </View>
          <Icon name='user' className='ai-rec-card__meta-icon' />
          <View className='ai-rec-card__meta'>
            {getTeachingTypeLabel(item.teachingType)}
          </View>
          <View className='ai-rec-card__meta ai-rec-card__meta--divider'>
            |
          </View>
          <View className='ai-rec-card__meta'>{item.totalHours} 课时</View>
        </View>

        <View className='ai-rec-card__meta ai-rec-card__meta--stroke'>
          适合泳姿：{item.stroke}
        </View>

        <View className='ai-rec-card__reason ai-rec-card__reason--package'>
          <View className='ai-rec-card__reason-text'>{item.reason}</View>
        </View>

        <View className='ai-rec-card__action'>
          <View className='ai-rec-card__action-text'>查看套餐详情</View>
        </View>
      </View>
    );
  }

  return (
    <View className='ai-rec-card ai-rec-card--custom' onClick={handleClick}>
      <View className='ai-rec-card__header'>
        <View className='ai-rec-card__tag ai-rec-card__tag--package'>
          <Icon name='package' className='ai-rec-card__tag-icon' />
          <View className='ai-rec-card__tag-text'>自定义套餐</View>
        </View>
        <View className='ai-rec-card__price'>
          ¥{formatPrice(item.estimatedTotalPrice)}
        </View>
      </View>

      <View className='ai-rec-card__package-name'>
        {item.coachName} · {item.hours} 课时定制方案
      </View>

      <View className='ai-rec-card__meta-row ai-rec-card__meta-row--package'>
        <Icon name='user' className='ai-rec-card__meta-icon' />
        <View className='ai-rec-card__meta'>
          {getTeachingTypeLabel(item.teachingType)}
        </View>
        <View className='ai-rec-card__meta ai-rec-card__meta--divider'>|</View>
        <View className='ai-rec-card__meta'>
          参考 ¥{item.referencePrice}/课时
        </View>
      </View>

      <View className='ai-rec-card__meta ai-rec-card__meta--stroke'>
        适合泳姿：{item.stroke}
      </View>

      <View className='ai-rec-card__reason ai-rec-card__reason--package'>
        <View className='ai-rec-card__reason-text'>{item.reason}</View>
      </View>

      <View className='ai-rec-card__action'>
        <View className='ai-rec-card__action-text'>查看套餐详情</View>
      </View>
    </View>
  );
}

function formatPrice(price: string): string {
  const num = Number(price);
  if (Number.isNaN(num)) return price;
  return num.toLocaleString('zh-CN');
}
