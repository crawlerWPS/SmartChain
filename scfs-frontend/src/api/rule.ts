/**
 * 规则配置 API - 对应后端 RuleController
 */
import { request } from '@umijs/max';
import type { PageQuery, PageResult, RuleDefinition, RuleChangeLog } from '@/types';

export interface RuleQuery extends PageQuery {
  category?: string;
  status?: number;
}

/** IF-RULE-001 规则分页 */
export async function pageRules(query: RuleQuery): Promise<PageResult<RuleDefinition>> {
  return request('/rules', { method: 'GET', params: query });
}

/** IF-RULE-002 规则详情 */
export async function getRule(id: number): Promise<RuleDefinition> {
  return request(`/rules/${id}`, { method: 'GET' });
}

/** IF-RULE-003 创建规则 */
export async function createRule(data: {
  ruleCode: string;
  ruleName: string;
  category: string;
  drlContent: string;
  params?: Record<string, any>;
}) {
  return request('/rules', { method: 'POST', data });
}

/** IF-RULE-004 修改规则 */
export async function updateRule(id: number, data: Partial<{
  ruleName: string;
  drlContent: string;
  params: Record<string, any>;
}>) {
  return request(`/rules/${id}`, { method: 'PUT', data });
}

/** IF-RULE-005 提交规则变更 */
export async function submitRuleChange(id: number, changeType: string, data: { drlContent?: string; params?: Record<string, any>; remark?: string }) {
  return request(`/rules/${id}/submit`, { method: 'POST', params: { changeType }, data });
}

/** IF-RULE-006 审批通过 */
export async function approveRuleChange(changeLogId: number, remark?: string) {
  return request(`/rules/changes/${changeLogId}/approve`, { method: 'POST', data: { remark } });
}

/** IF-RULE-007 驳回 */
export async function rejectRuleChange(changeLogId: number, reason: string) {
  return request(`/rules/changes/${changeLogId}/reject`, { method: 'POST', data: { reason } });
}

/** IF-RULE-008 待审批变更分页 */
export async function pagePendingChanges(query: PageQuery): Promise<PageResult<RuleChangeLog>> {
  return request('/rules/changes/pending', { method: 'GET', params: query });
}

/** IF-RULE-009 规则变更历史 */
export async function listRuleChangeLogs(ruleId: number): Promise<RuleChangeLog[]> {
  return request(`/rules/${ruleId}/changes`, { method: 'GET' });
}

/** IF-RULE-010 启用/禁用规则 */
export async function toggleRuleStatus(id: number, status: number) {
  return request(`/rules/${id}/status`, { method: 'PATCH', params: { status } });
}
