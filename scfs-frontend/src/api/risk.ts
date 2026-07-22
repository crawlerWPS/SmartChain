/**
 * 风险画像 API - 对应后端 RiskController
 */
import { request } from '@umijs/max';
import type { RiskProfile, TransactionStability, RiskWeightConfig, MaterialChecklistTemplate } from '@/types';

/** IF-RISK-001 风险评分 */
export async function calculateRiskScore(applicationId: number): Promise<RiskProfile> {
  return request(`/risk/applications/${applicationId}/score`, { method: 'POST' });
}

/** IF-RISK-002 查询画像 */
export async function getRiskProfile(id: number): Promise<RiskProfile> {
  return request(`/risk/profiles/${id}`, { method: 'GET' });
}

/** IF-RISK-003 按申请查询画像 */
export async function getRiskProfileByApplication(applicationId: number): Promise<RiskProfile | null> {
  return request(`/risk/applications/${applicationId}/profile`, { method: 'GET' });
}

/** IF-RISK-004 企业历史画像 */
export async function listRiskProfilesByEnterprise(enterpriseId: number): Promise<RiskProfile[]> {
  return request(`/risk/enterprises/${enterpriseId}/profiles`, { method: 'GET' });
}

/** IF-RISK-005 风险权重列表 */
export async function listWeightConfigs(): Promise<RiskWeightConfig[]> {
  return request('/risk/weights', { method: 'GET' });
}

/** IF-RISK-006 当前生效权重 */
export async function getEnabledWeightConfig(): Promise<RiskWeightConfig | null> {
  return request('/risk/weights/enabled', { method: 'GET' });
}

/** IF-RISK-007 创建权重草稿 */
export async function createWeightConfig(data: {
  configName: string;
  supplyChainWeight: number;
  transactionWeight: number;
  materialWeight: number;
  lowRiskThreshold: number;
  midRiskThreshold: number;
  highRiskThreshold: number;
}) {
  return request('/risk/weights', { method: 'POST', data });
}

/** IF-RISK-008 提交权重审核 */
export async function submitWeightConfig(id: number) {
  return request(`/risk/weights/${id}/submit`, { method: 'POST' });
}

/** IF-RISK-009 审批通过权重 */
export async function approveWeightConfig(id: number, remark?: string) {
  return request(`/risk/weights/${id}/approve`, { method: 'POST', data: { remark } });
}

/** IF-RISK-010 驳回权重 */
export async function rejectWeightConfig(id: number, reason: string) {
  return request(`/risk/weights/${id}/reject`, { method: 'POST', data: { reason } });
}

/** IF-RISK-011 交易稳定性 */
export async function getTransactionStability(enterpriseId: number): Promise<TransactionStability> {
  return request(`/risk/enterprises/${enterpriseId}/stability`, { method: 'GET' });
}

// ========== 材料模板 ==========
/** IF-TPL-001 模板列表 */
export async function listTemplates(): Promise<MaterialChecklistTemplate[]> {
  return request('/risk/templates', { method: 'GET' });
}

/** IF-TPL-002 创建模板草稿 */
export async function createTemplate(data: { businessType: string; requiredMaterials: string[] }) {
  return request('/risk/templates', { method: 'POST', data });
}

/** IF-TPL-003 提交模板 */
export async function submitTemplate(id: number) {
  return request(`/risk/templates/${id}/submit`, { method: 'POST' });
}

/** IF-TPL-004 审批模板 */
export async function approveTemplate(id: number, remark?: string) {
  return request(`/risk/templates/${id}/approve`, { method: 'POST', data: { remark } });
}

/** IF-TPL-005 驳回模板 */
export async function rejectTemplate(id: number, reason: string) {
  return request(`/risk/templates/${id}/reject`, { method: 'POST', data: { reason } });
}
