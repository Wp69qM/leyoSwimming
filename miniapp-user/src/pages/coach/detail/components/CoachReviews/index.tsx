import { View, Text, Image } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { Icon } from '@/components/common/Icon';

import './index.scss';

type Review = {
  id: number;
  avatar: string | null;
  nickname: string;
  rating: string;
  content: string;
  createdAt: string;
};

type CoachReviewsProps = {
  rating: string;
  totalCount: number;
  reviews: Review[];
  tags?: string[];
};

export function CoachReviews({
  rating,
  totalCount,
  reviews,
  tags = [],
}: CoachReviewsProps) {
  function handleViewAllReviews() {
    void Taro.showToast({ title: '评价列表页即将上线', icon: 'none' });
  }

  const defaultTags = ['专业耐心', '进步明显', '认真负责'];
  const displayTags =
    tags.length > 0 ? tags.slice(0, 4) : defaultTags.slice(0, 3);

  return (
    <View className='coach-detail-card'>
      <View className='coach-detail-card__title'>学员评价</View>
      {totalCount === 0 ? (
        <View className='coach-detail-card__empty'>暂无评价</View>
      ) : (
        <>
          <View className='coach-detail-review__summary'>
            <Text className='coach-detail-review__summary-score'>{rating}</Text>
            <View className='coach-detail-review__summary-meta'>
              <StarRating rating={rating} />
              <Text className='coach-detail-review__summary-count'>
                {totalCount} 条评价
              </Text>
            </View>
          </View>
          <View className='coach-detail-review__tags'>
            {displayTags.map((tag) => (
              <View key={tag} className='coach-detail-review__tag'>
                <View className='coach-detail-review__tag-text'>{tag}</View>
              </View>
            ))}
          </View>
          <View className='coach-detail-review__list'>
            {reviews.slice(0, 3).map((review) => (
              <View key={review.id} className='coach-detail-review__item'>
                {review.avatar ? (
                  <Image
                    className='coach-detail-review__avatar'
                    src={review.avatar}
                    mode='aspectFill'
                  />
                ) : (
                  <View className='coach-detail-review__avatar coach-detail-review__avatar--placeholder'>
                    <View className='coach-detail-review__avatar-text'>
                      {review.nickname ? review.nickname.charAt(0) : ''}
                    </View>
                  </View>
                )}
                <View className='coach-detail-review__info'>
                  <View className='coach-detail-review__row'>
                    <Text className='coach-detail-review__nickname'>
                      {review.nickname}
                    </Text>
                    <StarRating rating={review.rating} size='small' />
                  </View>
                  <Text className='coach-detail-review__content'>
                    {review.content}
                  </Text>
                  <Text className='coach-detail-review__time'>
                    {review.createdAt}
                  </Text>
                </View>
              </View>
            ))}
          </View>
          {totalCount > 3 && (
            <View
              className='coach-detail-review__view-all'
              onClick={handleViewAllReviews}
            >
              <Text className='coach-detail-review__view-all-text'>
                查看全部评价
              </Text>
              <Icon
                name='arrow-right'
                className='coach-detail-review__view-all-icon'
              />
            </View>
          )}
        </>
      )}
    </View>
  );
}

function StarRating({
  rating,
  size = 'normal',
}: {
  rating: string;
  size?: 'normal' | 'small';
}) {
  const score = Math.min(5, Math.max(0, Number(rating) || 0));
  const starClass =
    size === 'small'
      ? 'coach-detail-review__star coach-detail-review__star--small'
      : 'coach-detail-review__star';
  return (
    <View className='coach-detail-review__stars'>
      {Array.from({ length: 5 }).map((_, index) => (
        <Icon
          key={index}
          name='star'
          className={`${starClass} ${index < score ? 'coach-detail-review__star--active' : 'coach-detail-review__star--empty'}`}
        />
      ))}
    </View>
  );
}
