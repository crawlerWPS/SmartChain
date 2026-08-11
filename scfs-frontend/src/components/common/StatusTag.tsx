/**
 * StatusTag - 状态/风险等级自动配色标签
 */
import React from 'react';
import { ApplicationStatus, RiskLevel } from '@/types';
import { CodeTag } from './CodeTag';
import { APPLICATION_STATUS_MAP, RISK_LEVEL_MAP } from '@/utils';

export const ApplicationStatusTag: React.FC<{ status: ApplicationStatus }> = ({ status }) => {
  return <CodeTag type="APPLICATION_STATUS" code={status} fallbackLabel={APPLICATION_STATUS_MAP[status]?.label} />;
};

export const RiskLevelTag: React.FC<{ level: RiskLevel }> = ({ level }) => {
  return <CodeTag type="RISK_LEVEL" code={level} fallbackLabel={RISK_LEVEL_MAP[level]?.label} />;
};

export default ApplicationStatusTag;
