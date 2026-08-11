/**
 * 申请详情页 - 整合申请信息、状态流转、操作按钮
 */
import React, { useEffect, useState } from 'react';
import { Card, Descriptions, Steps, Button, Space, Tag, message, Modal, Input, Typography } from 'antd';
import { useSearchParams, history } from '@umijs/max';
import { getApplication, submitApplication, assignApplication, rejectApplication, approveApplication, getStatusHistory } from '@/api/application';
import { canSubmit, canAssign, canReject, canApprove, formatDate, formatAmount } from '@/utils';
import { ApplicationStatusTag } from '@/components/common/StatusTag';
import { Permission } from '@/components/common/Permission';
import { ApplicationStatus } from '@/types';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';

const { Title, Text } = Typography;

const ApplicationDetail: React.FC = () => {
  const [searchParams] = useSearchParams();
  const appId = Number(searchParams.get('appId') || 0);
  const [detail, setDetail] = useState<any>(null);
  const [history, setHistory] = useState<any[]>([]);
  const dictionary = useCodeDictionary();

  const load = async () => {
    if (!appId) return;
    try {
      const [d, h] = await Promise.all([
        getApplication(appId),
        getStatusHistory(appId).catch(() => []),
      ]);
      setDetail(d);
      setHistory(h || []);
    } catch (e: any) {
      message.error(e.message);
    }
  };

  useEffect(() => { load(); }, [appId]);

  const handleSubmit = async () => {
    try {
      await submitApplication(appId);
      message.success('已提交');
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const handleAssign = async () => {
    const handlerId = window.prompt('请输入审核人 ID');
    if (!handlerId) return;
    try {
      await assignApplication(appId, Number(handlerId));
      message.success('已分配');
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const handleReject = async () => {
    const reason = window.prompt('驳回原因');
    if (!reason) return;
    try {
      await rejectApplication(appId, reason);
      message.success('已驳回');
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const handleApprove = async () => {
    Modal.confirm({
      title: '确认审批通过？',
      onOk: async () => {
        try {
          await approveApplication(appId);
          message.success('已通过');
          load();
        } catch (e: any) { message.error(e.message); }
      },
    });
  };

  if (!detail) return <Card loading />;

  return (
    <div>
      <Title level={4}>融资申请详情 #{detail.appNo}</Title>

      <Card title="基本信息" style={{ marginBottom: 16 }}>
        <Descriptions bordered column={2}>
          <Descriptions.Item label="申请编号">{detail.appNo}</Descriptions.Item>
          <Descriptions.Item label="企业ID">{detail.enterpriseId}</Descriptions.Item>
          <Descriptions.Item label="业务类型">{dictionary.label('BUSINESS_TYPE', detail.businessType)}</Descriptions.Item>
          <Descriptions.Item label="融资金额">{formatAmount(detail.financingAmount)}</Descriptions.Item>
          <Descriptions.Item label="状态"><ApplicationStatusTag status={detail.status as ApplicationStatus} /></Descriptions.Item>
          <Descriptions.Item label="版本">v{detail.version}</Descriptions.Item>
          <Descriptions.Item label="提交人">{detail.submittedBy || '-'}</Descriptions.Item>
          <Descriptions.Item label="当前处理人">{detail.currentHandler || '-'}</Descriptions.Item>
          <Descriptions.Item label="提交时间">{formatDate(detail.submittedAt)}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{formatDate(detail.createdAt)}</Descriptions.Item>
        </Descriptions>

        <Space style={{ marginTop: 16 }}>
          {canSubmit(detail.status) && (
            <Permission perm={['VERIFY', 'create']}>
              <Button type="primary" onClick={handleSubmit}>提交申请</Button>
            </Permission>
          )}
          {canAssign(detail.status) && (
            <Permission perm={['VERIFY', 'approve']}>
              <Button onClick={handleAssign}>分配审核人</Button>
            </Permission>
          )}
          {canReject(detail.status) && (
            <Permission perm={['VERIFY', 'reject']}>
              <Button danger onClick={handleReject}>驳回</Button>
            </Permission>
          )}
          {canApprove(detail.status) && (
            <Permission perm={['VERIFY', 'approve']}>
              <Button type="primary" onClick={handleApprove}>审批通过</Button>
            </Permission>
          )}
        </Space>
      </Card>

      <Card title="操作快捷入口">
        <Space wrap>
          <Button onClick={() => window.location.href = `/audit/material/${appId}`}>材料核验</Button>
          <Button onClick={() => window.location.href = `/audit/preaudit/${appId}`}>预审补正</Button>
          <Button onClick={() => window.location.href = `/audit/report/${appId}`}>核验报告</Button>
          <Button onClick={() => window.location.href = `/audit/risk/${appId}`}>风险画像</Button>
        </Space>
      </Card>

      {history.length > 0 && (
        <Card title="状态流转历史" style={{ marginTop: 16 }}>
          <Steps
            direction="vertical"
            current={history.length - 1}
            items={history.map((h) => ({
              title: <ApplicationStatusTag status={h.toStatus} />,
              description: `操作人: ${h.operatorId} | ${formatDate(h.createdAt)} | ${h.remark || ''}`,
            }))}
          />
        </Card>
      )}
    </div>
  );
};

export default ApplicationDetail;
