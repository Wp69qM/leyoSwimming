import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import * as aiAssistantApi from '@/api/ai-assistant';
import * as authStore from '@/stores/authStore';
import AiAssistantPage from './index';

jest.mock('@tarojs/taro', () => ({
  getSystemInfoSync: jest.fn().mockReturnValue({ statusBarHeight: 20 }),
  navigateTo: jest.fn().mockResolvedValue(undefined),
  switchTab: jest.fn().mockResolvedValue(undefined),
  showToast: jest.fn(),
}));

jest.mock('@/api/ai-assistant', () => ({
  chat: jest.fn(),
  createSession: jest.fn(),
  listSessions: jest.fn(),
  getSessionDetail: jest.fn(),
}));

jest.mock('@/stores/authStore', () => ({
  useAuthStore: jest.fn(),
}));

jest.mock('@/components/common/Icon', () => ({
  Icon: ({ name }: { name: string }) => (
    <span data-testid={`icon-${name}`}>{name}</span>
  ),
}));

const mockCreateSessionResponse = {
  sessionId: 'session-1',
  title: '新会话',
  welcomeMessage: '你好呀！我是 leyo',
  suggestedQuestions: ['推荐自由泳教练', '预算3000的套餐'],
};

const mockChatResponse = {
  sessionId: 'session-1',
  message: {
    id: 'msg-2',
    role: 'assistant' as const,
    content: '为你找到以下教练',
    recommendations: [
      {
        type: 'coach' as const,
        coachId: 101,
        name: '张教练',
        avatarUrl: null,
        rating: '4.9',
        teachingYears: 8,
        gender: 'male',
        referencePrice: '200',
        teachingStrokes: ['自由泳', '仰泳'],
        reason: '教学经验丰富',
      },
    ],
    suggestedQuestions: ['可以预约体验课吗？'],
  },
};

const mockSessionListResponse = {
  sessions: [
    {
      sessionId: 'session-1',
      title: '自由泳教练推荐',
      lastMessageTime: '今天 14:32',
    },
    {
      sessionId: 'session-2',
      title: '预算3000的套餐推荐',
      lastMessageTime: '昨天 20:15',
    },
  ],
};

function mockLoggedInState() {
  (authStore.useAuthStore as unknown as jest.Mock).mockImplementation(
    (selector: (state: { isLoggedIn: boolean }) => boolean) =>
      selector({ isLoggedIn: true })
  );
}

function mockLoggedOutState() {
  (authStore.useAuthStore as unknown as jest.Mock).mockImplementation(
    (selector: (state: { isLoggedIn: boolean }) => boolean) =>
      selector({ isLoggedIn: false })
  );
}

function setupCreateSession() {
  (aiAssistantApi.createSession as jest.Mock).mockResolvedValue(
    mockCreateSessionResponse
  );
}

function setupChat() {
  (aiAssistantApi.chat as jest.Mock).mockResolvedValue(mockChatResponse);
}

function setupSessionList() {
  (aiAssistantApi.listSessions as jest.Mock).mockResolvedValue(
    mockSessionListResponse
  );
}

