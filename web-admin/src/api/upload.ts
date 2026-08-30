import request from './request';

export interface UploadFileResponse {
  url: string;
}

export async function uploadFile(file: File): Promise<string> {
  const formData = new FormData();
  formData.append('file', file);

  const res = await request.post<FormData, { data: string }>(
    '/common/file/upload',
    formData
  );
  return res.data;
}
