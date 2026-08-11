import { getAccessToken } from '@/utils/auth';

export interface CodeDictionaryItem {
  id: number;
  codeType: string;
  codeKey: string;
  code: string;
  codeValue: string;
  sortOrder: number;
  status: number;
  description?: string;
}

export async function listCodeDictionaries(type?: string): Promise<CodeDictionaryItem[]> {
  const query = type ? `?type=${encodeURIComponent(type)}` : '';
  const token = getAccessToken();
  const response = await fetch(`/api/v1/code-dictionaries${query}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
  });
  const result = await response.json();
  if (!response.ok || result.code !== 0) {
    throw new Error(result.message || `码值加载失败 (${response.status})`);
  }
  return result.data || [];
}
