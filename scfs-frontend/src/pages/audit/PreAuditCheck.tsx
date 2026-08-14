/**
 * 预审补正页
 */
import React, { useState, useEffect } from 'react';
import { Card, Tabs, Button, Descriptions, Progress, Tag, message, Empty, Table, Alert } from 'antd';
import { history, useParams } from '@umijs/max';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { checkCompleteness, checkValidity, checkConsistency, getCompletenessResult, getValidityResult, getConsistencyResult, getMismatchDetails } from '@/api/preaudit';
import { formatDate } from '@/utils';
import { CodeTag } from '@/components/common/CodeTag';
import type { EnterpriseInfoConsistencyResult, EnterpriseInfoMismatchDetail, MaterialCompletenessResult, MaterialValidityItem, MaterialValidityResult } from '@/types';

const PreAuditCheck: React.FC = () => {
  const params = useParams();
  const appId = Number(params?.appId || 0);
  const [completeness, setCompleteness] = useState<MaterialCompletenessResult | null>(null);
  const [validity, setValidity] = useState<MaterialValidityResult | null>(null);
  const [consistency, setConsistency] = useState<EnterpriseInfoConsistencyResult | null>(null);
  const [mismatches, setMismatches] = useState<EnterpriseInfoMismatchDetail[]>([]);
  const [loading, setLoading] = useState(false);

  const loadResults = async () => {
    if (!appId) return;
    try {
      const [c, v, con] = await Promise.all([
        getCompletenessResult(appId),
        getValidityResult(appId),
        getConsistencyResult(appId),
      ]);
      setCompleteness(c);
      setValidity(v);
      setConsistency(con);
      setMismatches(con?.id ? await getMismatchDetails(appId, con.id) : []);
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
        setMismatches(r.id ? await getMismatchDetails(appId, r.id) : []);
        message.success('一致性检查完成');
      }
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title={<div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <Button icon={<ArrowLeftOutlined />} onClick={() => history.push(`/audit/application/detail?appId=${appId}`)}>
        返回申请详情
      </Button>
      <span>材料预审 - 申请 #{appId}</span>
    </div>}>
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
                <>
                  <Descriptions bordered style={{ marginBottom: 16 }}>
                    <Descriptions.Item label="检查结论"><Tag color={validity.abnormalCount === 0 ? 'green' : 'red'}>{validity.abnormalCount === 0 ? '全部有效' : '存在异常'}</Tag></Descriptions.Item>
                    <Descriptions.Item label="总文件数">{validity.totalFiles}</Descriptions.Item>
                    <Descriptions.Item label="过期"><Tag color={validity.expiredCount ? 'red' : 'green'}>{validity.expiredCount}</Tag></Descriptions.Item>
                    <Descriptions.Item label="不完整"><Tag color={validity.incompleteCount ? 'orange' : 'green'}>{validity.incompleteCount}</Tag></Descriptions.Item>
                    <Descriptions.Item label="异常材料数"><Tag color={validity.abnormalCount ? 'red' : 'green'}>{validity.abnormalCount}</Tag></Descriptions.Item>
                    <Descriptions.Item label="检查时间">{formatDate(validity.checkedAt)}</Descriptions.Item>
                  </Descriptions>
                  <Table<MaterialValidityItem> rowKey="materialId" pagination={false} dataSource={validity.details?.materialResults || []} columns={[
                    { title: '文件', dataIndex: 'fileName', render: (v, r) => v || `材料 #${r.materialId}` },
                    { title: '材料类型', dataIndex: 'materialType', render: v => <CodeTag type="MATERIAL_TYPE" code={v} /> },
                    { title: 'OCR状态', dataIndex: 'recognized', render: v => <Tag color={v ? 'green' : 'orange'}>{v ? '已识别' : '未识别'}</Tag> },
                    { title: '有效性', dataIndex: 'valid', render: v => <Tag color={v ? 'green' : 'red'}>{v ? '有效' : '异常'}</Tag> },
                    { title: '检查结果', dataIndex: 'issues', render: (issues: string[]) => issues?.length ? issues.join('；') : '未发现异常' },
                  ]} />
                </>
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
                <>
                  <Alert showIcon type="info" message="当前对比融资申请登记信息与合同、发票等材料的 OCR 识别信息。" style={{ marginBottom: 16 }} />
                  <Descriptions bordered style={{ marginBottom: 16 }}>
                    <Descriptions.Item label="总体结论"><Tag color={consistency.overallConsistent ? 'green' : 'red'}>{consistency.overallConsistent ? '一致' : '不一致或缺少可比数据'}</Tag></Descriptions.Item>
                    <Descriptions.Item label="企业名称一致"><Tag color={consistency.nameConsistent ? 'green' : 'red'}>{consistency.nameConsistent ? '是' : '否'}</Tag></Descriptions.Item>
                    <Descriptions.Item label="统一社会信用代码一致"><Tag color={consistency.usccConsistent ? 'green' : 'red'}>{consistency.usccConsistent ? '是' : '否'}</Tag></Descriptions.Item>
                    <Descriptions.Item label="异常字段数">{consistency.mismatchCount}</Descriptions.Item>
                    <Descriptions.Item label="检查时间">{formatDate(consistency.checkedAt)}</Descriptions.Item>
                  </Descriptions>
                  <Table<EnterpriseInfoMismatchDetail> rowKey="id" pagination={false} dataSource={mismatches} locale={{ emptyText: consistency.overallConsistent ? '未发现不一致项' : '暂无明细' }} columns={[
                    { title: '检查字段', dataIndex: 'fieldName' },
                    { title: '问题说明', dataIndex: 'mismatchDetail' },
                    { title: '来源与识别值', dataIndex: 'sourceValues', render: (values: EnterpriseInfoMismatchDetail['sourceValues']) => <>{values?.map((v, i: number) => <div key={`${v.materialId || v.source}-${i}`}><Tag>{v.source}</Tag>{v.context.includes('BUYER') ? '买方' : '卖方'}：{v.value}</div>)}</> },
                  ]} />
                </>
              ) : <Empty description="尚未检查" />}
            </div>
          ),
        },
      ]} />
    </Card>
  );
};

export default PreAuditCheck;
