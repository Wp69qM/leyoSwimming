import { View, Text, Image, ScrollView } from '@tarojs/components';
import { Icon } from '@/components/common/Icon';
import type { CoachDetail } from '@/types/coach';

import './index.scss';

const GENDER_TEXT: Record<number, string> = {
  1: '男',
  2: '女',
};

type CoachHeaderProps = {
  coach: CoachDetail;
};

export function CoachHeader({ coach }: CoachHeaderProps) {
  const genderText = coach.gender ? GENDER_TEXT[coach.gender] : '';
  const ageText = coach.age ? `${coach.age}岁` : '';
  const metaParts = [ageText, `教龄${coach.yearsOfTeaching}年`].filter(Boolean);
  const surname = coach.name ? coach.name.charAt(0) : '';

  return (
    <View className='coach-detail-header'>
      {coach.avatar ? (
        <Image
          className='coach-detail-header__avatar'
          src={coach.avatar}
          mode='aspectFill'
        />
      ) : (
        <View className='coach-detail-header__avatar coach-detail-header__avatar--placeholder'>
          <Text className='coach-detail-header__avatar-text'>{surname}</Text>
        </View>
      )}
      <View className='coach-detail-header__row coach-detail-header__row--main'>
        <Text className='coach-detail-header__name'>{coach.name}</Text>
        {genderText && (
          <View className='coach-detail-header__gender'>
            <Text className='coach-detail-header__gender-text'>
              {genderText}
            </Text>
          </View>
        )}
        <View className='coach-detail-header__status'>
          <Text className='coach-detail-header__status-text'>
            {coach.realTimeStatus}
          </Text>
        </View>
      </View>
      {metaParts.length > 0 && (
        <Text className='coach-detail-header__meta'>
          {metaParts.join(' · ')}
        </Text>
      )}
      <View className='coach-detail-header__rating-row'>
        <Icon name='star' className='coach-detail-header__star' />
        <Text className='coach-detail-header__rating'>{coach.rating}分</Text>
        <Text className='coach-detail-header__rating-divider'>|</Text>
        <Text className='coach-detail-header__students'>
          带过 {coach.totalStudents} 位学员
        </Text>
        {coach.totalHours > 0 && (
          <>
            <Text className='coach-detail-header__rating-divider'>|</Text>
            <Text className='coach-detail-header__students'>
              累计 {coach.totalHours} 课时
            </Text>
          </>
        )}
      </View>
      {coach.certificates.length > 0 && (
        <ScrollView className='coach-detail-header__certs' scrollX>
          {coach.certificates.slice(0, 2).map((cert) => (
            <View key={cert.certType} className='coach-detail-header__cert'>
              <Text className='coach-detail-header__cert-text'>
                {cert.certType}
              </Text>
            </View>
          ))}
          {coach.certificates.length > 2 && (
            <View className='coach-detail-header__cert coach-detail-header__cert--more'>
              <Text className='coach-detail-header__cert-text'>
                +{coach.certificates.length - 2}
              </Text>
            </View>
          )}
        </ScrollView>
      )}
    </View>
  );
}
