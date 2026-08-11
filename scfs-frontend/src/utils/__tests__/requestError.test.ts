import { describe, expect, it } from 'vitest';
import { extractBackendError, normalizeRequestError } from '../requestError';

describe('request error normalization', () => {
  it('extracts Result.message from an Axios HTTP error', () => {
    const error = {
      response: {
        status: 400,
        data: { code: 1001, message: '用户名或密码错误', traceId: 'trace-1' },
      },
    };

    expect(extractBackendError(error)).toEqual({
      code: 1001,
      message: '用户名或密码错误',
      traceId: 'trace-1',
    });
    expect(normalizeRequestError(error).message).toBe('用户名或密码错误');
  });

  it('supports legacy msg responses', () => {
    expect(normalizeRequestError({ data: { code: 1006, msg: '业务校验失败' } }).message)
      .toBe('业务校验失败');
  });

  it('keeps backend messages for 500 responses', () => {
    const error = { response: { status: 500, data: { code: 2001, message: 'OCR 服务异常' } } };
    expect(normalizeRequestError(error).message).toBe('OCR 服务异常');
  });

  it('provides a network fallback', () => {
    expect(normalizeRequestError(new Error('Failed to fetch')).message).toBe('网络异常，请检查连接');
  });
});
