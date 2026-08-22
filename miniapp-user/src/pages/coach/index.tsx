import { useCallback, useEffect, useRef, useState } from 'react';
import { View, Text, Image, Input } from '@tarojs/components';
import Taro, { useReachBottom } from '@tarojs/taro';
import { fetchCoachList } from '@/api/coach';
import { handleBusinessError } from '@/api/request';
import { Icon } from '@/components/common/Icon';
import type { CoachListItem, CoachSortType } from '@/types/coach';

import './index.scss';

const STATUS_BAR_HEIGHT = Taro.getSystemInfoSync().statusBarHeight || 20;
const PAGE_SIZE = 10;

const SORT_OPTIONS: { key: CoachSortType; label: string }[] = [
  { key: 'rating', label: '综合评分' },
  { key: 'price', label: '价格' },
  { key: 'time', label: '可约时间' },
];

const REAL_TIME_STATUS_CLASS: Record<string, string> = {
  空闲中: 'coach-list-card__status--free',
  上课中: 'coach-list-card__status--teaching',
  休息中: 'coach-list-card__status--rest',
  已下班: 'coach-list-card__status--off',
  请假中: 'coach-list-card__status--leave',
};

const DETAIL_PATH = '/pages/coach/detail/index';

export default function CoachListPage() {
  const [items, setItems] = useState<CoachListItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [keyword, setKeyword] = useState('');
  const [sortBy, setSortBy] = useState<CoachSortType>('rating');
  const [priceOrder, setPriceOrder] = useState<'asc' | 'desc'>('asc');
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);

  const latestRequestId = useRef(0);
  const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const loadCoaches = useCallback(
    async ({
      isRefresh = false,
      nextPage = 1,
      nextKeyword = keyword,
      nextSortBy = sortBy,
      nextPriceOrder = priceOrder,
    }: {
      isRefresh?: boolean;
      nextPage?: number;
      nextKeyword?: string;
      nextSortBy?: CoachSortType;
      nextPriceOrder?: 'asc' | 'desc';
    } = {}) => {
      const requestId = ++latestRequestId.current;

      if (isRefresh || nextPage === 1) {
        setInitialLoading(true);
      } else {
        setLoading(true);
      }
      setError(null);

      try {
        const sortOrder =
          nextSortBy === 'price'
            ? nextPriceOrder
            : nextSortBy === 'time'
              ? 'asc'
              : 'desc';
        const data = await fetchCoachList({
          page: nextPage,
          pageSize: PAGE_SIZE,
          keyword: nextKeyword,
          sortBy: nextSortBy,
          sortOrder,
        });

        if (requestId !== latestRequestId.current) return;

        const list = data.items ?? [];
        setItems((prev) => (nextPage === 1 ? list : [...prev, ...list]));
        setHasMore(
          list.length === PAGE_SIZE &&
            list.length + (nextPage === 1 ? 0 : items.length) < data.total
        );
        setPage(nextPage);
      } catch (err) {
        if (requestId !== latestRequestId.current) return;
        setError(handleBusinessError(err));
      } finally {
        if (requestId === latestRequestId.current) {
          setInitialLoading(false);
          setLoading(false);
        }
      }
    },
    [keyword, sortBy, priceOrder, items.length]
  );

  useEffect(() => {
    void loadCoaches({ isRefresh: true });
    return () => {
      if (searchTimer.current) {
        clearTimeout(searchTimer.current);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useReachBottom(() => {
    if (!loading && hasMore) {
      void loadCoaches({ nextPage: page + 1 });
    }
  });

  function handleKeywordChange(value: string) {
    setKeyword(value);
    if (searchTimer.current) {
      clearTimeout(searchTimer.current);
    }
    searchTimer.current = setTimeout(() => {
      void loadCoaches({ isRefresh: true, nextPage: 1, nextKeyword: value });
    }, 300);
  }

  function clearKeyword() {
    setKeyword('');
    if (searchTimer.current) {
      clearTimeout(searchTimer.current);
    }
    void loadCoaches({ isRefresh: true, nextPage: 1, nextKeyword: '' });
  }

  function handleSortChange(key: CoachSortType) {
    if (key === sortBy) {
      if (key === 'price') {
        const nextOrder = priceOrder === 'asc' ? 'desc' : 'asc';
        setPriceOrder(nextOrder);
        void loadCoaches({
          isRefresh: true,
          nextSortBy: key,
          nextPriceOrder: nextOrder,
        });
      }
      return;
    }
    setSortBy(key);
    void loadCoaches({ isRefresh: true, nextSortBy: key });
  }

  function navigateToDetail(coachId: number) {
    void Taro.navigateTo({ url: `${DETAIL_PATH}?id=${coachId}` });
  }

  if (initialLoading && items.length === 0) {
    return <CoachListSkeleton />;
  }

  if (error && items.length === 0) {
    return (
      <View className='coach-list'>
        <CoachListHeader
          keyword={keyword}
          onKeywordChange={handleKeywordChange}
          onClear={clearKeyword}
        />
        <View className='coach-list__error'>
          <Icon name='error-circle' className='coach-list__error-icon' />
          <Text className='coach-list__error-text'>{error}</Text>
          <View
            className='coach-list__error-btn'
            onClick={() => loadCoaches({ isRefresh: true })}
          >
            重新加载
          </View>
        </View>
      </View>
    );
  }

  return (
    <View className='coach-list'>
      <CoachListHeader
        keyword={keyword}
        onKeywordChange={handleKeywordChange}
        onClear={clearKeyword}
      />

      <View className='coach-list__sort'>
        {SORT_OPTIONS.map((option) => (
          <View
            key={option.key}
            className={`coach-list__sort-item ${sortBy === option.key ? 'coach-list__sort-item--active' : ''}`}
            onClick={() => handleSortChange(option.key)}
          >
            <Text className='coach-list__sort-text'>{option.label}</Text>
            {option.key === 'price' && sortBy === 'price' && (
              <Icon
                name={priceOrder === 'asc' ? 'arrow-up' : 'arrow-down'}
                className='coach-list__sort-arrow'
              />
            )}
            {sortBy === option.key && option.key !== 'price' && (
              <View className='coach-list__sort-line' />
            )}
          </View>
        ))}
      </View>

      <View className='coach-list__content'>
        {items.length === 0 ? (
          <View className='coach-list__empty'>
            <View className='coach-list__empty-illustration'>
              <Icon name='empty' className='coach-list__empty-icon' />
            </View>
            <Text className='coach-list__empty-title'>暂无教练</Text>
            <Text className='coach-list__empty-text'>
              换个关键词或筛选条件试试
            </Text>
          </View>
        ) : (
          <>
            {items.map((coach) => (
              <CoachCard
                key={coach.id}
                coach={coach}
                onClick={() => navigateToDetail(coach.id)}
              />
            ))}
            {error && items.length > 0 && (
              <View className='coach-list__load-more-error'>
                <Text className='coach-list__load-more-error-text'>
                  {error}
                </Text>
                <View
                  className='coach-list__load-more-error-btn'
                  onClick={() => loadCoaches({ nextPage: page })}
                >
                  点击重试
                </View>
              </View>
            )}
            {!error && loading && (
              <View className='coach-list__loading-more'>加载中...</View>
            )}
            {!error && !loading && !hasMore && items.length > 0 && (
              <View className='coach-list__no-more'>— 没有更多了 —</View>
            )}
          </>
        )}
      </View>
    </View>
  );
}

function CoachListHeader({
  keyword,
  onKeywordChange,
  onClear,
}: {
  keyword: string;
  onKeywordChange: (value: string) => void;
  onClear: () => void;
}) {
  return (
    <View
      className='coach-list__header'
      style={{ paddingTop: `${STATUS_BAR_HEIGHT}px` }}
    >
      <View className='coach-list__navbar'>
        <Text className='coach-list__title'>教练</Text>
      </View>
      <View className='coach-list__search'>
        <Icon name='search' className='coach-list__search-icon' />
        <Input
          className='coach-list__search-input'
          type='text'
          placeholder='搜索教练姓名'
          value={keyword}
          onInput={(e) => onKeywordChange(e.detail.value)}
          confirmType='search'
        />
        {keyword && (
          <View className='coach-list__search-clear' onClick={onClear}>
            <Icon name='close' className='coach-list__search-clear-icon' />
          </View>
        )}
      </View>
    </View>
  );
}

const AVATAR_COLOR_CLASSES = [
  'coach-list-card__avatar--blue',
  'coach-list-card__avatar--orange',
  'coach-list-card__avatar--green',
  'coach-list-card__avatar--cyan',
];

function getAvatarColorClass(id: number): string {
  const index = Math.abs(id) % AVATAR_COLOR_CLASSES.length;
  return AVATAR_COLOR_CLASSES[index];
}

function CoachCard({
  coach,
  onClick,
}: {
  coach: CoachListItem;
  onClick: () => void;
}) {
  const statusClass =
    REAL_TIME_STATUS_CLASS[coach.realTimeStatus] ??
    'coach-list-card__status--default';
  const surname = coach.name ? coach.name.charAt(0) : '';
  const avatarColorClass = getAvatarColorClass(coach.id);
  return (
    <View className='coach-list-card' onClick={onClick}>
      {coach.avatar ? (
        <Image
          className='coach-list-card__avatar'
          src={coach.avatar}
          mode='aspectFill'
        />
      ) : (
        <View
          className={`coach-list-card__avatar-placeholder ${avatarColorClass}`}
        >
          <Text>{surname}</Text>
        </View>
      )}
      <View className='coach-list-card__info'>
        <View className='coach-list-card__row'>
          <Text className='coach-list-card__name'>{coach.name}</Text>
          <View className={`coach-list-card__status ${statusClass}`}>
            <Text className='coach-list-card__status-text'>
              {coach.realTimeStatus}
            </Text>
          </View>
        </View>
        <View className='coach-list-card__row coach-list-card__row--meta'>
          <Icon name='star' className='coach-list-card__star' />
          <Text className='coach-list-card__rating'>{coach.rating}</Text>
          <Text className='coach-list-card__meta-divider'>·</Text>
          <Text className='coach-list-card__years'>
            教龄 {coach.yearsOfTeaching} 年
          </Text>
        </View>
        <View className='coach-list-card__strokes'>
          {coach.teachingStrokes.slice(0, 2).map((stroke) => (
            <View key={stroke} className='coach-list-card__stroke-tag'>
              <Text className='coach-list-card__stroke-text'>{stroke}</Text>
            </View>
          ))}
        </View>
      </View>
    </View>
  );
}

function CoachListSkeleton() {
  return (
    <View className='coach-list coach-list--skeleton'>
      <View
        className='coach-list__header'
        style={{ paddingTop: `${STATUS_BAR_HEIGHT}px` }}
      >
        <View className='coach-list__navbar'>
          <View className='coach-list__title' />
        </View>
        <View className='coach-list__search'>
          <View className='coach-list__search-input' />
        </View>
      </View>
      <View className='coach-list__sort'>
        {[1, 2, 3].map((i) => (
          <View key={i} className='coach-list__sort-item'>
            <View className='coach-list__sort-text' />
          </View>
        ))}
      </View>
      <View className='coach-list__content'>
        {[1, 2, 3, 4, 5].map((i) => (
          <View key={i} className='coach-list-card'>
            <View className='coach-list-card__avatar' />
            <View className='coach-list-card__info'>
              <View className='coach-list-card__skeleton-line coach-list-card__skeleton-line--title' />
              <View className='coach-list-card__skeleton-line coach-list-card__skeleton-line--meta' />
              <View className='coach-list-card__skeleton-line coach-list-card__skeleton-line--tag' />
            </View>
          </View>
        ))}
      </View>
    </View>
  );
}
