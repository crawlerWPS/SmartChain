/**
 * 供应链图谱 API - 对应后端 GraphController
 */
import { request } from '@umijs/max';
import type { PageQuery, PageResult, Enterprise, SupplyChainRelation, EnterpriseRole, EnterprisePositionAnalysis, AbnormalRelation } from '@/types';

export interface EnterpriseQuery extends PageQuery {}

export interface GraphNode {
  id: string;
  label: string;
  enterpriseId: number;
  role?: string;
  isCore?: boolean;
  data?: Enterprise;
}

export interface GraphEdge {
  source: string;
  target: string;
  label?: string;
  relationType: string;
  weight?: number;
  data?: SupplyChainRelation;
}

export interface GraphData {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

export interface RelationImportRow {
  rowNumber: number;
  buyerName: string;
  buyerUscc: string;
  sellerName: string;
  sellerUscc: string;
  relationType: string;
  amount?: number;
  transactionDate?: string;
  remark?: string;
}

export interface RelationImportResult {
  total: number;
  createdEnterprises: number;
  createdRelations: number;
  updatedRelations: number;
  skippedDuplicates: number;
  errors: string[];
}

export async function importRelations(rows: RelationImportRow[]): Promise<RelationImportResult> {
  return request('/graph/relations/import', { method: 'POST', data: rows });
}

export async function recalculateAnalysis(): Promise<{ enterpriseCount: number; calculatedCount: number; coreEnterpriseId?: number; message: string }> {
  return request('/graph/analysis/recalculate', { method: 'POST' });
}

/** IF-GRAPH-001 企业分页 */
export async function pageEnterprises(query: EnterpriseQuery): Promise<PageResult<Enterprise>> {
  return request('/graph/enterprises', { method: 'GET', params: query });
}

/** IF-GRAPH-002 企业详情 */
export async function getEnterprise(id: number): Promise<Enterprise> {
  return request(`/graph/enterprises/${id}`, { method: 'GET' });
}

/** IF-GRAPH-003 关系列表 */
export async function getRelations(enterpriseId: number, level?: number): Promise<SupplyChainRelation[]> {
  return request(`/graph/relations/${enterpriseId}`, { method: 'GET', params: { level } });
}

/** IF-GRAPH-004 完整图谱（nodes + edges） */
export async function getGraphData(enterpriseId: number, level = 2): Promise<GraphData> {
  return request(`/graph/relations/${enterpriseId}`, { method: 'GET', params: { level } });
}

/** IF-GRAPH-004b 全部企业图谱（不指定起点企业，返回全部节点和边） */
export async function getAllGraphData(): Promise<GraphData> {
  return request('/graph/full', { method: 'GET' });
}

/** IF-GRAPH-005 企业角色（单企业） */
export async function getEnterpriseRole(enterpriseId: number): Promise<EnterpriseRole> {
  return request(`/graph/enterprises/${enterpriseId}/role`, { method: 'GET' });
}

/** IF-GRAPH-005b 全部企业角色 */
export async function listEnterpriseRoles(): Promise<EnterpriseRole[]> {
  return request('/graph/roles', { method: 'GET' });
}

/** IF-GRAPH-006 位置分析（单企业） */
export async function getPositionAnalysis(enterpriseId: number): Promise<EnterprisePositionAnalysis> {
  return request(`/graph/enterprises/${enterpriseId}/position`, { method: 'GET' });
}

/** IF-GRAPH-006b 全部位置分析 */
export async function listPositionAnalyses(): Promise<EnterprisePositionAnalysis[]> {
  return request('/graph/positions', { method: 'GET' });
}

/** IF-GRAPH-007 异常列表 */
export async function listAbnormals(enterpriseId?: number): Promise<AbnormalRelation[]> {
  return request('/graph/abnormals', { method: 'GET', params: { enterpriseId } });
}

/** IF-GRAPH-008 解除异常 */
export async function resolveAbnormal(id: number) {
  return request(`/graph/abnormals/${id}/resolve`, { method: 'POST' });
}

/** IF-GRAPH-009 重新计算角色 */
export async function recalculateRole(enterpriseId: number) {
  return request(`/graph/enterprises/${enterpriseId}/role/recalculate`, { method: 'POST' });
}
