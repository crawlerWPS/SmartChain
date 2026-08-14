/**
 * 风险画像 API - 对应后端 RiskController
 */
import { request } from '@umijs/max';
import type { PageResult, RiskProfile, TransactionStability, RiskWeightConfig, MaterialChecklistTemplate } from '@/types';

/** IF-RISK-001 风险评分 */
export async function calculateRiskScore(applicationId: number): Promise<RiskProfile> {
  return request(`/applications/${applicationId}/risk/calculate`, { method: 'POST' });
}

/** IF-RISK-002 查询画像 */
export async function getRiskProfile(id: number): Promise<RiskProfile> {
  return request(`/applications/${id}/risk`, { method: 'GET' });
}

/** IF-RISK-003 按申请查询画像 */
export async function getRiskProfileByApplication(applicationId: number): Promise<RiskProfile | null> {
  return request(`/applications/${applicationId}/risk`, { method: 'GET' });
}

/** IF-RISK-004 企业历史画像 */
export async function listRiskProfilesByEnterprise(enterpriseId: number): Promise<RiskProfile[]> {
  return request(`/risk/enterprises/${enterpriseId}/profiles`, { method: 'GET' });
}

/** IF-RISK-005 风险权重列表 */
export async function listWeightConfigs(): Promise<PageResult<RiskWeightConfig>> {
  return request('/weights', { method: 'GET' });
}

/** IF-RISK-006 当前生效权重 */
export async function getEnabledWeightConfig(): Promise<RiskWeightConfig | null> {
  return request('/weights/enabled', { method: 'GET' });
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
  return request('/weights', { method: 'POST', data });
}

/** IF-RISK-008 提交权重审核 */
export async function submitWeightConfig(id: number) {
  return request(`/weights/${id}/review`, { method: 'POST', data: { approved: false } });
}

/** IF-RISK-009 审批通过权重 */
export async function approveWeightConfig(id: number, remark?: string) {
  return request(`/weights/${id}/review`, { method: 'POST', data: { approved: true, remark } });
}

/** IF-RISK-010 驳回权重 */
export async function rejectWeightConfig(id: number, reason: string) {
  return request(`/weights/${id}/review`, { method: 'POST', data: { approved: false, rejectReason: reason } });
}

/** IF-RISK-011 交易稳定性 */
export async function getTransactionStability(enterpriseId: number): Promise<TransactionStability> {
  return request(`/risk/enterprises/${enterpriseId}/stability`, { method: 'GET' });
}

// ========== 材料模板 ==========
/** IF-TPL-001 模板列表 */
export async function listTemplates(): Promise<MaterialChecklistTemplate[]> {
  return request('/templates', { method: 'GET' });
}

/** IF-TPL-002 创建并启用模板 */
export async function createTemplate(data: { businessType: string; requiredMaterials: string[] }) {
  return request('/templates', { method: 'POST', data });
}

export async function updateTemplate(id: number, data: { businessType: string; requiredMaterials: string[] }) {
  return request(`/templates/${id}`, { method: 'PUT', data });
}

export async function deleteTemplate(id: number) {
  return request(`/templates/${id}`, { method: 'DELETE' });
}
