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

/** IF-GRAPH-005 企业角色 */
export async function getEnterpriseRole(enterpriseId: number): Promise<EnterpriseRole> {
  return request(`/graph/enterprises/${enterpriseId}/role`, { method: 'GET' });
}

/** IF-GRAPH-006 位置分析 */
export async function getPositionAnalysis(enterpriseId: number): Promise<EnterprisePositionAnalysis> {
  return request(`/graph/enterprises/${enterpriseId}/position`, { method: 'GET' });
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
