import { useEffect, useMemo, useRef, useState } from 'react'
import Taro, { useDidShow } from '@tarojs/taro'
import { View, Text, Input, Image } from '@tarojs/components'
import { fetchCoachStudentList } from '@/api/student'
import type { CoachStudentItem, StudentTab } from '@/types/student'
import './index.scss'

const TABS: { key: StudentTab; label: string }[] = [
  { key: 'active', label: '活跃学员' },
  { key: 'history', label: '历史学员' }
]

export default function StudentPage() {
  const [activeTab, setActiveTab] = useState<StudentTab>('active')
  const [keyword, setKeyword] = useState('')
  const [students, setStudents] = useState<CoachStudentItem[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(false)
  const latestRequestId = useRef(0)

  const loadStudents = async (tab: StudentTab, search: string, isRetry = false) => {
    const requestId = ++latestRequestId.current
    setLoading(true)
    if (isRetry) {
      setError(false)
    }

    try {
      const res = await fetchCoachStudentList({ tab, keyword: search || undefined })
      if (requestId !== latestRequestId.current) return
      setStudents(res.students || [])
      setError(false)
    } catch {
      if (requestId !== latestRequestId.current) return
      setError(true)
    } finally {
      if (requestId === latestRequestId.current) {
        setLoading(false)
      }
    }
  }

  useDidShow(() => {
    loadStudents(activeTab, keyword)
  })

  useEffect(() => {
    loadStudents(activeTab, keyword)
  }, [activeTab])

  const filteredStudents = useMemo(() => {
    const trimmed = keyword.trim()
    if (!trimmed) return students
    return students.filter(item => item.name.includes(trimmed))
  }, [students, keyword])

  const handleSearchInput = (value: string) => {
    setKeyword(value)
  }

  const handleSearchConfirm = () => {
    loadStudents(activeTab, keyword)
  }

  const handleTabChange = (tab: StudentTab) => {
    if (tab === activeTab) return
    setActiveTab(tab)
    setKeyword('')
  }

  const handleStudentClick = (studentUserId: number) => {
    Taro.navigateTo({ url: `/pages/student/detail/index?studentUserId=${studentUserId}` })
  }

  const renderSkeleton = () => (
    <View className='student__list'>
      {Array.from({ length: 4 }).map((_, index) => (
        <View key={index} className='student-card student-card--skeleton'>
          <View className='student-card__avatar skeleton' />
          <View className='student-card__info'>
            <View className='student-card__name-row skeleton' />
            <View className='student-card__tags skeleton' />
          </View>
        </View>
      ))}
    </View>
  )

  const renderEmpty = () => (
    <View className='student__empty'>
      <Text className='student__empty-title'>
        {activeTab === 'active' ? '暂无活跃学员' : '暂无历史学员'}
      </Text>
      <Text className='student__empty-desc'>
        {activeTab === 'active'
          ? '关联学员购买套餐后将出现在这里'
          : '暂无历史关联学员'}
      </Text>
    </View>
  )

  const renderError = () => (
    <View className='student__error'>
      <Text className='student__error-title'>网络异常，请重试</Text>
      <View
        className='student__error-button'
        onClick={() => loadStudents(activeTab, keyword, true)}
      >
        <Text className='student__error-button-text'>重新加载</Text>
      </View>
    </View>
  )

  const renderAvatar = (item: CoachStudentItem) => {
    if (item.avatarUrl) {
      return <Image className='student-card__avatar-img' src={item.avatarUrl} mode='aspectFill' />
    }
    const char = item.name ? item.name.charAt(0) : '?'
    return <Text className='student-card__avatar-text'>{char}</Text>
  }

  const renderContent = () => {
    if (loading && students.length === 0) return renderSkeleton()
    if (error) return renderError()
    if (filteredStudents.length === 0) return renderEmpty()

    return (
      <View className='student__list'>
        {filteredStudents.map(item => (
          <View
            key={item.studentUserId}
            className='student-card'
            onClick={() => handleStudentClick(item.studentUserId)}
          >
            <View className='student-card__avatar'>{renderAvatar(item)}</View>
            <View className='student-card__info'>
              <View className='student-card__name-row'>
                <Text className='student-card__name'>{item.name}</Text>
                <Text className='student-card__meta'>
                  {formatGenderAge(item.gender, item.age)}
                </Text>
                {item.isMinor && (
                  <View className='student-card__minor-tag'>
                    <Text className='student-card__minor-tag-text'>未成年</Text>
                  </View>
                )}
              </View>
              {activeTab === 'active' && item.activePackageTags.length > 0 && (
                <View className='student-card__tags'>
                  {item.activePackageTags.map((tag, index) => (
                    <View
                      key={index}
                      className={`student-card__tag student-card__tag--${tag.type}`}
                    >
                      <Text className='student-card__tag-text'>{tag.label}</Text>
                    </View>
                  ))}
                </View>
              )}
            </View>
            <Text className='student-card__arrow'>›</Text>
          </View>
        ))}
      </View>
    )
  }

  return (
    <View className='student'>
      <View className='student__search'>
        <View className='student__search-box'>
          <Text className='student__search-icon'>🔍</Text>
          <Input
            className='student__search-input'
            type='text'
            placeholder='搜索学员姓名'
            value={keyword}
            onInput={e => handleSearchInput(e.detail.value)}
            onConfirm={handleSearchConfirm}
            confirmType='search'
          />
        </View>
      </View>

      <View className='student__tabs'>
        <View className='student__tab-container'>
          {TABS.map(tab => (
            <View
              key={tab.key}
              className={`student__tab ${activeTab === tab.key ? 'student__tab--active' : ''}`}
              onClick={() => handleTabChange(tab.key)}
            >
              <Text className='student__tab-text'>{tab.label}</Text>
            </View>
          ))}
        </View>
      </View>

      <View className='student__content'>{renderContent()}</View>
    </View>
  )
}

function formatGenderAge(gender: string | null, age: number | null): string {
  const parts: string[] = []
  if (gender) parts.push(gender)
  if (age != null) parts.push(`${age}岁`)
  return parts.join(' · ') || ''
}
