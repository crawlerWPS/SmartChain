/**
 * 真实性核验 + 报告 API - 对应后端 ApplicationController 核验子路径
 */
import { request } from '@umijs/max';
import type { VerifyCheckResult, VerifyReport } from '@/types';

/** IF-VER-001 完整性核验 */
export async function verifyCompleteness(applicationId: number): Promise<VerifyCheckResult> {
  return request(`/applications/${applicationId}/verify/completeness`, { method: 'POST' });
}

/** IF-VER-002 有效性核验 */
export async function verifyValidity(applicationId: number): Promise<VerifyCheckResult> {
  return request(`/applications/${applicationId}/verify/validity`, { method: 'POST' });
}

/** IF-VER-003 一致性核验 */
export async function verifyConsistency(applicationId: number): Promise<VerifyCheckResult> {
  return request(`/applications/${applicationId}/verify/consistency`, { method: 'POST' });
}

/** IF-VER-004 逻辑检查（Drools） */
export async function verifyLogicCheck(applicationId: number): Promise<VerifyCheckResult> {
  return request(`/applications/${applicationId}/verify/logic-check`, { method: 'POST' });
}

/** IF-VER-005 全部核验 */
export async function verifyAll(applicationId: number): Promise<VerifyCheckResult[]> {
  return request(`/applications/${applicationId}/verify/all`, { method: 'POST' });
}

/** IF-VER-006 查询核验结果 */
export async function getCheckResults(applicationId: number): Promise<VerifyCheckResult[]> {
  return request(`/applications/${applicationId}/verify/results`, { method: 'GET' });
}

/** IF-VER-007 生成核验报告 */
export async function generateReport(applicationId: number): Promise<VerifyReport> {
  return request(`/applications/${applicationId}/report/generate`, { method: 'POST' });
}

/** IF-VER-008 查询报告 */
export async function getReport(applicationId: number): Promise<VerifyReport> {
  return request(`/applications/${applicationId}/report`, { method: 'GET' });
}

/** IF-VER-009 按 reportNo 查询 */
export async function getReportByNo(reportNo: string): Promise<VerifyReport> {
  return request(`/reports/${reportNo}`, { method: 'GET' });
}

/** IF-VER-010 导出报告 PDF */
export async function exportReportPdf(reportNo: string): Promise<Blob> {
  return request(`/reports/${reportNo}/export-pdf`, { method: 'GET', responseType: 'blob' });
}
