/**
 * 审计日志 API - 对应后端 AuditLogController
 */
import { request } from '@umijs/max';
import type { PageQuery, PageResult, SysAuditLog } from '@/types';

export interface AuditLogQuery extends PageQuery {
  module?: string;
  action?: string;
  userId?: number;
  startTime?: string;
  endTime?: string;
}

/** IF-AUDIT-001 审计日志分页 */
export async function pageAuditLogs(query: AuditLogQuery): Promise<PageResult<SysAuditLog>> {
  return request('/audit-logs', { method: 'GET', params: query });
}

/** IF-AUDIT-002 审计日志详情 */
export async function getAuditLog(id: number): Promise<SysAuditLog> {
  return request(`/audit-logs/${id}`, { method: 'GET' });
}

/** IF-AUDIT-003 模块列表（下拉框） */
export async function listAuditModules(): Promise<string[]> {
  return request('/audit-logs/modules', { method: 'GET' });
}

/** IF-AUDIT-004 导出审计日志 */
export async function exportAuditLogs(query: AuditLogQuery): Promise<Blob> {
  return request('/audit-logs/export', { method: 'GET', params: query, responseType: 'blob' });
}
