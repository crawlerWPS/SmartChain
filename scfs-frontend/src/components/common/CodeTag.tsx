import React from 'react';
import { Tag } from 'antd';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';

const colorByCode: Record<string, string> = {
  APPROVED: 'success', ENABLED: 'success', PASS: 'success', COMPLETED: 'success', LOW: 'success',
  PENDING: 'warning', SUBMITTED: 'processing', MID: 'warning', OPEN: 'warning',
  REJECTED: 'error', DISABLED: 'default', ABNORMAL: 'error', HIGH: 'error', EXTREME: 'error',
  DRAFT: 'default', CONFIRMED: 'processing', DISMISSED: 'default',
};

export const CodeTag: React.FC<{ type: string; code?: string | number | null; color?: string; fallbackLabel?: string }> = ({ type, code, color, fallbackLabel }) => {
  const dictionary = useCodeDictionary();
  const raw = code == null ? '' : String(code);
  const label = dictionary.label(type, code);
  return <Tag color={color || colorByCode[raw] || 'blue'}>{label === raw && fallbackLabel ? fallbackLabel : label}</Tag>;
};
