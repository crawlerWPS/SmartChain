/**
 * 工具函数 - 脱敏、日期、状态映射
 */
import dayjs from 'dayjs';
import { ApplicationStatus, RiskLevel, MaterialType, BusinessType } from '@/types';

/** USCC 脱敏（保留前 6 位 + 后 4 位） */
export function maskUscc(uscc?: string): string {
  if (!uscc || uscc.length < 12) return uscc || '';
  return `${uscc.slice(0, 6)}${'*'.repeat(uscc.length - 10)}${uscc.slice(-4)}`;
}

/** 手机号脱敏（前 3 + 后 4） */
export function maskPhone(phone?: string): string {
  if (!phone || phone.length < 8) return phone || '';
  return phone.replace(/(\d{3})\d+(\d{4})/, '$1****$2');
}

/** 法人姓名脱敏 */
export function maskName(name?: string): string {
  if (!name) return '';
  if (name.length <= 2) return name[0] + '*';
  return name[0] + '*'.repeat(name.length - 2) + name[name.length - 1];
}

/** 金额格式化（千分位） */
export function formatAmount(amount?: number): string {
  if (amount == null) return '-';
  return amount.toLocaleString('zh-CN', { style: 'currency', currency: 'CNY' });
}

/** 日期格式化 */
export function formatDate(date?: string | Date | null, format = 'YYYY-MM-DD HH:mm:ss'): string {
  if (!date) return '-';
  return dayjs(date).format(format);
}

/** 字节大小格式化 */
export function formatFileSize(bytes?: number): string {
  if (!bytes) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`;
}

/** 申请状态映射 */
export const APPLICATION_STATUS_MAP: Record<ApplicationStatus, { label: string; color: string }> = {
  [ApplicationStatus.DRAFT]: { label: '草稿', color: 'default' },
  [ApplicationStatus.SUBMITTED]: { label: '已提交', color: 'blue' },
  [ApplicationStatus.MATERIAL_REVIEW]: { label: '材料审核', color: 'processing' },
  [ApplicationStatus.MATERIAL_SUPPLEMENT]: { label: '材料补正', color: 'orange' },
  [ApplicationStatus.OCR_RECOGNIZING]: { label: 'OCR识别中', color: 'processing' },
  [ApplicationStatus.OCR_FAILED]: { label: 'OCR失败', color: 'red' },
  [ApplicationStatus.PREAUDIT]: { label: '材料预审', color: 'processing' },
  [ApplicationStatus.PREAUDIT_FAILED]: { label: '预审未通过', color: 'red' },
  [ApplicationStatus.PREAUDIT_PASSED]: { label: '预审通过', color: 'blue' },
  [ApplicationStatus.VERIFYING]: { label: '真实性核验', color: 'processing' },
  [ApplicationStatus.VERIFY_FAILED]: { label: '核验未通过', color: 'red' },
  [ApplicationStatus.VERIFY_PASSED]: { label: '核验通过', color: 'blue' },
  [ApplicationStatus.RISK_SCORING]: { label: '风险评分', color: 'processing' },
  [ApplicationStatus.APPROVED]: { label: '已通过', color: 'success' },
  [ApplicationStatus.REJECTED]: { label: '已驳回', color: 'red' },
};

/** 风险等级映射 */
export const RISK_LEVEL_MAP: Record<RiskLevel, { label: string; color: string }> = {
  [RiskLevel.LOW]: { label: '低风险', color: 'success' },
  [RiskLevel.MID]: { label: '中风险', color: 'warning' },
  [RiskLevel.HIGH]: { label: '高风险', color: 'error' },
};

/** 材料类型映射 */
export const MATERIAL_TYPE_MAP: Record<string, string> = {
  CONTRACT: '合同',
  ORDER: '订单',
  INVOICE: '发票',
  LOGISTICS_DOC: '物流单据',
  ACCEPTANCE_CERT: '验收凭证',
  PAYMENT_VOUCHER: '付款凭证',
  BUSINESS_LICENSE: '营业执照',
};

/** 业务类型映射 */
export const BUSINESS_TYPE_MAP: Record<string, string> = {
  ACCOUNTS_RECEIVABLE: '应收账款融资',
  ORDER_FINANCING: '订单融资',
  PREPAID_FINANCING: '预付款融资',
};

/** 是否可提交（DRAFT 状态） */
export function canSubmit(status: ApplicationStatus): boolean {
  return status === ApplicationStatus.DRAFT || status === ApplicationStatus.MATERIAL_SUPPLEMENT;
}

/** 是否可分配审核人（SUBMITTED 状态） */
export function canAssign(status: ApplicationStatus): boolean {
  return status === ApplicationStatus.SUBMITTED;
}

/** 是否可驳回 */
export function canReject(status: ApplicationStatus): boolean {
  return [
    ApplicationStatus.SUBMITTED,
    ApplicationStatus.MATERIAL_REVIEW,
    ApplicationStatus.PREAUDIT,
    ApplicationStatus.VERIFYING,
    ApplicationStatus.PENDING_DECISION,
  ].includes(status);
}

/** 是否可审批通过（核验完成） */
export function canApprove(status: ApplicationStatus): boolean {
  return [
    ApplicationStatus.VERIFY_PASSED,
    ApplicationStatus.RISK_SCORING,
    ApplicationStatus.PENDING_DECISION,
  ].includes(status);
}

/** 下载 Blob 文件 */
export function downloadBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
