import axios from 'axios';
import request from './request';
import type {
  ApiResponse,
  KnowledgeAddRequest,
  KnowledgeUploadRequest,
  KnowledgeListRequest,
  KnowledgeListResponse,
  KnowledgeToggleRequest,
  KnowledgeDeleteRequest,
  KnowledgeDetailRequest,
  KnowledgeDetail,
} from '@/types/api';

export function addKnowledge(
  data: KnowledgeAddRequest
): Promise<ApiResponse<null>> {
  return request.post('/admin/knowledge/add', data) as Promise<ApiResponse<null>>;
}

export function uploadKnowledgeFile(
  data: KnowledgeUploadRequest
): Promise<ApiResponse<null>> {
  const formData = new FormData();
  formData.append('title', data.title);
  formData.append('category', data.category);
  formData.append('file', data.file);
  return request.post('/admin/knowledge/upload', formData) as Promise<
    ApiResponse<null>
  >;
}

export function getKnowledgeList(
  data: KnowledgeListRequest
): Promise<ApiResponse<KnowledgeListResponse>> {
  return request.post('/admin/knowledge/list', data) as Promise<
    ApiResponse<KnowledgeListResponse>
  >;
}

export function toggleKnowledgeStatus(
  data: KnowledgeToggleRequest
): Promise<ApiResponse<null>> {
  return request.post('/admin/knowledge/toggle', data) as Promise<
    ApiResponse<null>
  >;
}

export function deleteKnowledge(
  data: KnowledgeDeleteRequest
): Promise<ApiResponse<null>> {
  return request.post('/admin/knowledge/delete', data) as Promise<
    ApiResponse<null>
  >;
}

export function getKnowledgeDetail(
  data: KnowledgeDetailRequest
): Promise<ApiResponse<KnowledgeDetail>> {
  return request.post('/admin/knowledge/detail', data) as Promise<
    ApiResponse<KnowledgeDetail>
  >;
}

export async function downloadKnowledgeFile(
  data: KnowledgeDetailRequest
): Promise<void> {
  const token = localStorage.getItem('admin_token');
  const response = await axios({
    method: 'post',
    url: '/api/admin/knowledge/download',
    data,
    responseType: 'blob',
    headers: {
      Authorization: token ? `Bearer ${token}` : '',
    },
  });

  const blob = new Blob([response.data]);
  const contentDisposition = response.headers['content-disposition'];
  let filename = 'document.txt';
  if (contentDisposition) {
    const match = contentDisposition.match(/filename\*?=['"]?UTF-8''([^;]+)/i);
    if (match && match[1]) {
      filename = decodeURIComponent(match[1]);
    } else {
      const filenameMatch = contentDisposition.match(/filename=['"]?([^'";]+)['"]?/i);
      if (filenameMatch && filenameMatch[1]) {
        filename = filenameMatch[1];
      }
    }
  }

  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}