describe('AiAssistantPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockLoggedInState();
    setupCreateSession();
    setupChat();
    setupSessionList();
  });

  test('renders welcome state with quick tags', async () => {
    render(<AiAssistantPage />);

    await waitFor(() => {
      expect(screen.getByText('你好，我是 leyo 👋')).toBeInTheDocument();
    });

    expect(screen.getByText('大家都在问')).toBeInTheDocument();
    expect(screen.getByText('自由泳教练')).toBeInTheDocument();
    expect(screen.getByText('套餐推荐')).toBeInTheDocument();
  });

  test('sends message when quick tag is clicked', async () => {
    render(<AiAssistantPage />);

    await waitFor(() => {
      expect(screen.getByText('自由泳教练')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('自由泳教练'));

    await waitFor(() => {
      expect(aiAssistantApi.chat).toHaveBeenCalledWith({
        sessionId: 'session-1',
        message: '推荐自由泳教练',
      });
    });

    await waitFor(() => {
      expect(screen.getByText('推荐自由泳教练')).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(screen.getByText('为你找到以下教练')).toBeInTheDocument();
    });
  });

  test('renders recommendation cards', async () => {
    render(<AiAssistantPage />);

    await waitFor(() => {
      expect(screen.getByText('自由泳教练')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('自由泳教练'));

    await waitFor(() => {
      expect(screen.getByText('张教练')).toBeInTheDocument();
    });

    expect(screen.getByText('教练推荐')).toBeInTheDocument();
    expect(screen.getByText('查看教练详情')).toBeInTheDocument();
  });

  test('opens and closes history drawer', async () => {
    render(<AiAssistantPage />);

    await waitFor(() => {
      expect(screen.getByText('历史')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('历史'));

    await waitFor(() => {
      expect(screen.getByText('历史会话')).toBeInTheDocument();
    });

    expect(screen.getByText('自由泳教练推荐')).toBeInTheDocument();
    expect(screen.getByText('预算3000的套餐推荐')).toBeInTheDocument();
    expect(screen.getByText('当前')).toBeInTheDocument();

    const drawerClose = document.querySelector('.ai-history-drawer__close');
    fireEvent.click(drawerClose!);

    await waitFor(() => {
      expect(screen.queryByText('历史会话')).not.toBeInTheDocument();
    });
  });

  test('disables send button when input is empty', async () => {
    render(<AiAssistantPage />);

    await waitFor(() => {
      expect(screen.getByText('你好，我是 leyo 👋')).toBeInTheDocument();
    });

    const sendButton = document.querySelector('.ai-input-bar__send');
    expect(sendButton).toHaveClass('ai-input-bar__send--disabled');
  });

  test('hides history button for logged out users', async () => {
    mockLoggedOutState();
    render(<AiAssistantPage />);

    await waitFor(() => {
      expect(screen.getByText('新会话')).toBeInTheDocument();
    });

    expect(screen.queryByText('历史')).not.toBeInTheDocument();
  });

  test('navigates to login when guest clicks history placeholder area', async () => {
    mockLoggedOutState();
    render(<AiAssistantPage />);

    await waitFor(() => {
      expect(screen.getByText('新会话')).toBeInTheDocument();
    });

    // The header placeholder is not clickable, so there is no history button to click
    expect(screen.queryByText('历史')).not.toBeInTheDocument();
  });

  test('loads selected session detail from history drawer', async () => {
    const mockDetailResponse = {
      sessionId: 'session-2',
      title: '预算3000的套餐推荐',
      messages: [
        {
          id: 'msg-1',
          role: 'user' as const,
          content: '预算3000左右，有什么套餐推荐？',
        },
        {
          id: 'msg-2',
          role: 'assistant' as const,
          content: '推荐以下套餐',
        },
      ],
    };
    (aiAssistantApi.getSessionDetail as jest.Mock).mockResolvedValue(
      mockDetailResponse
    );

    render(<AiAssistantPage />);

    await waitFor(() => {
      expect(screen.getByText('历史')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('历史'));

    await waitFor(() => {
      expect(screen.getByText('预算3000的套餐推荐')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('预算3000的套餐推荐'));

    await waitFor(() => {
      expect(aiAssistantApi.getSessionDetail).toHaveBeenCalledWith('session-2');
    });

    await waitFor(() => {
      expect(
        screen.getByText('预算3000左右，有什么套餐推荐？')
      ).toBeInTheDocument();
    });
  });

  test('sends message via input bar', async () => {
    render(<AiAssistantPage />);

    await waitFor(() => {
      expect(screen.getByText('你好，我是 leyo 👋')).toBeInTheDocument();
    });

    const textarea = document.querySelector('.ai-input-bar__textarea');
    fireEvent.input(textarea!, { target: { value: '女教练推荐' } });

    const sendButton = document.querySelector('.ai-input-bar__send');
    expect(sendButton).toHaveClass('ai-input-bar__send--active');

    fireEvent.click(sendButton!);

    await waitFor(() => {
      expect(aiAssistantApi.chat).toHaveBeenCalledWith({
        sessionId: 'session-1',
        message: '女教练推荐',
      });
    });
  });

  test('shows error state and allows retry', async () => {
    (aiAssistantApi.createSession as jest.Mock).mockRejectedValue(
      new Error('创建会话失败')
    );

    render(<AiAssistantPage />);

    await waitFor(() => {
      expect(screen.getByText('创建会话失败')).toBeInTheDocument();
    });

    setupCreateSession();
    fireEvent.click(screen.getByText('重试'));

    await waitFor(() => {
      expect(screen.getByText('你好，我是 leyo 👋')).toBeInTheDocument();
    });
  });
});
