import { useState } from 'react';
import { View, Text, Textarea } from '@tarojs/components';
import { Icon } from '@/components/common/Icon';

interface AiInputBarProps {
  onSend: (message: string) => void;
  disabled?: boolean;
}

export function AiInputBar({ onSend, disabled = false }: AiInputBarProps) {
  const [inputValue, setInputValue] = useState('');

  const trimmedValue = inputValue.trim();
  const canSend = trimmedValue.length > 0 && !disabled;

  function handleSend() {
    if (!canSend) return;
    onSend(trimmedValue);
    setInputValue('');
  }

  function handleInput(
    event:
      React.ChangeEvent<HTMLTextAreaElement> | { detail: { value: string } }
  ) {
    if (
      'detail' in event &&
      event.detail &&
      typeof event.detail.value === 'string'
    ) {
      setInputValue(event.detail.value);
      return;
    }
    if (
      'target' in event &&
      event.target &&
      typeof event.target.value === 'string'
    ) {
      setInputValue(event.target.value);
    }
  }

  function handleConfirm() {
    handleSend();
  }

  return (
    <View className='ai-input-bar'>
      <View className='ai-input-bar__container'>
        <Icon name='input-icon' className='ai-input-bar__input-icon' />
        <Textarea
          className='ai-input-bar__textarea'
          value={inputValue}
          onInput={handleInput}
          onConfirm={handleConfirm}
          placeholder='输入问题，如：推荐自由泳教练'
          placeholderClass='ai-input-bar__placeholder'
          disabled={disabled}
          maxlength={500}
          autoHeight
          showConfirmBar={false}
          fixed
        />
        <View
          className={`ai-input-bar__send ${canSend ? 'ai-input-bar__send--active' : 'ai-input-bar__send--disabled'}`}
          onClick={handleSend}
        >
          <Icon name='send' className='ai-input-bar__send-icon' />
        </View>
      </View>
      <View className='ai-input-bar__disclaimer'>
        <Icon name='shield' className='ai-input-bar__disclaimer-icon' />
        <Text className='ai-input-bar__disclaimer-text'>
          leyo 的回答由 AI 生成，仅供参考
        </Text>
      </View>
    </View>
  );
}
