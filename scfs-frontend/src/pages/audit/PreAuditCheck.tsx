/**
 * 预审补正页
 */
import React, { useState, useEffect } from 'react';
import { Card, Tabs, Button, Descriptions, Progress, Tag, message, List, Empty } from 'antd';
import { useSearchParams } from '@umijs/max';
import { checkCompleteness, checkValidity, checkConsistency, getCompletenessResult, getValidityResult, getConsistencyResult } from '@/api/preaudit';
import { formatDate } from '@/utils';

const PreAuditCheck: React.FC = () => {
  const [searchParams] = useSearchParams();
  const appId = Number(searchParams.get('appId') || 0);
  const [completeness, setCompleteness] = useState<any>(null);
  const [validity, setValidity] = useState<any>(null);
  const [consistency, setConsistency] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  const loadResults = async () => {
    try {
      const [c, v, con] = await Promise.all([
        getCompletenessResult(appId),
        getValidityResult(appId),
        getConsistencyResult(appId),
      ]);
      setCompleteness(c);
      setValidity(v);
      setConsistency(con);
    } catch (e: any) {
      // ignore
    }
  };

  useEffect(() => { loadResults(); }, [appId]);

  const handleCheck = async (type: 'completeness' | 'validity' | 'consistency') => {
    setLoading(true);
    try {
      if (type === 'completeness') {
        const r = await checkCompleteness(appId);
        setCompleteness(r);
        message.success('完整性检查完成');
      } else if (type === 'validity') {
        const r = await checkValidity(appId);
        setValidity(r);
        message.success('有效性检查完成');
      } else {
        const r = await checkConsistency(appId);
        setConsistency(r);
        message.success('一致性检查完成');
      }
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title={`材料预审 - 申请 #${appId}`}>
      <Tabs items={[
        {
          key: 'completeness',
          label: '完整性',
          children: (
            <div>
              <Button onClick={() => handleCheck('completeness')} loading={loading} style={{ marginBottom: 16 }}>执行检查</Button>
              {completeness ? (
                <Descriptions bordered>
                  <Descriptions.Item label="需提交">{completeness.requiredCount}</Descriptions.Item>
                  <Descriptions.Item label="已提交">{completeness.submittedCount}</Descriptions.Item>
                  <Descriptions.Item label="完整度" span={2}>
                    <Progress percent={completeness.completenessPct} status={completeness.completenessPct >= 100 ? 'success' : 'active'} />
                  </Descriptions.Item>
                  <Descriptions.Item label="缺失材料" span={2}>
                    {completeness.missingMaterials?.length ? completeness.missingMaterials.join(', ') : '无'}
                  </Descriptions.Item>
                </Descriptions>
              ) : <Empty description="尚未检查" />}
            </div>
          ),
        },
        {
          key: 'validity',
          label: '有效性',
          children: (
            <div>
              <Button onClick={() => handleCheck('validity')} loading={loading} style={{ marginBottom: 16 }}>执行检查</Button>
              {validity ? (
                <Descriptions bordered>
                  <Descriptions.Item label="总文件数">{validity.totalFiles}</Descriptions.Item>
                  <Descriptions.Item label="过期"><Tag color="red">{validity.expiredCount}</Tag></Descriptions.Item>
                  <Descriptions.Item label="不完整"><Tag color="orange">{validity.incompleteCount}</Tag></Descriptions.Item>
                  <Descriptions.Item label="异常"><Tag color="red">{validity.abnormalCount}</Tag></Descriptions.Item>
                </Descriptions>
              ) : <Empty description="尚未检查" />}
            </div>
          ),
        },
        {
          key: 'consistency',
          label: '一致性',
          children: (
            <div>
              <Button onClick={() => handleCheck('consistency')} loading={loading} style={{ marginBottom: 16 }}>执行检查</Button>
              {consistency ? (
                <Descriptions bordered>
                  <Descriptions.Item label="总体一致"><Tag color={consistency.overallConsistent ? 'green' : 'red'}>{consistency.overallConsistent ? '一致' : '不一致'}</Tag></Descriptions.Item>
                  <Descriptions.Item label="名称一致"><Tag color={consistency.nameConsistent ? 'green' : 'red'}>{consistency.nameConsistent ? '是' : '否'}</Tag></Descriptions.Item>
                  <Descriptions.Item label="USCC一致"><Tag color={consistency.usccConsistent ? 'green' : 'red'}>{consistency.usccConsistent ? '是' : '否'}</Tag></Descriptions.Item>
                  <Descriptions.Item label="法人一致"><Tag color={consistency.legalPersonConsistent ? 'green' : 'red'}>{consistency.legalPersonConsistent ? '是' : '否'}</Tag></Descriptions.Item>
                  <Descriptions.Item label="地址一致"><Tag color={consistency.addressConsistent ? 'green' : 'red'}>{consistency.addressConsistent ? '是' : '否'}</Tag></Descriptions.Item>
                  <Descriptions.Item label="不一致数">{consistency.mismatchCount}</Descriptions.Item>
                </Descriptions>
              ) : <Empty description="尚未检查" />}
            </div>
          ),
        },
      ]} />
    </Card>
  );
};

export default PreAuditCheck;
