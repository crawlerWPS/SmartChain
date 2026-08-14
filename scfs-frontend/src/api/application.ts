/**
 * 融资申请 + 材料 API - 对应后端 ApplicationController
 */
import { request } from '@umijs/max';
import type {
  PageQuery,
  PageResult,
  FinancingApplication,
  ApplicationMaterial,
  MaterialRecognitionResult,
  ApplicationStatusHistory,
} from '@/types';
import type { OcrTemplate } from './ocrTemplate';

export interface ApplicationQuery extends PageQuery {
  status?: string;
  submittedBy?: number;
  enterpriseId?: number;
}

export interface ApplicationCreate {
  buyerEnterpriseId: number;
  sellerEnterpriseId: number;
  businessType: string;
  financingAmount: number;
}

/** IF-APP-001 申请分页 */
export async function pageApplications(query: ApplicationQuery): Promise<PageResult<FinancingApplication>> {
  return request('/applications', { method: 'GET', params: query });
}

/** IF-APP-002 申请详情 */
export async function getApplication(id: number): Promise<FinancingApplication> {
  return request(`/applications/${id}`, { method: 'GET' });
}

/** IF-APP-003 创建草稿 */
export interface ApplicationCustomer {
  enterpriseId: number;
  name: string;
  uscc: string;
  industry?: string;
  legalPerson?: string;
  address?: string;
}

export async function listApplicationCustomers(keyword?: string, buyerOnly = false): Promise<ApplicationCustomer[]> {
  return request('/applications/customers', { method: 'GET', params: { keyword, buyerOnly } });
}

export async function listApplicationSellers(buyerEnterpriseId: number, keyword?: string): Promise<ApplicationCustomer[]> {
  return request('/applications/customers/sellers', { method: 'GET', params: { buyerEnterpriseId, keyword } });
}

export async function createApplicationCustomer(data: Omit<ApplicationCustomer, 'enterpriseId'>): Promise<number> {
  return request('/applications/customers', { method: 'POST', data });
}

export async function maintainTradeRelation(buyerEnterpriseId: number, sellerEnterpriseId: number) {
  return request('/applications/trade-relations', { method: 'POST', data: { buyerEnterpriseId, sellerEnterpriseId } });
}

export async function createApplication(data: ApplicationCreate): Promise<number> {
  return request('/applications', { method: 'POST', data });
}

/** IF-APP-004 更新草稿 */
export async function updateApplication(id: number, data: Partial<ApplicationCreate>): Promise<FinancingApplication> {
  return request(`/applications/${id}`, { method: 'PUT', data });
}

/** IF-APP-005 提交申请 */
export async function submitApplication(id: number, remark?: string) {
  return request(`/applications/${id}/submit`, { method: 'POST', data: { remark } });
}

/** IF-APP-007 驳回 */
export async function rejectApplication(id: number, reason: string) {
  return request(`/applications/${id}/reject`, { method: 'POST', data: { remark: reason } });
}

/** IF-APP-008 通过 */
export async function approveApplication(id: number, remark: string) {
  return request(`/applications/${id}/approve`, { method: 'POST', data: { remark } });
}

/** 风控无法判断时升级运营主管 */
export async function escalateApplicationToOps(id: number, supervisorId: number, remark: string) {
  return request(`/applications/${id}/escalate-to-ops`, {
    method: 'POST',
    data: { supervisorId, remark },
  });
}

/** IF-APP-009 状态历史 */
export async function getStatusHistory(id: number): Promise<ApplicationStatusHistory[]> {
  return request(`/applications/${id}/status-history`, { method: 'GET' });
}

export async function downloadMaterial(fileObjectId: number): Promise<Blob> {
  return request(`/files/${fileObjectId}/download`, { method: 'GET', responseType: 'blob' });
}

export async function previewMaterial(fileObjectId: number): Promise<Blob> {
  return request(`/files/${fileObjectId}/preview`, { method: 'GET', responseType: 'blob' });
}

// ========== 材料 ==========
/** IF-MAT-001 材料列表 */
export async function listMaterials(applicationId: number): Promise<ApplicationMaterial[]> {
  return request(`/applications/${applicationId}/materials`, { method: 'GET' });
}

/** IF-MAT-002 上传材料 */
export async function uploadMaterial(
  applicationId: number,
  file: File,
  materialType: string,
  ocrTemplateId: number,
  onProgress?: (percent: number) => void
) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('materialType', materialType);
  formData.append('ocrTemplateId', String(ocrTemplateId));
  return request(`/applications/${applicationId}/materials`, {
    method: 'POST',
    data: formData,
    requestType: 'form',
    onUploadProgress: (e: { total?: number; loaded: number }) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded / e.total) * 100));
      }
    },
  });
}

export async function listSelectableOcrTemplates(materialType: string): Promise<OcrTemplate[]> {
  return request('/applications/materials/ocr-templates', { method: 'GET', params: { materialType } });
}

/** IF-MAT-003 手动指定材料类型 */
export async function updateMaterialType(materialId: number, materialType: string) {
  return request(`/applications/materials/${materialId}/type`, { method: 'PUT', data: { materialType } });
}

/** IF-MAT-004 重新识别 */
export async function reRecognizeMaterial(materialId: number) {
  return request(`/applications/materials/${materialId}/re-recognize`, { method: 'POST' });
}

/** IF-MAT-005 获取识别结果 */
export async function getRecognitionResult(materialId: number): Promise<MaterialRecognitionResult> {
  return request(`/applications/materials/${materialId}/recognition`, { method: 'GET' });
}

/** IF-MAT-006 修正识别结果 */
export async function updateRecognitionResult(materialId: number, data: Partial<MaterialRecognitionResult>) {
  return request(`/applications/materials/${materialId}/recognition`, { method: 'PUT', data });
}

/** IF-MAT-007 删除材料 */
export async function deleteMaterial(materialId: number) {
  return request(`/applications/materials/${materialId}`, { method: 'DELETE' });
}
