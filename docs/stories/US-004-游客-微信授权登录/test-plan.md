# US-004 实施任务清单（Tasks）

> **US 关联**：[user-story.md](./user-story.md)（US-004 用户微信授权登录）
> **文档角色**：TDD 任务清单 + 测试计划（双重角色）
> **状态**：🔲 待开发执行
> **作者**：开发　|　**最后更新**：2026-07-30

---

## 0. 双重角色说明

本文档承担**双重角色**：
- **TDD 任务清单**（superpowers 风格）：每个 task = 2-5 分钟可执行单元
- **测试计划**：覆盖 [user-story.md](./user-story.md) 中所有 GWT 场景

每个 Task 严格遵循 **RED → GREEN → COMMIT** 循环，不允许跳步。

---

## 1. Task 概览

| Task | 标题 | 优先级 | 对应 GWT 场景 |
|------|------|--------|--------------|
| 1 | User Repository（findByUnionId + create） | P0 | [§6.1 场景 1](./user-story.md#61-场景-1首次微信授权登录成功跳转补充资料页正常路径)、[§6.2 场景 2](./user-story.md#62-场景-2已注册用户微信授权登录成功跳转首页正常路径) |
| 2 | 微信 OAuth Service（code2session + 用户创建/查询） | P0 | §6.1、§6.2、[§6.4 场景 4](./user-story.md#64-场景-4微信接口调用失败异常路径)、[§6.5 场景 5](./user-story.md#65-场景-5登录凭证已失效异常路径) |
| 3 | JWT 签发与验证 | P0 | §6.1、§6.2（token 签发） |
| 4 | POST /auth/wechat-login API 端点 | P0 | §6.1-6.5 全部场景 |
| 5 | 小程序登录页 | P1 | [§6.3 场景 3](./user-story.md#63-场景-3用户拒绝授权异常路径)、§6.1、§6.2 |

---

## 2. 实施任务

### Task 1: User Repository（findByUnionId + create）[P0]

**Files:**
- Create: `backend/src/repositories/user.ts`
- Test: `backend/tests/repositories/user.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1首次微信授权登录成功跳转补充资料页正常路径)、[§6.2 场景 2](./user-story.md#62-场景-2已注册用户微信授权登录成功跳转首页正常路径)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/repositories/user.test.ts
import { UserRepository } from '../../src/repositories/user';

describe('UserRepository', () => {
  describe('findByUnionId', () => {
    it('returns active user when union_id matches (老用户复用 — 场景 2)', async () => {
      const repo = new UserRepository();
      const user = await repo.findByUnionId('union_xxx_001');  // 预置数据：status=0

      expect(user).toMatchObject({
        id: expect.any(Number),
        union_id: 'union_xxx_001',
        identity_status: '注册用户',
        status: 0,
      });
    });

    it('returns null when union_id matches a deleted user (软删除账号不复用)', async () => {
      const repo = new UserRepository();
      // 预置数据：union_id='union_deleted' 的记录 status=1
      const user = await repo.findByUnionId('union_deleted');
      expect(user).toBeNull();
    });

    it('returns null when union_id not found (新用户 — 场景 1)', async () => {
      const repo = new UserRepository();
      const user = await repo.findByUnionId('union_not_exist');
      expect(user).toBeNull();
    });
  });

  describe('create', () => {
    it('creates new user with identity_status=注册用户 (状态机转换：游客→注册用户)', async () => {
      const repo = new UserRepository();
      const newUser = await repo.create({
        openid: 'openid_new',
        union_id: 'union_new',
        identity_status: '注册用户',
        profile_completed: false,
        status: 0,
      });

      expect(newUser).toMatchObject({
        id: expect.any(Number),
        openid: 'openid_new',
        union_id: 'union_new',
        identity_status: '注册用户',
        profile_completed: false,
        status: 0,
      });
    });
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- user.test.ts`
Expected: FAIL with `Cannot find module '../../src/repositories/user'`

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/repositories/user.ts
export interface UserCreateInput {
  openid: string;
  union_id: string;
  identity_status: string;
  profile_completed: boolean;
  status: string;
}

export interface User {
  id: number;
  openid: string;
  union_id: string;
  identity_status: string;
  profile_completed: boolean;
  status: string;
  created_at: Date;
  updated_at: Date;
}

export class UserRepository {
  // 按 union_id 查询 active 用户（软删除账号不返回，符合 PRD §5.2.1 第 4 条）
  async findByUnionId(unionId: string): Promise<User | null> {
    const user = await db('user')
      .where({ union_id: unionId, status: 0 })
      .first();
    return user || null;
  }

  // 创建新用户（首次微信授权登录：identity_status 直接置为 '注册用户'）
  async create(input: UserCreateInput): Promise<User> {
    const [id] = await db('user').insert({
      openid: input.openid,
      union_id: input.union_id,
      identity_status: input.identity_status,  // '注册用户' — 状态机转换
      profile_completed: input.profile_completed,
      status: input.status,
      created_at: new Date(),
      updated_at: new Date(),
    });
    return this.findById(id);
  }

  async findById(id: number): Promise<User> {
    return db('user').where({ id }).first();
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- user.test.ts`
Expected: PASS（4 个测试全过）

- [ ] **Step 5: Commit**

```bash
git add backend/src/repositories/user.ts backend/tests/repositories/user.test.ts
git commit -m "feat(user): add UserRepository with findByUnionId and create (identity_status transition)"
```

---

### Task 2: 微信 OAuth Service（code2session 调用 + 用户创建/查询）[P0]

**Files:**
- Create: `backend/src/services/wechat-auth.ts`
- Create: `backend/src/clients/wechat.ts`
- Test: `backend/tests/services/wechat-auth.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1首次微信授权登录成功跳转补充资料页正常路径)、[§6.2 场景 2](./user-story.md#62-场景-2已注册用户微信授权登录成功跳转首页正常路径)、[§6.4 场景 4](./user-story.md#64-场景-4微信接口调用失败异常路径)、[§6.5 场景 5](./user-story.md#65-场景-5登录凭证已失效异常路径)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/services/wechat-auth.test.ts
import { WechatAuthService } from '../../src/services/wechat-auth';

// mock 微信客户端
jest.mock('../../src/clients/wechat', () => ({
  code2session: jest.fn(),
}));

describe('WechatAuthService', () => {
  describe('authenticate', () => {
    it('场景 1: 首次登录创建新用户，identity_status=注册用户', async () => {
      const wechat = require('../../src/clients/wechat');
      wechat.code2session.mockResolvedValue({
        openid: 'openid_new',
        unionid: 'union_new',
        session_key: 'session_key_new',
      });

      const service = new WechatAuthService();
      const result = await service.authenticate('code_new');

      expect(result.user.identity_status).toBe('注册用户');
      expect(result.user.profile_completed).toBe(false);
      expect(result.isNewUser).toBe(true);
      // session_key 应缓存到 Redis
      expect(await redis.get(`wechat:session_key:${result.user.id}`)).toBe('session_key_new');
    });

    it('场景 2: 老用户登录复用账号，不修改 identity_status', async () => {
      const wechat = require('../../src/clients/wechat');
      wechat.code2session.mockResolvedValue({
        openid: 'openid_existing',
        unionid: 'union_xxx_001',  // 预置数据中存在
        session_key: 'session_key_old',
      });

      const service = new WechatAuthService();
      const result = await service.authenticate('code_old');

      expect(result.user.id).toBe(1001);  // 复用已有记录
      expect(result.isNewUser).toBe(false);
    });

    it('场景 5: code 失效返回 WECHAT_CODE_INVALID', async () => {
      const wechat = require('../../src/clients/wechat');
      wechat.code2session.mockResolvedValue({ errcode: 40029, errmsg: 'invalid code' });

      const service = new WechatAuthService();
      await expect(service.authenticate('invalid_code')).rejects.toThrow('WECHAT_CODE_INVALID');
    });

    it('场景 4: 微信接口超时返回 WECHAT_API_TIMEOUT', async () => {
      const wechat = require('../../src/clients/wechat');
      wechat.code2session.mockRejectedValue(new Error('ETIMEDOUT'));

      const service = new WechatAuthService();
      await expect(service.authenticate('timeout_code')).rejects.toThrow('WECHAT_API_TIMEOUT');
    });
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- wechat-auth.test.ts`
Expected: FAIL with `Cannot find module '../../src/services/wechat-auth'`

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/clients/wechat.ts
import axios from 'axios';

const WECHAT_API_BASE = 'https://api.weixin.qq.com';
const CODE2SESSION_PATH = '/sns/jscode2session';
const TIMEOUT_MS = 3000;  // 3s 超时

// 调用微信 code2session 接口
export async function code2session(code: string) {
  try {
    const res = await axios.get(`${WECHAT_API_BASE}${CODE2SESSION_PATH}`, {
      params: {
        appid: process.env.WX_APPID,
        secret: process.env.WX_SECRET,
        js_code: code,
        grant_type: 'authorization_code',
      },
      timeout: TIMEOUT_MS,
    });
    return res.data;
  } catch (err) {
    if (err.code === 'ETIMEDOUT' || err.code === 'ECONNABORTED') {
      throw new Error('WECHAT_API_TIMEOUT');
    }
    throw new Error('WECHAT_API_ERROR');
  }
}

// backend/src/services/wechat-auth.ts
import { code2session } from '../clients/wechat';
import { UserRepository } from '../repositories/user';

export interface AuthResult {
  user: { id: number; identity_status: string; profile_completed: boolean };
  sessionKey: string;
  isNewUser: boolean;
}

export class WechatAuthService {
  constructor(private userRepo = new UserRepository()) {}

  async authenticate(code: string): Promise<AuthResult> {
    // 1. 调用微信 code2session
    const wxSession = await code2session(code);

    // 2. 校验微信返回
    if (wxSession.errcode) {
      if (wxSession.errcode === 40029) {
        throw new Error('WECHAT_CODE_INVALID');
      }
      throw new Error('WECHAT_API_ERROR');
    }

    // 3. 按 union_id 查询用户
    const unionId = wxSession.unionid || null;
    const openid = wxSession.openid;
    const existingUser = unionId ? await this.userRepo.findByUnionId(unionId) : null;

    let user;
    let isNewUser;

    if (existingUser) {
      // 场景 2: 老用户复用
      user = existingUser;
      isNewUser = false;
    } else {
      // 场景 1: 新用户创建（状态机转换：游客→注册用户）
      user = await this.userRepo.create({
        openid,
        union_id: unionId || openid,  // union_id 缺失时用 openid 兜底
        identity_status: '注册用户',  // 核心状态转换
        profile_completed: false,
        status: 0,
      });
      isNewUser = true;
    }

    // 4. 缓存 session_key 到 Redis（TTL 7200s）
    await redis.set(
      `wechat:session_key:${user.id}`,
      wxSession.session_key,
      'EX',
      7200
    );

    return {
      user: {
        id: user.id,
        identity_status: user.identity_status,
        profile_completed: user.profile_completed,
      },
      sessionKey: wxSession.session_key,
      isNewUser,
    };
  }
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- wechat-auth.test.ts`
Expected: PASS（4 个测试全过）

- [ ] **Step 5: Commit**

```bash
git add backend/src/services/wechat-auth.ts backend/src/clients/wechat.ts backend/tests/services/wechat-auth.test.ts
git commit -m "feat(auth): add WechatAuthService with code2session and user creation (identity_status transition)"
```

---

### Task 3: JWT 签发与验证 [P0]

**Files:**
- Create: `backend/src/services/jwt.ts`
- Create: `backend/src/services/session.ts`
- Test: `backend/tests/services/jwt.test.ts`

**对应 GWT**：[user-story.md §6.1 场景 1](./user-story.md#61-场景-1首次微信授权登录成功跳转补充资料页正常路径)、[§6.2 场景 2](./user-story.md#62-场景-2已注册用户微信授权登录成功跳转首页正常路径)（token 签发）

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/services/jwt.test.ts
import { JwtService } from '../../src/services/jwt';
import { SessionService } from '../../src/services/session';

describe('JwtService', () => {
  it('signs access token with user_id and identity_status', () => {
    const jwt = new JwtService();
    const token = jwt.signAccessToken({
      userId: 1001,
      identityStatus: '注册用户',
      profileCompleted: false,
    });

    const payload = jwt.verifyAccessToken(token);
    expect(payload.sub).toBe('1001');
    expect(payload.identity_status).toBe('注册用户');
    expect(payload.profile_completed).toBe(false);
  });

  it('rejects expired access token', () => {
    const jwt = new JwtService();
    const expiredToken = jwt.signAccessToken(
      { userId: 1001, identityStatus: '注册用户', profileCompleted: false },
      { expiresIn: -1 }  // 已过期
    );
    expect(() => jwt.verifyAccessToken(expiredToken)).toThrow('TOKEN_EXPIRED');
  });
});

describe('SessionService', () => {
  it('creates session record with refresh_token hash', async () => {
    const session = new SessionService();
    const result = await session.create({
      userId: 1001,
      sessionKey: 'session_key_xxx',
    });

    expect(result.accessToken).toMatch(/^eyJ/);  // JWT 格式
    expect(result.refreshToken).toHaveLength(64);  // 32 字节 hex
    expect(result.expiresIn).toBe(7200);

    // DB 中应存在 session 记录，refresh_token 为 hash 而非明文
    const record = await db('user_session').where({ user_id: 1001 }).first();
    expect(record.refresh_token_hash).not.toBe(result.refreshToken);
    expect(record.refresh_token_hash).toHaveLength(64);  // SHA-256 hex
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- jwt.test.ts`
Expected: FAIL with `Cannot find module '../../src/services/jwt'`

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/services/jwt.ts
import jwt from 'jsonwebtoken';
import crypto from 'crypto';

export interface JwtPayload {
  userId: number;
  identityStatus: string;
  profileCompleted: boolean;
}

export class JwtService {
  private secret = process.env.JWT_SECRET || 'dev-secret';

  // 签发 access_token（有效期 2h）
  signAccessToken(payload: JwtPayload, opts?: { expiresIn?: number }): string {
    const exp = opts?.expiresIn ?? 7200;
    return jwt.sign(
      {
        sub: String(payload.userId),
        identity_status: payload.identityStatus,
        profile_completed: payload.profileCompleted,
      },
      this.secret,
      { expiresIn: exp > 0 ? exp : 1 }  // 支持测试传入负值
    );
  }

  verifyAccessToken(token: string): any {
    try {
      return jwt.verify(token, this.secret);
    } catch (err) {
      if (err.name === 'TokenExpiredError') {
        throw new Error('TOKEN_EXPIRED');
      }
      throw new Error('TOKEN_INVALID');
    }
  }
}

// backend/src/services/session.ts
import { JwtService, JwtPayload } from './jwt';
import crypto from 'crypto';

export class SessionService {
  constructor(private jwt = new JwtService()) {}

  async create(input: { userId: number; sessionKey: string; identityStatus: string; profileCompleted: boolean }) {
    // 1. 签发 access_token
    const accessToken = this.jwt.signAccessToken({
      userId: input.userId,
      identityStatus: input.identityStatus,
      profileCompleted: input.profileCompleted,
    });

    // 2. 生成 refresh_token（密码学安全随机数）
    const refreshToken = crypto.randomBytes(32).toString('hex');
    const refreshTokenHash = crypto.createHash('sha256').update(refreshToken).digest('hex');

    // 3. 写入 user_session 表（session_key 加密存储）
    await db('user_session').insert({
      user_id: input.userId,
      session_key_encrypted: encrypt(input.sessionKey),  // AES 加密
      refresh_token_hash: refreshTokenHash,
      expires_at: new Date(Date.now() + 7 * 24 * 3600 * 1000),  // 7d
      created_at: new Date(),
    });

    return {
      accessToken,
      refreshToken,
      expiresIn: 7200,
    };
  }
}

function encrypt(plain: string): string {
  // AES-256-CBC 加密，密钥从环境变量读取
  const key = Buffer.from(process.env.SESSION_KEY_SECRET || 'dev-session-key-32bytes!!', 'utf8');
  const iv = crypto.randomBytes(16);
  const cipher = crypto.createCipheriv('aes-256-cbc', key, iv);
  let encrypted = cipher.update(plain, 'utf8', 'hex');
  encrypted += cipher.final('hex');
  return iv.toString('hex') + ':' + encrypted;
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- jwt.test.ts`
Expected: PASS（3 个测试全过）

- [ ] **Step 5: Commit**

```bash
git add backend/src/services/jwt.ts backend/src/services/session.ts backend/tests/services/jwt.test.ts
git commit -m "feat(auth): add JwtService and SessionService with access_token + refresh_token"
```

---

### Task 4: POST /auth/wechat-login API 端点 [P0]

**Files:**
- Create: `backend/src/controllers/auth.ts`
- Create: `backend/src/routes/auth.ts`
- Test: `backend/tests/controllers/auth.test.ts`

**对应 GWT**：[§6.1 场景 1](./user-story.md#61-场景-1首次微信授权登录成功跳转补充资料页正常路径)、[§6.2 场景 2](./user-story.md#62-场景-2已注册用户微信授权登录成功跳转首页正常路径)、[§6.3 场景 3](./user-story.md#63-场景-3用户拒绝授权异常路径)、[§6.4 场景 4](./user-story.md#64-场景-4微信接口调用失败异常路径)、[§6.5 场景 5](./user-story.md#65-场景-5登录凭证已失效异常路径)

- [ ] **Step 1: 写失败测试**

```typescript
// backend/tests/controllers/auth.test.ts
import request from 'supertest';
import { app } from '../../src/app';

describe('POST /auth/wechat-login', () => {
  it('场景 1: 首次登录返回 200 + isNewUser=true + profileCompleted=false', async () => {
    // mock WechatAuthService: 新用户
    const res = await request(app)
      .post('/api/v1/auth/wechat-login')
      .send({ code: 'code_new_user' });

    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({
      accessToken: expect.stringMatching(/^eyJ/),
      refreshToken: expect.any(String),
      expiresIn: 7200,
      isNewUser: true,
      profileCompleted: false,
      userId: expect.any(Number),
    });
  });

  it('场景 2: 老用户登录返回 200 + isNewUser=false', async () => {
    const res = await request(app)
      .post('/api/v1/auth/wechat-login')
      .send({ code: 'code_old_user' });

    expect(res.status).toBe(200);
    expect(res.body.isNewUser).toBe(false);
    expect(res.body.profileCompleted).toBe(true);
  });

  it('场景 5: code 失效返回 401 + WECHAT_CODE_INVALID', async () => {
    const res = await request(app)
      .post('/api/v1/auth/wechat-login')
      .send({ code: 'invalid_code' });

    expect(res.status).toBe(401);
    expect(res.body.error).toBe('WECHAT_CODE_INVALID');
    expect(res.body.message).toBe('登录凭证已失效，请重新点击登录');
  });

  it('场景 4: 微信接口错误返回 502 + WECHAT_API_ERROR', async () => {
    const res = await request(app)
      .post('/api/v1/auth/wechat-login')
      .send({ code: 'wechat_error_code' });

    expect(res.status).toBe(502);
    expect(res.body.error).toBe('WECHAT_API_ERROR');
    expect(res.body.message).toBe('微信服务暂时不可用，请稍后重试');
  });

  it('场景 4 超时: 返回 504 + WECHAT_API_TIMEOUT', async () => {
    const res = await request(app)
      .post('/api/v1/auth/wechat-login')
      .send({ code: 'timeout_code' });

    expect(res.status).toBe(504);
    expect(res.body.error).toBe('WECHAT_API_TIMEOUT');
  });

  it('幂等: 相同 code 5 分钟内重复提交返回首次结果', async () => {
    const first = await request(app)
      .post('/api/v1/auth/wechat-login')
      .send({ code: 'idempotent_code' });

    const second = await request(app)
      .post('/api/v1/auth/wechat-login')
      .send({ code: 'idempotent_code' });

    expect(second.status).toBe(200);
    expect(second.body.userId).toBe(first.body.userId);
  });

  it('缺少 code 字段返回 400', async () => {
    const res = await request(app)
      .post('/api/v1/auth/wechat-login')
      .send({});

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('VALIDATION_ERROR');
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- auth.test.ts`
Expected: FAIL with `Cannot find module '../../src/app'` 或 404

- [ ] **Step 3: 写最小实现**

```typescript
// backend/src/controllers/auth.ts
import { WechatAuthService } from '../services/wechat-auth';
import { SessionService } from '../services/session';

const wechatAuth = new WechatAuthService();
const session = new SessionService();

export async function wechatLogin(ctx) {
  const { code } = ctx.request.body;

  // 1. 参数校验
  if (!code) {
    ctx.status = 400;
    ctx.body = { error: 'VALIDATION_ERROR', message: 'code 字段必填' };
    return;
  }

  // 2. 幂等校验（5 分钟内相同 code 返回首次结果）
  const idempotentKey = `auth:idempotent:wechat-login:${code}`;
  const cached = await redis.get(idempotentKey);
  if (cached) {
    ctx.body = JSON.parse(cached);
    return;
  }

  try {
    // 3. 调用微信 OAuth Service（含状态机转换：游客→注册用户）
    const authResult = await wechatAuth.authenticate(code);

    // 4. 签发 JWT + 写 session
    const tokens = await session.create({
      userId: authResult.user.id,
      sessionKey: authResult.sessionKey,
      identityStatus: authResult.user.identity_status,
      profileCompleted: authResult.user.profile_completed,
    });

    // 5. 组装响应
    const response = {
      ...tokens,
      isNewUser: authResult.isNewUser,
      profileCompleted: authResult.user.profile_completed,
      userId: authResult.user.id,
    };

    // 6. 缓存幂等结果（5 分钟）
    await redis.set(idempotentKey, JSON.stringify(response), 'EX', 300);

    ctx.body = response;
  } catch (err) {
    // 错误码映射
    const errorMap = {
      WECHAT_CODE_INVALID: { status: 401, message: '登录凭证已失效，请重新点击登录' },
      WECHAT_API_ERROR: { status: 502, message: '微信服务暂时不可用，请稍后重试' },
      WECHAT_API_TIMEOUT: { status: 504, message: '网络异常，请重试' },
    };
    const mapped = errorMap[err.message] || { status: 500, message: '登录失败，请重试' };
    ctx.status = mapped.status;
    ctx.body = { error: err.message, message: mapped.message };
  }
}

// backend/src/routes/auth.ts
import Router from 'koa-router';
import { wechatLogin } from '../controllers/auth';

const router = new Router({ prefix: '/api/v1/auth' });
router.post('/wechat-login', wechatLogin);
export default router;
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- auth.test.ts`
Expected: PASS（7 个测试全过）

- [ ] **Step 5: Commit**

```bash
git add backend/src/controllers/auth.ts backend/src/routes/auth.ts backend/tests/controllers/auth.test.ts
git commit -m "feat(api): add POST /auth/wechat-login endpoint with idempotency and error mapping"
```

---

### Task 5: 小程序登录页 [P1]

**Files:**
- Create: `miniapp-user/src/pages/login/index.tsx`
- Create: `miniapp-user/src/pages/login/index.test.tsx`

**对应 GWT**：[user-story.md §6.3 场景 3](./user-story.md#63-场景-3用户拒绝授权异常路径)、[§6.1 场景 1](./user-story.md#61-场景-1首次微信授权登录成功跳转补充资料页正常路径)、[§6.2 场景 2](./user-story.md#62-场景-2已注册用户微信授权登录成功跳转首页正常路径)

- [ ] **Step 1: 写失败测试**

```typescript
// miniapp-user/src/pages/login/index.test.tsx
import { render, fireEvent, waitFor } from '@testing-library/react';
import Taro from '@tarojs/taro';
import LoginPage from './index';

jest.mock('@tarojs/taro');

describe('LoginPage', () => {
  it('场景 3: 用户拒绝授权展示提示文案', async () => {
    (Taro.login as jest.Mock).mockRejectedValue({ errMsg: 'login:fail auth deny' });

    const { findByText, getByText } = render(<LoginPage />);
    fireEvent.click(getByText('微信一键登录'));

    expect(await findByText('需要微信授权才能登录')).toBeInTheDocument();
  });

  it('场景 1: 首次登录成功跳转补充资料页', async () => {
    (Taro.login as jest.Mock).mockResolvedValue({ code: 'mock_code' });
    (Taro.request as jest.Mock).mockResolvedValue({
      statusCode: 200,
      data: { accessToken: 'xxx', isNewUser: true, profileCompleted: false },
    });

    const { getByText } = render(<LoginPage />);
    fireEvent.click(getByText('微信一键登录'));

    await waitFor(() => {
      expect(Taro.navigateTo).toHaveBeenCalledWith({
        url: '/pages/profile-setup/index'  // US-005 补充资料页
      });
    });
  });

  it('场景 2: 老用户登录成功跳转首页', async () => {
    (Taro.login as jest.Mock).mockResolvedValue({ code: 'mock_code' });
    (Taro.request as jest.Mock).mockResolvedValue({
      statusCode: 200,
      data: { accessToken: 'xxx', isNewUser: false, profileCompleted: true },
    });

    const { getByText } = render(<LoginPage />);
    fireEvent.click(getByText('微信一键登录'));

    await waitFor(() => {
      expect(Taro.switchTab).toHaveBeenCalledWith({
        url: '/pages/home/index'
      });
    });
  });

  it('场景 4: 微信接口错误展示错误文案', async () => {
    (Taro.login as jest.Mock).mockResolvedValue({ code: 'mock_code' });
    (Taro.request as jest.Mock).mockResolvedValue({
      statusCode: 502,
      data: { message: '微信服务暂时不可用，请稍后重试' },
    });

    const { getByText, findByText } = render(<LoginPage />);
    fireEvent.click(getByText('微信一键登录'));

    expect(await findByText('微信服务暂时不可用，请稍后重试')).toBeInTheDocument();
  });

  it('防重复点击: 登录中按钮 disabled', async () => {
    (Taro.login as jest.Mock).mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve({ code: 'mock' }), 500))
    );

    const { getByText } = render(<LoginPage />);
    const button = getByText('微信一键登录');
    fireEvent.click(button);
    expect(button).toBeDisabled();
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npm test -- login/index.test.tsx`
Expected: FAIL with `Cannot find module './index'`

- [ ] **Step 3: 写最小实现**

```tsx
// miniapp-user/src/pages/login/index.tsx
import { useState } from 'react';
import { View, Text, Button } from '@tarojs/components';
import Taro from '@tarojs/taro';

export default function LoginPage() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleLogin() {
    if (loading) return;  // 防重复点击
    setLoading(true);
    setError('');

    try {
      // 1. 调用 wx.login 获取 code
      const { code } = await Taro.login();
      if (!code) {
        setError('需要微信授权才能登录');
        return;
      }

      // 2. 提交到后端
      const res = await Taro.request({
        url: `${API_BASE}/api/v1/auth/wechat-login`,
        method: 'POST',
        data: { code },
      });

      if (res.statusCode === 200) {
        // 3. 保存 token
        Taro.setStorageSync('accessToken', res.data.accessToken);
        Taro.setStorageSync('refreshToken', res.data.refreshToken);

        // 4. 按业务标志跳转
        if (res.data.profileCompleted) {
          Taro.switchTab({ url: '/pages/home/index' });
        } else {
          Taro.navigateTo({ url: '/pages/profile-setup/index' });
        }
      } else {
        setError(res.data.message || '登录失败，请重试');
      }
    } catch (err) {
      // 场景 3: 拒绝授权
      if (err.errMsg?.includes('auth deny')) {
        setError('需要微信授权才能登录');
      } else {
        setError('网络异常，请重试');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <View>
      <View>欢迎来到乐游游泳</View>
      {error && <Text>{error}</Text>}
      <Button
        onClick={handleLogin}
        disabled={loading}
        loading={loading}
        style={{ backgroundColor: '#07C160', color: '#fff' }}
      >
        {loading ? '登录中...' : '微信一键登录'}
      </Button>
    </View>
  );
}
```

- [ ] **Step 4: 跑测试确认通过**

Run: `npm test -- login/index.test.tsx`
Expected: PASS（5 个测试全过）

- [ ] **Step 5: Commit**

```bash
git add miniapp-user/src/pages/login/
git commit -m "feat(miniapp): add wechat login page with error handling and route redirect"
```

---

## 3. 任务执行纪律

- **严格顺序**：Task 1 → Task 2 → Task 3 → Task 4 → Task 5
- **每步必须可见**：Step 1（写测试）→ Step 2（看失败）→ Step 3（写实现）→ Step 4（看通过）→ Step 5（commit）
- **不允许 placeholder**：任何 "TBD" / "TODO" / "实现 later" / "类似 Task N" = 立即返工
- **每个 Task 结束 = 1 次 commit**：禁止跨 Task 累积 commit
- **P0 必做 / P1 选做**：MVP 阶段 P0（Task 1-4）必做，P1（Task 5）视进度决定
- **GWT 覆盖**：每个 Task 头部必须明确「对应 GWT」场景编号
- **状态机验证**：Task 1 和 Task 2 必须显式断言 `identity_status='注册用户'`（游客→注册用户转换）

---

## 4. GWT 场景覆盖矩阵

| GWT 场景 | Task 1 | Task 2 | Task 3 | Task 4 | Task 5 |
|---------|--------|--------|--------|--------|--------|
| §6.1 首次登录（正常） | ✅ | ✅ | ✅ | ✅ | ✅ |
| §6.2 老用户登录（正常） | ✅ | ✅ | ✅ | ✅ | ✅ |
| §6.3 拒绝授权（异常） | — | — | — | — | ✅ |
| §6.4 微信接口失败（异常） | — | ✅ | — | ✅ | ✅ |
| §6.5 code 失效（异常） | — | ✅ | — | ✅ | — |

---

## 5. 上下游引用

- **上游需求**：[./user-story.md](./user-story.md)（GWT 业务级场景）
- **设计输入**：[./tech-design.md](./tech-design.md)（API/数据模型/状态机/微信 OAuth 流程）
- **Figma 设计交付物**：[./user-story.md](./user-story.md#13-figma-链接) §13-15（UI 状态截图 + 页面级设计决策 + 设计评审记录）
- **测试计划规范**：[../../spec/test-plan/README.md](../../spec/test-plan/README.md)

---

## 6. 变更日志

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-07-30 | Dev | 初版：5 个 Task 完整示范（双重角色：TDD 任务清单 + 测试计划）；覆盖 5 个 GWT 场景；显式验证状态机转换（游客→注册用户） |
