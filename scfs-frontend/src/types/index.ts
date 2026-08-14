/**
 * 全局 TS 类型 - 对应后端 5 个 Schema 全部数据表 + 业务枚举
 */

// 通用响应
export interface Result<T> {
  code: number;
  msg: string;
  data: T;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}

export interface PageQuery {
  page?: number;
  size?: number;
  sortBy?: string;
  sortOrder?: 'ASC' | 'DESC';
  keyword?: string;
}

// ========== 通用实体基类 ==========
export interface BaseEntity {
  id: number;
  createdAt: string;
  updatedAt?: string;
}

// ========== schema_common ==========
export interface SysUser extends BaseEntity {
  username: string;
  realName: string;
  roleCode: string;
  email?: string;
  phone?: string;
  status: number;
}

export interface SysRole {
  id: number;
  roleCode: string;
  roleName: string;
  roleType: string;
  description?: string;
  status: number;
  createdAt: string;
}

export interface SysRolePermission extends BaseEntity {
  roleId: number;
  module: string;
  permissions: string[];
}

export interface SysMenu extends BaseEntity {
  parentId: number | null;
  menuName: string;
  menuCode: string;
  menuType: 'DIRECTORY' | 'MENU' | 'BUTTON';
  path?: string;
  component?: string;
  permission?: string;
  icon?: string;
  sort: number;
  visible: number;
  status: number;
}

export interface SysAuditLog {
  id: number;
  userId: number;
  username: string;
  module: string;
  action: string;
  targetType?: string;
  targetId?: string;
  detail?: Record<string, any>;
  ipAddress?: string;
  createdAt: string;
}

export interface FileObject {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  minioBucket: string;
  minioObjectKey: string;
  contentHash?: string;
  uploadedBy: number;
  createdAt: string;
}

export interface RuleDefinition extends BaseEntity {
  ruleCode: string;
  ruleName: string;
  category: string;
  drlContent: string;
  params?: Record<string, any>;
  status: number;
  version: number;
  createdBy: number;
}

export interface RuleChangeLog {
  id: number;
  ruleId: number;
  ruleCode: string;
  changeType: 'CREATE' | 'UPDATE' | 'DELETE';
  oldVersion?: number;
  newVersion?: number;
  oldContent?: string;
  newContent?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  makerId: number;
  checkerId?: number;
  checkedAt?: string;
  rejectReason?: string;
  createdAt: string;
}

