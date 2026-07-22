/**
 * 文件 API - 对应后端 FileController + MinIO
 */
import { request } from '@umijs/max';
import type { FileObject } from '@/types';

/** IF-FILE-001 上传文件（multipart） */
export async function uploadFile(
  file: File,
  onProgress?: (percent: number) => void
): Promise<FileObject> {
  const formData = new FormData();
  formData.append('file', file);
  return request('/files/upload', {
    method: 'POST',
    data: formData,
    requestType: 'form',
    onUploadProgress: (e) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded / e.total) * 100));
      }
    },
  });
}

/** IF-FILE-002 获取文件信息 */
export async function getFile(fileObjectId: number): Promise<FileObject> {
  return request(`/files/${fileObjectId}`, { method: 'GET' });
}

/** IF-FILE-003 预签名下载链接 */
export async function getDownloadUrl(fileObjectId: number): Promise<{ url: string; expireIn: number }> {
  return request(`/files/${fileObjectId}/download-url`, { method: 'GET' });
}

/** IF-FILE-004 预签名预览链接 */
export async function getPreviewUrl(fileObjectId: number): Promise<{ url: string; expireIn: number }> {
  return request(`/files/${fileObjectId}/preview-url`, { method: 'GET' });
}

/** 文件类型白名单校验（前端拦截） */
export const ALLOWED_EXTENSIONS = ['pdf', 'jpg', 'jpeg', 'png', 'docx', 'xlsx'];
export const MAX_FILE_SIZE_MB = 50;

export function isFileAllowed(file: File): { ok: boolean; reason?: string } {
  const ext = file.name.split('.').pop()?.toLowerCase() || '';
  if (!ALLOWED_EXTENSIONS.includes(ext)) {
    return { ok: false, reason: `不支持的文件类型: ${ext}，仅允许 ${ALLOWED_EXTENSIONS.join(', ')}` };
  }
  if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
    return { ok: false, reason: `文件大小超过 ${MAX_FILE_SIZE_MB}MB 限制` };
  }
  return { ok: true };
}
