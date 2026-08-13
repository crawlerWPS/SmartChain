/**
 * 全局运行时入口 - JWT 拦截器、错误处理、用户初始化
 * 对应 RFC §3.1 统一响应与错误码
 */
import { history } from '@umijs/max';
import { message as antdMessage } from 'antd';
import type { RequestConfig, RunTimeLayoutConfig } from '@umijs/max';
import { getCurrentUser, setCurrentUser, type CurrentUser } from '@/access/access';
import { clearTokens, getAccessToken } from '@/utils/auth';
import { normalizeRequestError } from '@/utils/requestError';

/** 全局初始化 - 加载用户信息 */
export async function getInitialState(): Promise<{ currentUser?: CurrentUser }> {
  const token = getAccessToken();
  if (!token) {
    return {};
  }
  try {
    // 调用 /auth/me 获取用户信息（此处略，依赖 api 层）
    const userStr = localStorage.getItem('scfs_current_user');
    if (userStr) {
      const currentUser = JSON.parse(userStr) as CurrentUser;
      setCurrentUser(currentUser);
      return { currentUser };
    }
  } catch (e) {
    clearTokens();
  }
  return {};
}

/** ProLayout 布局配置 */
export const layout: RunTimeLayoutConfig = ({ initialState }) => {
  return {
    title: 'SCFS 供应链金融风控平台',
    logo: '/project-logo.png',
    siderWidth: 300,
    avatarProps: {
      title: initialState?.currentUser?.realName || '未登录',
      size: 'small',
    },
    menu: { locale: false },
    onMenuHeaderClick: () => history.push('/workspace'),
    logout: async () => {
      clearTokens();
      setCurrentUser(null);
      history.push('/login');
    },
    // 错误页跳转
    unAccessible: () => {
      history.push('/login');
    },
  };
};

/** 请求拦截器配置 */
export const request: RequestConfig = {
  baseURL: '/api/v1',
  timeout: 30000,
  requestInterceptors: [
    (config) => {
      const token = getAccessToken();
      if (token) {
        config.headers = config.headers || {};
        (config.headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
      }
      return config;
    },
  ],
  responseInterceptors: [
    (response) => {
      const data = response.data;
      // 统一处理后端 Result 包装
      if (data && typeof data === 'object' && 'code' in data) {
        if (data.code === 0) {
          // 成功
          return { ...response, data: data.data };
        }
        // 业务错误
        const msg = data.message || data.msg || `操作失败 [${data.code}]`;
        if (data.code === 1002 || data.code === 401) {
          // 未授权
          clearTokens();
          history.push('/login');
        } else if (data.code === 2001) {
          // OCR 异常 - 提示人工识别
          antdMessage.warning('OCR 识别失败，请人工指定材料类型');
        } else {
          antdMessage.error(msg);
        }
        return Promise.reject(normalizeRequestError({
          data: { code: data.code, message: msg, traceId: data.traceId },
        }));
      }
      // 文件流等直接返回
      return response;
    },
  ],
  errorConfig: {
    errorHandler(error: any, opts: any) {
      const normalized = normalizeRequestError(error);
      const status = error?.response?.status;
      const code = normalized.code;

      if (opts?.skipErrorHandler) {
        throw normalized;
      }

      if (status === 401) {
        clearTokens();
        history.push('/login');
      } else if (code === 1002) {
        clearTokens();
        history.push('/login');
      }

      // 必须继续抛出，页面 catch 才能展示后端 Result.message。
      throw normalized;
    },
  },
};
