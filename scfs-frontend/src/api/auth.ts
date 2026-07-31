/**
 * 认证 API - 对应后端 AuthController
 */
import { request } from '@umijs/max';

export interface LoginParams {
  username: string;
  password: string;
  captcha?: string;
}

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  userInfo: {
    userId: number;
    username: string;
    realName: string;
    roleCode: string;
    permissions: Record<string, string[]>;
  };
}

/** IF-AUTH-001 登录 */
export async function login(params: LoginParams): Promise<LoginResult> {
  return request('/auth/login', { method: 'POST', data: params });
}

/** IF-AUTH-002 刷新令牌 */
export async function refreshToken(refreshToken: string): Promise<{ accessToken: string; expiresIn: number }> {
  return request('/auth/refresh', { method: 'POST', data: { refreshToken } });
}

/** IF-AUTH-003 登出 */
export async function logout(): Promise<void> {
  return request('/auth/logout', { method: 'POST' });
}

/** IF-AUTH-004 获取当前用户 */
export async function getCurrentUser() {
  return request<LoginResult['user']>('/auth/me', { method: 'GET' });
}
