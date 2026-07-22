/**
 * 材料预审 API - 对应后端 PreAuditController
 */
import { request } from '@umijs/max';
import type {
  MaterialCompletenessResult,
  MaterialValidityResult,
  EnterpriseInfoConsistencyResult,
  EnterpriseInfoMismatchDetail,
  SupplementList,
} from '@/types';

/** IF-PA-001 完整性检查 */
export async function checkCompleteness(applicationId: number): Promise<MaterialCompletenessResult> {
  return request(`/preaudit/applications/${applicationId}/completeness`, { method: 'POST' });
}

/** IF-PA-002 有效性检查 */
export async function checkValidity(applicationId: number): Promise<MaterialValidityResult> {
  return request(`/preaudit/applications/${applicationId}/validity`, { method: 'POST' });
}

/** IF-PA-003 一致性检查 */
export async function checkConsistency(applicationId: number): Promise<EnterpriseInfoConsistencyResult> {
  return request(`/preaudit/applications/${applicationId}/consistency`, { method: 'POST' });
}

/** IF-PA-004 查询完整性结果 */
export async function getCompletenessResult(applicationId: number): Promise<MaterialCompletenessResult | null> {
  return request(`/preaudit/applications/${applicationId}/completeness`, { method: 'GET' });
}

/** IF-PA-005 查询有效性结果 */
export async function getValidityResult(applicationId: number): Promise<MaterialValidityResult | null> {
  return request(`/preaudit/applications/${applicationId}/validity`, { method: 'GET' });
}

/** IF-PA-006 查询一致性结果 */
export async function getConsistencyResult(applicationId: number): Promise<EnterpriseInfoConsistencyResult | null> {
  return request(`/preaudit/applications/${applicationId}/consistency`, { method: 'GET' });
}

/** IF-PA-007 一致性明细 */
export async function getMismatchDetails(resultId: number): Promise<EnterpriseInfoMismatchDetail[]> {
  return request(`/preaudit/consistency/${resultId}/mismatches`, { method: 'GET' });
}

/** IF-PA-008 生成补正清单 */
export async function generateSupplementList(applicationId: number, deadline: string): Promise<SupplementList> {
  return request(`/preaudit/applications/${applicationId}/supplement`, { method: 'POST', data: { deadline } });
}

/** IF-PA-009 查询补正清单 */
export async function getSupplementList(applicationId: number): Promise<SupplementList | null> {
  return request(`/preaudit/applications/${applicationId}/supplement`, { method: 'GET' });
}

/** IF-PA-010 标记补正完成 */
export async function completeSupplement(supplementId: number) {
  return request(`/preaudit/supplement/${supplementId}/complete`, { method: 'POST' });
}
