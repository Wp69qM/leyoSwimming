import type {
  ChatMessage,
  ChatRequest,
  ChatResponse,
  CreateSessionResponse,
  MessageRole,
  Recommendation,
  SessionDetailResponse,
  SessionListItem,
  SessionListResponse,
} from '@/types/ai-assistant';
import { request } from './request';

interface BackendRecommendation {
  type: 'coach' | 'package' | 'custom_package';
  id?: number;
  coachId?: number;
  coachName?: string;
  name?: string;
  avatarUrl?: string | null;
  rating?: number | string;
  teachingYears?: number;
  referencePrice?: number;
  price?: number;
  hours?: number;
  pricePerHour?: number;
  totalPrice?: number;
  classSize?: string;
  validityDays?: number;
  strokes?: string[];
  reason?: string;
}

interface BackendChatResponse {
  sessionId: string;
  messageId: string;
  reply: {
    text: string;
    recommendations?: BackendRecommendation[];
    suggestedQuestions?: string[];
  };
}

interface BackendCreateSessionResponse {
  sessionId: string;
  welcomeMessage: string;
  suggestedQuestions: string[];
}

interface BackendSessionListItem {
  sessionId: string;
  title: string;
  lastMessageAt: string;
  messageCount: number;
}

interface BackendSessionListResponse {
  items: BackendSessionListItem[];
  total: number;
  page: number;
  size: number;
}

interface BackendSessionDetailResponse {
  sessionId: string;
  messages: BackendRawMessage[];
}

interface BackendRawMessage {
  role: MessageRole | string;
  content?: string;
  recommendations?: BackendRecommendation[];
  suggestedQuestions?: string[];
}

function mapRecommendation(item: BackendRecommendation): Recommendation {
  const base = {
    reason: item.reason ?? '',
  };

  if (item.type === 'coach') {
    return {
      type: 'coach',
      ...base,
      coachId: item.coachId ?? 0,
      name: item.name ?? '',
      avatarUrl: item.avatarUrl ?? null,
      rating: String(item.rating ?? ''),
      teachingYears: item.teachingYears ?? 0,
      gender: '',
      referencePrice: String(item.referencePrice ?? ''),
      teachingStrokes: item.strokes ?? [],
    };
  }

  if (item.type === 'package') {
    return {
      type: 'package',
      ...base,
      id: item.id ?? 0,
      name: item.name ?? '',
      price: String(item.price ?? ''),
      totalHours: item.hours ?? 0,
      teachingType: item.classSize ?? '',
      validDays: item.validityDays ?? 0,
      stroke: item.strokes?.[0] ?? '',
    };
  }

  return {
    type: 'custom_package',
    ...base,
    coachId: item.coachId ?? 0,
    coachName: item.coachName ?? '',
    referencePrice: String(item.referencePrice ?? ''),
    hours: item.hours ?? 0,
    estimatedTotalPrice: String(item.totalPrice ?? ''),
    teachingType: item.classSize ?? '',
    stroke: item.strokes?.[0] ?? '',
  };
}

function mapBackendMessage(msg: BackendRawMessage): ChatMessage | null {
  if (msg.role !== 'user' && msg.role !== 'assistant') {
    return null;
  }
  return {
    role: msg.role,
    content: msg.content ?? '',
    recommendations: (msg.recommendations ?? []).map(mapRecommendation),
    suggestedQuestions: msg.suggestedQuestions ?? [],
  };
}

export function chat(data: ChatRequest): Promise<ChatResponse> {
  return request<BackendChatResponse>({
    url: '/ai-assistant/chat',
    method: 'POST',
    data,
    needToken: false,
  }).then((res) => {
    const message: ChatMessage = {
      id: res.messageId,
      role: 'assistant',
      content: res.reply.text ?? '',
      recommendations: (res.reply.recommendations ?? []).map(mapRecommendation),
      suggestedQuestions: res.reply.suggestedQuestions ?? [],
    };

    return {
      sessionId: res.sessionId,
      message,
    };
  });
}

export function createSession(): Promise<CreateSessionResponse> {
  return request<BackendCreateSessionResponse>({
    url: '/ai-assistant/session/create',
    method: 'POST',
    data: {},
    needToken: true,
  }).then((res) => ({
    sessionId: res.sessionId,
    title: '',
    welcomeMessage: res.welcomeMessage,
    suggestedQuestions: res.suggestedQuestions ?? [],
  }));
}

export function listSessions(): Promise<SessionListResponse> {
  return request<BackendSessionListResponse>({
    url: '/ai-assistant/session/list',
    method: 'POST',
    data: {},
    needToken: true,
  }).then((res) => ({
    sessions: (res.items ?? []).map(
      (item): SessionListItem => ({
        sessionId: item.sessionId,
        title: item.title,
        lastMessageTime: item.lastMessageAt,
      })
    ),
  }));
}

export function getSessionDetail(
  sessionId: string
): Promise<SessionDetailResponse> {
  return request<BackendSessionDetailResponse>({
    url: '/ai-assistant/session/detail',
    method: 'POST',
    data: { sessionId },
    needToken: true,
  }).then((res) => ({
    sessionId: res.sessionId,
    title: '',
    messages: (res.messages ?? [])
      .map(mapBackendMessage)
      .filter((m): m is ChatMessage => m !== null),
  }));
}
