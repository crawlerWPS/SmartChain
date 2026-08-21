/** 风险权重配置页（双岗复核） */
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Modal, Form, Input, InputNumber, message, Popconfirm, Space } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { listWeightConfigs, createWeightConfig, approveWeightConfig, rejectWeightConfig } from '@/api/risk';
import { Permission } from '@/components/common/Permission';
import { formatDate } from '@/utils';
import { CodeTag } from '@/components/common/CodeTag';
import type { RiskWeightConfig } from '@/types';

const WeightConfig: React.FC = () => {
  const [list, setList] = useState<RiskWeightConfig[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [createVisible, setCreateVisible] = useState(false);
  const [rejecting, setRejecting] = useState<RiskWeightConfig>();
  const [createForm] = Form.useForm();
  const [rejectForm] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const result = await listWeightConfigs();
      setList(result.list || []);
      setTotal(result.total || 0);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleApprove = async (id: number) => {
    try {
      await approveWeightConfig(id);
      message.success('权重配置已通过并生效');
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const handleReject = async () => {
    if (!rejecting) return;
    const { reason } = await rejectForm.validateFields();
    try {
      await rejectWeightConfig(rejecting.id, reason);
      message.success('权重配置已驳回');
      setRejecting(undefined);
      rejectForm.resetFields();
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    if (values.supplyChainWeight + values.transactionWeight + values.materialWeight !== 100) {
      message.error('三个权重之和必须为 100');
      return;
    }
    try {
      await createWeightConfig(values);
      message.success('权重配置已创建并进入待复核状态');
      setCreateVisible(false);
      createForm.resetFields();
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const columns = [
    { title: '配置名称', dataIndex: 'configName', key: 'configName' },
    { title: '供应链', dataIndex: 'supplyChainWeight', key: 'supplyChainWeight', render: (v: number) => `${v}%` },
    { title: '交易', dataIndex: 'transactionWeight', key: 'transactionWeight', render: (v: number) => `${v}%` },
    { title: '材料', dataIndex: 'materialWeight', key: 'materialWeight', render: (v: number) => `${v}%` },
    { title: '低风险阈值', dataIndex: 'lowRiskThreshold', key: 'lowRiskThreshold' },
    { title: '中风险阈值', dataIndex: 'midRiskThreshold', key: 'midRiskThreshold' },
    { title: '高风险阈值', dataIndex: 'highRiskThreshold', key: 'highRiskThreshold' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: string) => <CodeTag type="DUAL_CONTROL_STATUS" code={v} /> },
    { title: '经办人', dataIndex: 'makerId', key: 'makerId' },
    { title: '复核人', dataIndex: 'checkerId', key: 'checkerId', render: (v: number) => v || '-' },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => formatDate(v) },
    { title: '操作', key: 'action', render: (_: unknown, record: RiskWeightConfig) => (
      record.status === 'PENDING' ? <Permission perm={['RULE', 'approve']} menuCode="weight:approve">
        <Space>
          <Popconfirm title="确认通过该权重配置并使其生效？" onConfirm={() => handleApprove(record.id)}>
            <a>通过</a>
          </Popconfirm>
          <Permission perm={['RULE', 'approve']} menuCode="weight:reject"><a style={{ color: '#ff4d4f' }} onClick={() => setRejecting(record)}>驳回</a></Permission>
        </Space>
      </Permission> : '-'
    ) },
  ];

  return (
    <Card title="风险权重配置" extra={<Permission perm={['RULE', 'create']} menuCode="weight:create">
      <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>新建权重</Button>
    </Permission>}>
      <Button icon={<ReloadOutlined />} onClick={load} style={{ marginBottom: 16 }}>刷新</Button>
      <Table columns={columns} dataSource={list} rowKey="id" loading={loading}
        pagination={{ total, pageSize: 20, hideOnSinglePage: true }} />
      <Modal title="新建风险权重配置" open={createVisible} onOk={handleCreate}
        onCancel={() => { setCreateVisible(false); createForm.resetFields(); }}>
        <Form form={createForm} layout="vertical" initialValues={{ supplyChainWeight: 40, transactionWeight: 30, materialWeight: 30, lowRiskThreshold: 85, midRiskThreshold: 70, highRiskThreshold: 50 }}>
          <Form.Item name="configName" label="配置名称" rules={[{ required: true, message: '请输入配置名称' }]}><Input /></Form.Item>
          <Form.Item name="supplyChainWeight" label="供应链权重（%）" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
          <Form.Item name="transactionWeight" label="交易权重（%）" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
          <Form.Item name="materialWeight" label="材料权重（%）" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
          <Form.Item name="lowRiskThreshold" label="低风险阈值" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
          <Form.Item name="midRiskThreshold" label="中风险阈值" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
          <Form.Item name="highRiskThreshold" label="高风险阈值" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
        </Form>
      </Modal>
      <Modal title="驳回权重配置" open={!!rejecting} onOk={handleReject}
        onCancel={() => { setRejecting(undefined); rejectForm.resetFields(); }}>
        <Form form={rejectForm} layout="vertical">
          <Form.Item name="reason" label="驳回原因" rules={[{ required: true, message: '请输入驳回原因' }]}>
            <Input.TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default WeightConfig;
