import { View, Text } from '@tarojs/components';
import { Icon } from '@/components/common/Icon';

import './index.scss';

type TimeItem = {
  dayOfWeek: string;
  timeRanges: string[];
};

type CoachAvailableTimesProps = {
  times: TimeItem[];
  onViewAll: () => void;
};

export function CoachAvailableTimes({
  times,
  onViewAll,
}: CoachAvailableTimesProps) {
  return (
    <View className='coach-detail-card'>
      <View className='coach-detail-card__header'>
        <Text className='coach-detail-card__title'>可约时间</Text>
        {times.length > 0 && (
          <View className='coach-detail-card__more' onClick={onViewAll}>
            <Text className='coach-detail-card__more-text'>查看全部</Text>
            <Icon name='arrow-right' className='coach-detail-card__more-icon' />
          </View>
        )}
      </View>
      {times.length === 0 ? (
        <Text className='coach-detail-card__empty'>本周暂无可约时段</Text>
      ) : (
        <View className='coach-detail-time__list'>
          {times.map((item) => (
            <View key={item.dayOfWeek} className='coach-detail-time__row'>
              <Text className='coach-detail-time__day'>{item.dayOfWeek}</Text>
              <View className='coach-detail-time__slots'>
                {item.timeRanges.length === 0 ? (
                  <Text className='coach-detail-time__empty'>暂无可约</Text>
                ) : (
                  item.timeRanges.slice(0, 3).map((range) => (
                    <View key={range} className='coach-detail-time__slot'>
                      <Text className='coach-detail-time__slot-text'>
                        {range}
                      </Text>
                    </View>
                  ))
                )}
              </View>
            </View>
          ))}
        </View>
      )}
    </View>
  );
}
