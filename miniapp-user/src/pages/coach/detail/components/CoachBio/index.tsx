import { useState } from 'react';
import { View, Text } from '@tarojs/components';
import type { CoachDetail } from '@/types/coach';

import './index.scss';

type CoachBioProps = {
  coach: CoachDetail;
};

export function CoachBio({ coach }: CoachBioProps) {
  const [expanded, setExpanded] = useState(false);
  const hasBio = Boolean(coach.bio);
  const hasStrokes = coach.teachingStrokes.length > 0;

  return (
    <View className='coach-detail-card'>
      <Text className='coach-detail-card__title'>教练简介</Text>
      {hasBio && (
        <View className='coach-detail-bio__content'>
          <Text
            className={`coach-detail-bio__text ${expanded ? '' : 'coach-detail-bio__text--collapsed'}`}
          >
            {coach.bio}
          </Text>
          {coach.bio && coach.bio.length > 60 && (
            <Text
              className='coach-detail-bio__toggle'
              onClick={() => setExpanded(!expanded)}
            >
              {expanded ? '收起' : '展开'}
            </Text>
          )}
        </View>
      )}
      {!hasBio && <Text className='coach-detail-card__empty'>暂无简介</Text>}
      {hasStrokes && (
        <View className='coach-detail-bio__strokes'>
          {coach.teachingStrokes.map((stroke) => (
            <View key={stroke} className='coach-detail-bio__stroke'>
              <Text className='coach-detail-bio__stroke-text'>{stroke}</Text>
            </View>
          ))}
        </View>
      )}
    </View>
  );
}
