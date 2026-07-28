import { describe, it, expect } from 'vitest';
import {
  maskUscc,
  maskPhone,
  maskName,
  formatAmount,
  formatDate,
  formatFileSize,
  canSubmit,
  canAssign,
  canReject,
  canApprove,
  APPLICATION_STATUS_MAP,
  RISK_LEVEL_MAP,
} from '../index';
import { ApplicationStatus, RiskLevel } from '@/types';

describe('脱敏函数', () => {
  describe('maskUscc', () => {
    it('18 位 USCC 应保留前 6 + 后 4 位，中间用 * 填充', () => {
      const uscc = '91110000123456789X';
      const masked = maskUscc(uscc);
      expect(masked).toHaveLength(18);
      expect(masked.startsWith('911100')).toBe(true);
      expect(masked.endsWith('789X')).toBe(true);
      expect(masked).toContain('*');
    });

    it('小于 12 位的字符串应原样返回', () => {
      expect(maskUscc('12345')).toBe('12345');
    });

    it('空值应返回空字符串', () => {
      expect(maskUscc(undefined)).toBe('');
      expect(maskUscc('')).toBe('');
    });
  });

  describe('maskPhone', () => {
    it('11 位手机号应保留前 3 + 后 4 位', () => {
      const masked = maskPhone('13812345678');
      expect(masked).toBe('138****5678');
    });

    it('小于 8 位的号码应原样返回', () => {
      expect(maskPhone('12345')).toBe('12345');
    });

    it('空值应返回空字符串', () => {
      expect(maskPhone(undefined)).toBe('');
    });
  });

  describe('maskName', () => {
    it('2 字姓名应保留姓 + *', () => {
      expect(maskName('张三')).toBe('张*');
    });

    it('3 字姓名应保留首尾，中间用 *', () => {
      expect(maskName('张三丰')).toBe('张*丰');
    });

    it('4 字姓名应保留首尾，中间 2 个 *', () => {
      expect(maskName('欧阳明日')).toBe('欧**日');
    });

    it('空值应返回空字符串', () => {
      expect(maskName(undefined)).toBe('');
      expect(maskName('')).toBe('');
    });
  });
});

describe('格式化函数', () => {
  describe('formatAmount', () => {
    it('应格式化为人民币千分位', () => {
      const result = formatAmount(1234567.89);
      expect(result).toContain('1,234,567.89');
      expect(result).toContain('¥');
    });

    it('null/undefined 应返回 -', () => {
      expect(formatAmount(null)).toBe('-');
      expect(formatAmount(undefined)).toBe('-');
    });
  });

  describe('formatDate', () => {
    it('应按默认格式 YYYY-MM-DD HH:mm:ss 格式化', () => {
      const result = formatDate('2024-01-15T10:30:00');
      expect(result).toBe('2024-01-15 10:30:00');
    });

    it('应支持自定义格式', () => {
      const result = formatDate('2024-01-15T10:30:00', 'YYYY/MM/DD');
      expect(result).toBe('2024/01/15');
    });

    it('空值应返回 -', () => {
      expect(formatDate(null)).toBe('-');
      expect(formatDate(undefined)).toBe('-');
      expect(formatDate('')).toBe('-');
    });
  });

  describe('formatFileSize', () => {
    it('0 字节应返回 0 B', () => {
      expect(formatFileSize(0)).toBe('0 B');
    });

    it('1024 字节应返回 1.00 KB', () => {
      expect(formatFileSize(1024)).toBe('1.00 KB');
    });

    it('1048576 字节应返回 1.00 MB', () => {
      expect(formatFileSize(1048576)).toBe('1.00 MB');
    });

    it('空值应返回 0 B', () => {
      expect(formatFileSize(undefined)).toBe('0 B');
    });
  });
});

describe('状态映射', () => {
  it('APPLICATION_STATUS_MAP 应包含所有状态', () => {
    expect(APPLICATION_STATUS_MAP[ApplicationStatus.DRAFT].label).toBe('草稿');
    expect(APPLICATION_STATUS_MAP[ApplicationStatus.APPROVED].label).toBe('已通过');
    expect(APPLICATION_STATUS_MAP[ApplicationStatus.REJECTED].label).toBe('已驳回');
  });

  it('RISK_LEVEL_MAP 应包含所有风险等级', () => {
    expect(RISK_LEVEL_MAP[RiskLevel.LOW].label).toBe('低风险');
    expect(RISK_LEVEL_MAP[RiskLevel.MID].label).toBe('中风险');
    expect(RISK_LEVEL_MAP[RiskLevel.HIGH].label).toBe('高风险');
  });
});

describe('状态流转判断', () => {
  describe('canSubmit', () => {
    it('DRAFT 状态可提交', () => {
      expect(canSubmit(ApplicationStatus.DRAFT)).toBe(true);
    });

    it('MATERIAL_SUPPLEMENT 状态可提交', () => {
      expect(canSubmit(ApplicationStatus.MATERIAL_SUPPLEMENT)).toBe(true);
    });

    it('SUBMITTED 状态不可提交', () => {
      expect(canSubmit(ApplicationStatus.SUBMITTED)).toBe(false);
    });

    it('APPROVED 状态不可提交', () => {
      expect(canSubmit(ApplicationStatus.APPROVED)).toBe(false);
    });
  });

  describe('canAssign', () => {
    it('SUBMITTED 状态可分配审核人', () => {
      expect(canAssign(ApplicationStatus.SUBMITTED)).toBe(true);
    });

    it('DRAFT 状态不可分配', () => {
      expect(canAssign(ApplicationStatus.DRAFT)).toBe(false);
    });
  });

  describe('canReject', () => {
    it('SUBMITTED 状态可驳回', () => {
      expect(canReject(ApplicationStatus.SUBMITTED)).toBe(true);
    });

    it('MATERIAL_REVIEW 状态可驳回', () => {
      expect(canReject(ApplicationStatus.MATERIAL_REVIEW)).toBe(true);
    });

    it('APPROVED 状态不可驳回', () => {
      expect(canReject(ApplicationStatus.APPROVED)).toBe(false);
    });
  });

  describe('canApprove', () => {
    it('VERIFY_PASSED 状态可审批通过', () => {
      expect(canApprove(ApplicationStatus.VERIFY_PASSED)).toBe(true);
    });

    it('RISK_SCORING 状态可审批通过', () => {
      expect(canApprove(ApplicationStatus.RISK_SCORING)).toBe(true);
    });

    it('DRAFT 状态不可审批通过', () => {
      expect(canApprove(ApplicationStatus.DRAFT)).toBe(false);
    });
  });
});
