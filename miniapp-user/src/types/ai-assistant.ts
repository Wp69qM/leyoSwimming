export type MessageRole = 'user' | 'assistant';

export type RecommendationType = 'coach' | 'package' | 'custom_package';

export interface CoachRecommendation {
  type: 'coach';
  coachId: number;
  name: string;
  avatarUrl: string | null;
  rating: string;
  teachingYears: number;
  gender: 'male' | 'female' | string;
  referencePrice: string;
  teachingStrokes: string[];
  reason: string;
}

export interface PackageRecommendation {
  type: 'package';
  id: number;
  name: string;
  price: string;
  totalHours: number;
  teachingType: string;
  validDays: number;
  stroke: string;
  reason: string;
}

export interface CustomPackageRecommendation {
  type: 'custom_package';
  coachId: number;
  coachName: string;
  referencePrice: string;
  hours: number;
  estimatedTotalPrice: string;
  teachingType: string;
  stroke: string;
  reason: string;
}

export type Recommendation =
  CoachRecommendation | PackageRecommendation | CustomPackageRecommendation;

export interface ChatMessage {
  id?: string | number;
  role: MessageRole;
  content: string;
  recommendations?: Recommendation[];
  suggestedQuestions?: string[];
  createdAt?: string;
}

export interface ChatRequest {
  sessionId: string | null;
  message: string;
}

export interface ChatResponse {
  sessionId: string;
  message: ChatMessage;
}

export interface CreateSessionResponse {
  sessionId: string;
  title: string;
  welcomeMessage: string;
  suggestedQuestions: string[];
}

export interface SessionListItem {
  sessionId: string;
  title: string;
  lastMessageTime: string;
}

export interface SessionListResponse {
  sessions: SessionListItem[];
}

export interface SessionDetailResponse {
  sessionId: string;
  title: string;
  messages: ChatMessage[];
}

export interface QuickTag {
  id: string;
  title: string;
  subtitle: string;
  icon: string;
  message: string;
}

export type AiPageStatus = 'idle' | 'loading' | 'error' | 'success';
