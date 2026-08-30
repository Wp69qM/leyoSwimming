/**
 * 业务类型定义占位文件。
 * 后续通过 `openapi-typescript` 从 `openapi/leyo-swimming-v1.yaml` 生成真实类型。
 */

export interface UserProfile {
  id: string;
  nickname: string;
  avatarUrl?: string;
  phone?: string;
}

export interface CoachProfile {
  id: string;
  name: string;
  avatarUrl?: string;
  status: number;
  referencePrice?: number;
}