export interface RiskWeightConfig {
  id: number;
  configName: string;
  supplyChainWeight: number;
  transactionWeight: number;
  materialWeight: number;
  lowRiskThreshold: number;
  midRiskThreshold: number;
  highRiskThreshold: number;
  status: 'PENDING' | 'REJECTED' | 'ENABLED' | 'DISABLED';
  version: number;
  makerId?: number;
  checkerId?: number;
  checkedAt?: string;
  rejectReason?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface MaterialChecklistTemplate {
  id: number;
  businessType: BusinessType;
  requiredMaterials: string[];
  version: number;
  status: 'DRAFT' | 'ENABLED' | 'DISABLED';
  makerId?: number;
  checkerId?: number;
  checkedAt?: string;
  rejectReason?: string;
  createdAt: string;
  updatedAt?: string;
}

// ========== schema_graph ==========
export interface Enterprise extends BaseEntity {
  name: string;
  uscc: string;
  industry?: string;
  legalPerson?: string;
  registeredCapital?: number;
  establishDate?: string;
  address?: string;
  dataSource?: string;
  lastSyncedAt?: string;
}

export interface SupplyChainRelation extends BaseEntity {
  fromEnterpriseId: number;
  toEnterpriseId: number;
  relationType: 'SUPPLY' | 'PURCHASE' | 'GUARANTEE';
  firstCoopDate?: string;
  lastCoopDate?: string;
  totalTransactions?: number;
  totalAmount?: number;
  coreEnterpriseId?: number;
  level: number;
}

export interface EnterpriseRole {
  id: number;
  enterpriseId: number;
  enterpriseName?: string;
  role: EnterpriseRoleEnum;
  coreEnterpriseId?: number;
  coopDurationYears?: number;
  coopEnterpriseCount?: number;
  influenceLevel?: string;
  credibilityLevel?: string;
  calculatedAt: string;
}

export interface EnterprisePositionAnalysis {
  id: number;
  enterpriseId: number;
  enterpriseName?: string;
  inCoreChain: boolean;
  distanceToCore: number;
  upstreamStable: boolean;
  downstreamStable: boolean;
  credibility: 'HIGH' | 'MID' | 'LOW';
  credibilityReason?: string;
  calculatedAt: string;
}

export interface AbnormalRelation {
  id: number;
  enterpriseId: number;
  enterpriseName?: string;
  abnormalType: AbnormalType;
  severity: 'INFO' | 'WARN' | 'DANGER';
  description: string;
  evidence?: Record<string, any>;
  status: 'OPEN' | 'RESOLVED';
  detectedAt: string;
  createdAt: string;
}

// ========== schema_verify ==========
export interface FinancingApplication extends BaseEntity {
  appNo: string;
  enterpriseId: number;
  buyerEnterpriseId: number;
  sellerEnterpriseId: number;
  buyerName?: string;
  sellerName?: string;
  businessType: BusinessType;
  financingAmount: number;
  submittedBy?: number;
  status: ApplicationStatus;
  currentHandler?: number;
  submittedAt?: string;
  approvedAt?: string;
  version: number;
}

export interface ApplicationStatusHistory {
  id: number;
  applicationId: number;
  fromStatus: ApplicationStatus | null;
  toStatus: ApplicationStatus;
  operatorId: number;
  remark?: string;
  createdAt: string;
}

export interface ApplicationMaterial extends BaseEntity {
  applicationId: number;
  fileObjectId: number;
  ocrTemplateId?: number;
  ocrTemplateCode?: string;
  ocrTemplateName?: string;
  fileName?: string;
  fileType?: string;
  fileSize?: number;
  materialType?: MaterialType;
  identifiedBy?: 'OCR' | 'MANUAL';
  confidence?: number;
  status: 'PENDING' | 'IDENTIFIED' | 'REJECTED';
}

export interface MaterialRecognitionResult {
  id: number;
  applicationMaterialId: number;
  buyerName?: string;
  buyerUscc?: string;
  sellerName?: string;
  sellerUscc?: string;
  commodity?: string;
  amount?: number;
  amountInWords?: string;
  contractDate?: string;
  orderDate?: string;
  invoiceDate?: string;
  logisticsDate?: string;
  acceptanceDate?: string;
  paymentDate?: string;
  contractPeriod?: string;
  paymentTerm?: string;
  transactionNo?: string;
  fieldConfidence?: Record<string, number>;
  rawOcrResult?: Record<string, any>;
  fieldPositions?: Record<string, any>;
  recognizedAt?: string;
}

export interface VerifyCheckResult {
  id: number;
  applicationId: number;
  checkType: CheckType;
    result: 'PASS' | 'ABNORMAL' | 'MISSING' | 'FAIL' | 'WARN';
  details?: Record<string, any>;
  executedRules?: string[];
  executedAt: string;
}

export interface VerifyReport {
  id: number;
  reportNo: string;
  applicationId: number;
  version: number;
  overallAssessment: string;
  abnormalCount: number;
  riskHints?: string[];
  contentSnapshot?: Record<string, any>;
  contentHash?: string;
  generatedAt: string;
}

// ========== schema_preaudit ==========
export interface MaterialCompletenessResult {
  id: number;
  applicationId: number;
  requiredCount: number;
  submittedCount: number;
  completenessPct: number;
  missingMaterials?: string[];
  checkedAt: string;
}

export interface MaterialValidityResult {
  id: number;
  applicationId: number;
  totalFiles: number;
  expiredCount: number;
  incompleteCount: number;
  abnormalCount: number;
  details?: {
    allValid?: boolean;
    abnormalItems?: MaterialValidityItem[];
    materialResults?: MaterialValidityItem[];
  };
  checkedAt: string;
}

export interface MaterialValidityItem {
  materialId: number;
  fileName?: string;
  materialType: string;
  recognitionStatus?: string;
  recognized: boolean;
  expired: boolean;
  missingFields: string[];
  issues: string[];
  valid: boolean;
}

export interface EnterpriseInfoConsistencyResult {
  id: number;
  applicationId: number;
  overallConsistent: boolean;
  nameConsistent: boolean;
  usccConsistent: boolean;
  legalPersonConsistent: boolean;
  addressConsistent: boolean;
  mismatchCount: number;
  checkedAt: string;
}

export interface EnterpriseInfoMismatchDetail {
  id: number;
  resultId: number;
  fieldType: 'NAME' | 'USCC' | 'LEGAL_PERSON' | 'ADDRESS';
  fieldName: string;
  consistent: boolean;
  sourceValues?: Array<{
    materialId?: number;
    source: string;
    context: string;
    value: string;
  }>;
  mismatchDetail?: string;
}

export interface SupplementList {
  id: number;
  applicationId: number;
  supplementItems?: string[];
  status: 'PENDING' | 'COMPLETED' | 'OVERDUE';
  deadline?: string;
  generatedAt: string;
  createdAt: string;
}

// ========== schema_risk ==========
export interface RiskProfile {
  id: number;
  applicationId: number;
  enterpriseId: number;
  version: number;
  supplyChainScore: number;
  transactionScore: number;
  materialScore: number;
  weightedConfigId?: number;
  overallScore: number;
  riskLevel: RiskLevel;
  riskReasons?: string[];
  suggestions?: string[];
  contentHash?: string;
  generatedAt: string;
  createdAt: string;
}

export interface TransactionStability {
  id: number;
  enterpriseId: number;
  score: number;
  transactionCount12m: number;
  amountStdDev?: number;
  trendData?: number[];
  calculatedAt: string;
}

// ========== 枚举 ==========
export enum BusinessType {
  ACCOUNTS_RECEIVABLE = 'ACCOUNTS_RECEIVABLE',
  ORDER_FINANCING = 'ORDER_FINANCING',
  PREPAID_FINANCING = 'PREPAID_FINANCING',
}

export enum ApplicationStatus {
  DRAFT = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  MATERIAL_REVIEW = 'MATERIAL_REVIEW',
  MATERIAL_SUPPLEMENT = 'MATERIAL_SUPPLEMENT',
  OCR_RECOGNIZING = 'OCR_RECOGNIZING',
  OCR_FAILED = 'OCR_FAILED',
  PREAUDIT = 'PREAUDIT',
  PREAUDIT_FAILED = 'PREAUDIT_FAILED',
  PREAUDIT_PASSED = 'PREAUDIT_PASSED',
  VERIFYING = 'VERIFYING',
  VERIFY_FAILED = 'VERIFY_FAILED',
  VERIFY_PASSED = 'VERIFY_PASSED',
  RISK_SCORING = 'RISK_SCORING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}

export enum MaterialType {
  CONTRACT = 'CONTRACT',
  ORDER = 'ORDER',
  INVOICE = 'INVOICE',
  LOGISTICS_DOC = 'LOGISTICS_DOC',
  ACCEPTANCE_CERT = 'ACCEPTANCE_CERT',
  PAYMENT_VOUCHER = 'PAYMENT_VOUCHER',
  BUSINESS_LICENSE = 'BUSINESS_LICENSE',
}

export enum CheckType {
  COMPLETENESS = 'COMPLETENESS',
  VALIDITY = 'VALIDITY',
  CONSISTENCY = 'CONSISTENCY',
  LOGIC_CHECK = 'LOGIC_CHECK',
  SUBJECT = 'SUBJECT',
  AMOUNT = 'AMOUNT',
  TIME = 'TIME',
  REPEAT = 'REPEAT',
}

export enum RiskLevel {
  LOW = 'LOW',
  MID = 'MID',
  HIGH = 'HIGH',
}

export enum EnterpriseRoleEnum {
  CORE = 'CORE',
  FIRST_TIER_SUPPLIER = 'FIRST_TIER_SUPPLIER',
  FIRST_TIER_BUYER = 'FIRST_TIER_BUYER',
  SECOND_TIER_NODE = 'SECOND_TIER_NODE',
  PERIPHERAL = 'PERIPHERAL',
}

export enum AbnormalType {
  SOLO_CYCLE = 'SOLO_CYCLE',
  MULTI_LEVEL_TRANSITIVE = 'MULTI_LEVEL_TRANSITIVE',
  FREQUENT_CHANGE = 'FREQUENT_CHANGE',
  CONCENTRATION_RISK = 'CONCENTRATION_RISK',
}

export enum DualControlStatus {
  DRAFT = 'DRAFT',
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}
