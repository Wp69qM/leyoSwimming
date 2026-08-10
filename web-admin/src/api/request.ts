import axios, { type AxiosError, type AxiosResponse } from 'axios';
import { useAdminAuthStore } from '@/stores/adminAuth';
import { router } from '@/router';
import type { ApiResponse } from '@/types/api';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    const { code, message } = response.data;
    if (code !== 0) {
      if (code === 200001 || code === 200002) {
        const authStore = useAdminAuthStore();
        authStore.clearAuth();
        router.push('/login?expired=1');
      }
      return Promise.reject(new Error(message || '请求失败'));
    }
    return response.data;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const message = error.response?.data?.message || '网络异常，请稍后重试';
    return Promise.reject(new Error(message));
  }
);

export default request;
