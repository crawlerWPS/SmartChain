/**
 * 申请详情页 - 整合申请信息、状态流转、操作按钮
 */
import React, { useEffect, useState } from 'react';
import { Card, Descriptions, Button, Space, message, Modal, Typography, Form, Input, InputNumber } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useSearchParams, history } from '@umijs/max';
import { getApplication, submitApplication, rejectApplication, approveApplication, escalateApplicationToOps } from '@/api/application';
import { canSubmit, formatDate, formatAmount } from '@/utils';
import { ApplicationStatusTag } from '@/components/common/StatusTag';
import { Permission } from '@/components/common/Permission';
import { ApplicationStatus } from '@/types';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';
import { hasRole } from '@/access/access';

const { Title } = Typography;

const ApplicationDetail: React.FC = () => {
  const [searchParams] = useSearchParams();
  const appId = Number(searchParams.get('appId') || 0);
  const [detail, setDetail] = useState<any>(null);
  const [reviewAction, setReviewAction] = useState<'approve' | 'escalate'>();
  const [reviewSubmitting, setReviewSubmitting] = useState(false);
  const [reviewForm] = Form.useForm();
  const dictionary = useCodeDictionary();
  const riskOfficer = hasRole('RCO');
  const operationsSupervisor = hasRole('OPS');
  const canReview = (riskOfficer && detail?.status === ApplicationStatus.SUBMITTED)
    || (operationsSupervisor && detail?.status === ApplicationStatus.PENDING_DECISION);

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

  const handleReject = async () => {
    const reason = window.prompt('驳回原因');
    if (!reason) return;
    try {
      await rejectApplication(appId, reason);
      message.success('已驳回');
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const handleReviewSubmit = async () => {
    const values = await reviewForm.validateFields();
    setReviewSubmitting(true);
    try {
      if (reviewAction === 'approve') {
        await approveApplication(appId, values.remark.trim());
        message.success('融资申请已通过');
      } else if (reviewAction === 'escalate') {
        await escalateApplicationToOps(appId, values.supervisorId, values.remark.trim());
        message.success('已升级并分配至运营主管');
      }
      setReviewAction(undefined);
      reviewForm.resetFields();
      load();
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setReviewSubmitting(false);
    }
  };

  const openReview = (action: 'approve' | 'escalate') => {
    reviewForm.resetFields();
    setReviewAction(action);
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
            <Permission perm={['VERIFY', 'create']} menuCode="application:submit">
              <Button type="primary" onClick={handleSubmit}>提交申请</Button>
            </Permission>
          )}
          {canReview && (
            <Permission perm={['VERIFY', 'reject']} menuCode="application:reject">
              <Button danger onClick={handleReject}>驳回</Button>
            </Permission>
          )}
          {canReview && (
            <Permission perm={['VERIFY', 'approve']} menuCode="application:approve">
              <Button type="primary" onClick={() => openReview('approve')}>通过</Button>
            </Permission>
          )}
          {riskOfficer && detail.status === ApplicationStatus.SUBMITTED && (
            <Permission perm={['VERIFY', 'approve']} menuCode="application:escalate">
              <Button onClick={() => openReview('escalate')}>无法判断，升级运营主管</Button>
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

      <Modal
        title={reviewAction === 'approve' ? '通过融资申请' : '升级运营主管'}
        open={!!reviewAction}
        confirmLoading={reviewSubmitting}
        okText="提交"
        cancelText="取消"
        onOk={handleReviewSubmit}
        onCancel={() => { setReviewAction(undefined); reviewForm.resetFields(); }}
      >
        <Form form={reviewForm} layout="vertical">
          {reviewAction === 'escalate' && (
            <Form.Item name="supervisorId" label="运营主管用户 ID"
              rules={[{ required: true, message: '请输入运营主管用户 ID' }, { type: 'number', min: 1, message: '用户 ID 必须是正整数' }]}>
              <InputNumber min={1} precision={0} style={{ width: '100%' }} placeholder="请输入启用状态的运营主管用户 ID" />
            </Form.Item>
          )}
          <Form.Item name="remark" label="审核意见"
            rules={[{ required: true, whitespace: true, message: '请填写审核意见' }]}>
            <Input.TextArea rows={4} maxLength={500} showCount placeholder="请填写本次审核判断依据和意见" />
          </Form.Item>
        </Form>
      </Modal>

    </div>
  );
};

export default ApplicationDetail;
