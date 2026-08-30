import { useState } from 'react';
import { View, Text } from '@tarojs/components';
import Taro from '@tarojs/taro';
import { Icon } from '@/components/common/Icon';

interface AiHeaderProps {
  onNewSession: () => void;
  onOpenHistory: () => void;
  onClose: () => void;
}

export function AiHeader({
  onNewSession,
  onOpenHistory,
  onClose,
}: AiHeaderProps) {
  const statusBarHeight = Taro.getSystemInfoSync().statusBarHeight || 20;
  const [dropdownOpen, setDropdownOpen] = useState(false);

  const handleToggleDropdown = () => {
    setDropdownOpen((prev) => !prev);
  };

  const handleCloseDropdown = () => {
    setDropdownOpen(false);
  };

  const handleHistoryClick = () => {
    setDropdownOpen(false);
    onOpenHistory();
  };

  const handleNewSessionClick = () => {
    setDropdownOpen(false);
    onNewSession();
  };

  return (
    <View className='ai-header' style={{ paddingTop: `${statusBarHeight}px` }}>
      <View className='ai-header__inner'>
        <View className='ai-header__back' onClick={onClose}>
          <View className='ai-header__back-icon'>‹</View>
        </View>

        <View className='ai-header__title'>AI助理</View>

        <View className='ai-header__right'>
          <View className='ai-header__menu' onClick={handleToggleDropdown}>
            <Icon name='stack' className='ai-header__menu-icon' />
          </View>

          {dropdownOpen && (
            <>
              <View
                className='ai-header__dropdown-overlay'
                onClick={handleCloseDropdown}
              />
              <View className='ai-header__dropdown'>
                <View
                  className='ai-header__dropdown-item'
                  onClick={handleHistoryClick}
                >
                  <Icon
                    name='history'
                    className='ai-header__dropdown-icon'
                  />
                  <View className='ai-header__dropdown-text'>查看历史</View>
                </View>
                <View className='ai-header__dropdown-divider' />
                <View
                  className='ai-header__dropdown-item'
                  onClick={handleNewSessionClick}
                >
                  <Icon name='plus' className='ai-header__dropdown-icon' />
                  <View className='ai-header__dropdown-text'>新建会话</View>
                </View>
              </View>
            </>
          )}
        </View>
      </View>
    </View>
  );
}
