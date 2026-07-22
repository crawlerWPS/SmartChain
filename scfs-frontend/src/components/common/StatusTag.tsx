/**
 * StatusTag - 状态/风险等级自动配色标签
 */
import React from 'react';
import { Tag } from 'antd';
import {
  APPLICATION_STATUS_MAP,
  RISK_LEVEL_MAP,
} from '@/utils';
import { ApplicationStatus, RiskLevel } from '@/types';

export const ApplicationStatusTag: React.FC<{ status: ApplicationStatus }> = ({ status }) => {
  const map = APPLICATION_STATUS_MAP[status] || { label: status, color: 'default' };
  return <Tag color={map.color}>{map.label}</Tag>;
};

export const RiskLevelTag: React.FC<{ level: RiskLevel }> = ({ level }) => {
  const map = RISK_LEVEL_MAP[level] || { label: level, color: 'default' };
  return <Tag color={map.color}>{map.label}</Tag>;
};

export default ApplicationStatusTag;
