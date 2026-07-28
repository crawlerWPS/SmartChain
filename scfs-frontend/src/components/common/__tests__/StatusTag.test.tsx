import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { ApplicationStatusTag, RiskLevelTag } from '../StatusTag';
import { ApplicationStatus, RiskLevel } from '@/types';

describe('ApplicationStatusTag', () => {
  it('DRAFT 状态应渲染"草稿"标签', () => {
    render(<ApplicationStatusTag status={ApplicationStatus.DRAFT} />);
    expect(screen.getByText('草稿')).toBeInTheDocument();
  });

  it('APPROVED 状态应渲染"已通过"标签', () => {
    render(<ApplicationStatusTag status={ApplicationStatus.APPROVED} />);
    expect(screen.getByText('已通过')).toBeInTheDocument();
  });

  it('REJECTED 状态应渲染"已驳回"标签', () => {
    render(<ApplicationStatusTag status={ApplicationStatus.REJECTED} />);
    expect(screen.getByText('已驳回')).toBeInTheDocument();
  });

  it('VERIFYING 状态应渲染"真实性核验"标签', () => {
    render(<ApplicationStatusTag status={ApplicationStatus.VERIFYING} />);
    expect(screen.getByText('真实性核验')).toBeInTheDocument();
  });
});

describe('RiskLevelTag', () => {
  it('LOW 风险应渲染"低风险"标签', () => {
    render(<RiskLevelTag level={RiskLevel.LOW} />);
    expect(screen.getByText('低风险')).toBeInTheDocument();
  });

  it('MID 风险应渲染"中风险"标签', () => {
    render(<RiskLevelTag level={RiskLevel.MID} />);
    expect(screen.getByText('中风险')).toBeInTheDocument();
  });

  it('HIGH 风险应渲染"高风险"标签', () => {
    render(<RiskLevelTag level={RiskLevel.HIGH} />);
    expect(screen.getByText('高风险')).toBeInTheDocument();
  });
});
