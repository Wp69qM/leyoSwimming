import { useEffect, useState, useCallback } from 'react';
import { View } from '@tarojs/components';
import Taro from '@tarojs/taro';
import {
  chat,
  createSession,
  getSessionDetail,
  listSessions,
} from '@/api/ai-assistant';
import { handleBusinessError } from '@/api/request';
import { MOCK_QUICK_TAGS } from '@/mocks/ai-assistant';
import { useAuthStore } from '@/stores/authStore';
import type {
  ChatMessage,
  QuickTag,
  SessionListItem,
} from '@/types/ai-assistant';
import {
  AiHeader,
  AiChatArea,
  AiQuickTags,
  AiInputBar,
  AiHistoryDrawer,
} from './components';
import './index.scss';

export default function AiAssistantPage() {
  const isLoggedIn = useAuthStore((state) => state.isLoggedIn);

  const [sessionId, setSessionId] = useState<string | null>(null);
  const [welcomeMessage, setWelcomeMessage] = useState<string>('');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [quickTags, setQuickTags] = useState<QuickTag[]>(MOCK_QUICK_TAGS);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [historyVisible, setHistoryVisible] = useState(false);
  const [historySessions, setHistorySessions] = useState<SessionListItem[]>([]);

  const initSession = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await createSession();
      setSessionId(data.sessionId);
      setWelcomeMessage(data.welcomeMessage);
      setMessages([]);
      const questions = data.suggestedQuestions ?? [];
      if (questions.length > 0) {
        setQuickTags(buildQuickTagsFromQuestions(questions));
      } else {
        setQuickTags(MOCK_QUICK_TAGS);
      }
    } catch (err) {
      setError(handleBusinessError(err));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void initSession();
  }, [initSession]);

  async function handleSend(message: string) {
    if (!sessionId) return;

    const userMessage: ChatMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: message,
    };

    setMessages((prev) => [...prev, userMessage]);
    setIsLoading(true);
    setError(null);

    try {
      const response = await chat({ sessionId, message });
      setSessionId(response.sessionId);
      setMessages((prev) => [...prev, response.message]);
    } catch (err) {
      setError(handleBusinessError(err));
    } finally {
      setIsLoading(false);
    }
  }

  function handleNewSession() {
    void initSession();
  }

  async function handleOpenHistory() {
    if (!isLoggedIn) {
      void Taro.navigateTo({ url: '/pages/login/wechat/index' });
      return;
    }

    setHistoryVisible(true);
    try {
      const data = await listSessions();
      setHistorySessions(data.sessions);
    } catch (err) {
      Taro.showToast({
        title: handleBusinessError(err),
        icon: 'none',
      });
    }
  }

  function handleClose() {
    void Taro.switchTab({ url: '/pages/index/index' });
  }

  function handleCloseHistory() {
    setHistoryVisible(false);
  }

  async function handleSelectSession(selectedSessionId: string) {
    setHistoryVisible(false);
    setIsLoading(true);
    setError(null);
    try {
      const data = await getSessionDetail(selectedSessionId);
      setSessionId(data.sessionId);
      setWelcomeMessage(
        '你好呀！我是 leyo，你的专属游泳教练助理 🏊‍♂️ 有什么想了解的尽管问我～'
      );
      setMessages(data.messages);
    } catch (err) {
      setError(handleBusinessError(err));
    } finally {
      setIsLoading(false);
    }
  }

  function handleRetry() {
    if (messages.length === 0) {
      void initSession();
      return;
    }

    const lastUserMessage = [...messages]
      .reverse()
      .find((msg) => msg.role === 'user');
    if (lastUserMessage) {
      void handleSend(lastUserMessage.content);
    }
  }

  const showQuickTags = messages.length === 0 && !error;

  return (
    <View className='ai-assistant-page'>
      <AiHeader
        isLoggedIn={isLoggedIn}
        onNewSession={handleNewSession}
        onOpenHistory={handleOpenHistory}
        onClose={handleClose}
      />

      <View className='ai-assistant-page__body'>
        <AiChatArea
          welcomeMessage={welcomeMessage}
          messages={messages}
          isLoading={isLoading}
          error={error}
          onRetry={handleRetry}
          onSuggestedQuestionClick={handleSend}
        />

        {showQuickTags && (
          <AiQuickTags tags={quickTags} onTagClick={handleSend} />
        )}
      </View>

      <AiInputBar onSend={handleSend} disabled={isLoading || !sessionId} />

      <AiHistoryDrawer
        visible={historyVisible}
        sessions={historySessions}
        currentSessionId={sessionId}
        onClose={handleCloseHistory}
        onSelectSession={handleSelectSession}
      />
    </View>
  );
}

function buildQuickTagsFromQuestions(questions: string[]): QuickTag[] {
  return questions.slice(0, 6).map((question, index) => ({
    id: `quick-${index}`,
    title: getTagTitle(question),
    subtitle: question,
    icon: getTagIcon(index),
    message: question,
  }));
}

function getTagTitle(question: string): string {
  if (question.includes('教练')) return question.replace('推荐', '');
  if (question.includes('套餐')) return '套餐推荐';
  if (question.includes('体验')) return '体验课';
  return question;
}

function getTagIcon(index: number): string {
  const icons = ['swim', 'crown', 'female', 'seedling', 'emotion', 'gift'];
  return icons[index % icons.length];
}
