/**
 * 申请详情页 - 整合申请信息、状态流转、操作按钮
 */
import React, { useEffect, useState } from 'react';
import { Card, Descriptions, Button, Space, message, Modal, Typography } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useSearchParams, history } from '@umijs/max';
import { getApplication, submitApplication, assignApplication, rejectApplication, approveApplication } from '@/api/application';
import { canSubmit, canAssign, canReject, canApprove, formatDate, formatAmount } from '@/utils';
import { ApplicationStatusTag } from '@/components/common/StatusTag';
import { Permission } from '@/components/common/Permission';
import { ApplicationStatus } from '@/types';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';

const { Title } = Typography;

const ApplicationDetail: React.FC = () => {
  const [searchParams] = useSearchParams();
  const appId = Number(searchParams.get('appId') || 0);
  const [detail, setDetail] = useState<any>(null);
  const dictionary = useCodeDictionary();

  const load = async () => {
    if (!appId) return;
    try {
      const d = await getApplication(appId);
      setDetail(d);
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
    const handlerId = window.prompt('请输入风控审核员 ID');
    if (!handlerId) return;
    const parsedHandlerId = Number(handlerId.trim());
    if (!Number.isInteger(parsedHandlerId) || parsedHandlerId <= 0) {
      message.error('审核人 ID 必须是正整数');
      return;
    }
    try {
      await assignApplication(appId, parsedHandlerId);
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
      <Space align="center" size={12} style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => history.push('/audit/application')}>
          返回申请列表
        </Button>
        <Title level={4} style={{ margin: 0 }}>融资申请详情 #{detail.appNo}</Title>
      </Space>

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

    </div>
  );
};

export default ApplicationDetail;
