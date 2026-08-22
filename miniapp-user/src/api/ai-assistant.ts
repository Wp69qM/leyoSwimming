import type {
  ChatMessage,
  ChatRequest,
  ChatResponse,
  CreateSessionResponse,
  Recommendation,
  SessionDetailResponse,
  SessionListResponse,
} from '@/types/ai-assistant';
import { request } from './request';

interface BackendRecommendation {
  type: 'coach' | 'package' | 'custom_package';
  id?: number;
  coach_id?: number;
  coach_name?: string;
  name?: string;
  avatar_url?: string | null;
  rating?: number | string;
  teaching_years?: number;
  reference_price?: number;
  price?: number;
  hours?: number;
  price_per_hour?: number;
  total_price?: number;
  class_size?: string;
  validity_days?: number;
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

function mapRecommendation(item: BackendRecommendation): Recommendation {
  const base = {
    reason: item.reason ?? '',
  };

  if (item.type === 'coach') {
    return {
      type: 'coach',
      ...base,
      coachId: item.coach_id ?? 0,
      name: item.name ?? '',
      avatarUrl: item.avatar_url ?? null,
      rating: String(item.rating ?? ''),
      teachingYears: item.teaching_years ?? 0,
      gender: '',
      referencePrice: String(item.reference_price ?? ''),
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
      teachingType: item.class_size ?? '',
      validDays: item.validity_days ?? 0,
      stroke: item.strokes?.[0] ?? '',
    };
  }

  return {
    type: 'custom_package',
    ...base,
    coachId: item.coach_id ?? 0,
    coachName: item.coach_name ?? '',
    referencePrice: String(item.reference_price ?? ''),
    hours: item.hours ?? 0,
    estimatedTotalPrice: String(item.total_price ?? ''),
    teachingType: item.class_size ?? '',
    stroke: item.strokes?.[0] ?? '',
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
  return request<CreateSessionResponse>({
    url: '/ai-assistant/session/create',
    method: 'POST',
    data: {},
    needToken: false,
  });
}

export function listSessions(): Promise<SessionListResponse> {
  return request<SessionListResponse>({
    url: '/ai-assistant/session/list',
    method: 'POST',
    data: {},
    needToken: true,
  });
}

export function getSessionDetail(
  sessionId: string
): Promise<SessionDetailResponse> {
  return request<SessionDetailResponse>({
    url: '/ai-assistant/session/detail',
    method: 'POST',
    data: { sessionId },
    needToken: true,
  });
}
