export interface BackendErrorBody {
  code?: number;
  message?: string;
  msg?: string;
  traceId?: string;
}

/** 从 Umi/Axios/后端 Result 的各种错误形态中提取统一错误信息。 */
export function extractBackendError(error: any): BackendErrorBody {
  const body = error?.response?.data
    ?? error?.data
    ?? error?.info
    ?? (typeof error?.code === 'number' ? error : undefined);

  if (!body || typeof body !== 'object') {
    return {};
  }

  return {
    code: typeof body.code === 'number' ? body.code : undefined,
    message: body.message || body.msg,
    traceId: body.traceId,
  };
}

/** 保留后端 message，并为网络异常等没有 Result 响应的场景提供清晰兜底。 */
export function normalizeRequestError(error: any): Error & BackendErrorBody {
  const backend = extractBackendError(error);
  const status = error?.response?.status;
  let message = backend.message;

  if (!message) {
    if (status === 401) message = '登录已过期，请重新登录';
    else if (status === 403) message = '您没有访问此资源的权限';
    else if (status === 413) message = '文件大小超过 50MB 限制';
    else if (status && status >= 500) message = `服务器异常 (${status})，请稍后重试`;
    else if (!status) message = '网络异常，请检查连接';
    else message = error?.message || `请求失败 (${status})`;
  }

  const normalized = new Error(message) as Error & BackendErrorBody;
  normalized.code = backend.code;
  normalized.traceId = backend.traceId;
  return normalized;
}
