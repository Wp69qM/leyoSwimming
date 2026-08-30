import Taro, { useDidShow } from '@tarojs/taro';

export default function AiAssistantEntryPage() {
  useDidShow(() => {
    Taro.redirectTo({ url: '/pages/ai-assistant/index' });
  });

  return null;
}
