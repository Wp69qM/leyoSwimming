import type {
  ChatResponse,
  CreateSessionResponse,
  QuickTag,
  SessionDetailResponse,
  SessionListResponse,
} from '@/types/ai-assistant';

export const MOCK_QUICK_TAGS: QuickTag[] = [
  {
    id: 'coach-freestyle',
    title: '自由泳教练',
    subtitle: '专业教练推荐',
    icon: 'swim',
    message: '推荐自由泳教练',
  },
  {
    id: 'budget-3000',
    title: '3000元套餐',
    subtitle: '高性价比之选',
    icon: 'crown',
    message: '预算3000的套餐',
  },
  {
    id: 'female-coach',
    title: '女教练',
    subtitle: '耐心细致',
    icon: 'female',
    message: '女教练推荐',
  },
  {
    id: 'adult-beginner',
    title: '成人零基础',
    subtitle: '包教包会',
    icon: 'seedling',
    message: '成人零基础',
  },
  {
    id: 'kids-swim',
    title: '儿童游泳',
    subtitle: '寓教于乐',
    icon: 'emotion',
    message: '儿童游泳',
  },
  {
    id: 'experience',
    title: '体验课',
    subtitle: '新人特惠99元',
    icon: 'gift',
    message: '体验课推荐',
  },
];

export const MOCK_CREATE_SESSION_RESPONSE: CreateSessionResponse = {
  sessionId: 'mock-session-id',
  title: '新会话',
  welcomeMessage:
    '你好呀！我是 leyo，你的专属游泳教练助理 🏊‍♂️ 有什么想了解的尽管问我～',
  suggestedQuestions: [
    '推荐自由泳教练',
    '预算3000的套餐',
    '女教练推荐',
    '成人零基础',
    '儿童游泳',
    '体验课推荐',
  ],
};

export const MOCK_CHAT_RESPONSE_COACH: ChatResponse = {
  sessionId: 'mock-session-id',
  message: {
    id: 'msg-2',
    role: 'assistant',
    content:
      '当然可以！我为你找到了 3 位擅长自由泳的教练，他们都获得了学员的一致好评，快来看看吧 👇',
    recommendations: [
      {
        type: 'coach',
        coachId: 101,
        name: '张教练',
        avatarUrl: null,
        rating: '4.9',
        teachingYears: 8,
        gender: 'male',
        referencePrice: '200',
        teachingStrokes: ['自由泳', '仰泳'],
        reason: '教学经验丰富，擅长成人零基础教学，学员进步快',
      },
      {
        type: 'coach',
        coachId: 102,
        name: '李教练',
        avatarUrl: null,
        rating: '4.8',
        teachingYears: 6,
        gender: 'female',
        referencePrice: '190',
        teachingStrokes: ['自由泳', '蛙泳'],
        reason: '温柔耐心，特别适合女性和儿童学员，口碑极佳',
      },
    ],
    suggestedQuestions: [
      '张教练的课程时间安排是怎样的？',
      '李教练可以预约体验课吗？',
      '预算3000左右有什么套餐推荐？',
    ],
  },
};

export const MOCK_CHAT_RESPONSE_PACKAGE: ChatResponse = {
  sessionId: 'mock-session-id',
  message: {
    id: 'msg-4',
    role: 'assistant',
    content: '3000 元预算的话，我推荐这两个套餐，性价比都很高哦 💰',
    recommendations: [
      {
        type: 'package',
        id: 201,
        name: '自由泳进阶套餐（16课时）',
        price: '2880',
        totalHours: 16,
        teachingType: '一对一教学',
        validDays: 180,
        stroke: '自由泳',
        reason: '性价比高，适合想系统学习自由泳的学员',
      },
      {
        type: 'package',
        id: 202,
        name: '综合泳姿提升套餐（20课时）',
        price: '3200',
        totalHours: 20,
        teachingType: '一对二教学',
        validDays: 240,
        stroke: '蛙泳 · 自由泳',
        reason: '课时多更划算，适合想全面提升泳姿的学员',
      },
    ],
    suggestedQuestions: ['这两个套餐可以同时购买吗？', '套餐可以指定教练吗？'],
  },
};

export const MOCK_CHAT_RESPONSE_CUSTOM_PACKAGE: ChatResponse = {
  sessionId: 'mock-session-id',
  message: {
    id: 'msg-6',
    role: 'assistant',
    content:
      '根据你的需求，我为你生成了一个自定义课时套餐方案，你可以参考一下 👇',
    recommendations: [
      {
        type: 'custom_package',
        coachId: 101,
        coachName: '张教练',
        referencePrice: '200',
        hours: 12,
        estimatedTotalPrice: '2400',
        teachingType: '一对一',
        stroke: '自由泳',
        reason: '按参考单价预估，12 课时一对一自由泳教学，适合系统入门',
      },
    ],
    suggestedQuestions: ['可以调整课时数吗？', '有效期是多久？'],
  },
};

export const MOCK_SESSION_LIST_RESPONSE: SessionListResponse = {
  sessions: [
    {
      sessionId: 'mock-session-id',
      title: '自由泳教练推荐',
      lastMessageTime: '今天 14:32',
    },
    {
      sessionId: 'session-2',
      title: '预算3000的套餐推荐',
      lastMessageTime: '昨天 20:15',
    },
    {
      sessionId: 'session-3',
      title: '儿童游泳课程咨询',
      lastMessageTime: '昨天 10:08',
    },
    {
      sessionId: 'session-4',
      title: '成人零基础学游泳',
      lastMessageTime: '3月12日 16:45',
    },
    {
      sessionId: 'session-5',
      title: '体验课推荐',
      lastMessageTime: '3月10日 09:30',
    },
    {
      sessionId: 'session-6',
      title: '女教练推荐',
      lastMessageTime: '3月8日 14:20',
    },
  ],
};

export const MOCK_SESSION_DETAIL_RESPONSE: SessionDetailResponse = {
  sessionId: 'session-2',
  title: '预算3000的套餐推荐',
  messages: [
    {
      id: 'msg-1',
      role: 'user',
      content: '预算3000左右，有什么套餐推荐？',
    },
    {
      id: 'msg-2',
      role: 'assistant',
      content:
        '根据你的预算，leyo 为你精选了以下 3 个高性价比套餐，都在 3000 元以内哦～',
      recommendations: [
        {
          type: 'package',
          id: 203,
          name: '10 节正价课',
          price: '1800',
          totalHours: 10,
          teachingType: '一对一',
          validDays: 90,
          stroke: '自由泳 · 仰泳',
          reason: '性价比高，适合想系统学习自由泳的学员',
        },
        {
          type: 'package',
          id: 204,
          name: '6 节正价课',
          price: '1140',
          totalHours: 6,
          teachingType: '一对一',
          validDays: 60,
          stroke: '蛙泳 · 自由泳',
          reason: '短期见效，适合时间不固定的学员',
        },
        {
          type: 'package',
          id: 205,
          name: '新人体验课',
          price: '99',
          totalHours: 1,
          teachingType: '一对一',
          validDays: 30,
          stroke: '自由泳',
          reason: '新人特惠，先体验再决定',
        },
      ],
      suggestedQuestions: [
        '这三个套餐有什么区别？',
        '可以先预约体验课吗？',
        '套餐有效期是多久？',
      ],
    },
  ],
};
