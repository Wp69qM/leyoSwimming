export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T | null;
}

export interface AdminInfo {
  id: number;
  username: string;
  name: string;
  role: string;
}

export interface AdminLoginResponse {
  token: string;
  expiresIn: number;
  admin: AdminInfo;
}

export interface AdminLoginRequest {
  username: string;
  password: string;
}
