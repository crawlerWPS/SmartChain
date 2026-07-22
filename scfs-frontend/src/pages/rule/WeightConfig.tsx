/**
 * 风险权重配置页 - 双岗
 */
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Tag, Modal, Form, InputNumber, message } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { listWeightConfigs, createWeightConfig, submitWeightConfig, approveWeightConfig, rejectWeightConfig } from '@/api/risk';
import { Permission } from '@/components/common/Permission';
import { formatDate } from '@/utils';

const WeightConfig: React.FC = () => {
  const [list, setList] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [createVisible, setCreateVisible] = useState(false);
  const [createForm] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const result = await listWeightConfigs();
      setList(result || []);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleSubmit = async (id: number) => {
    try {
      await submitWeightConfig(id);
      message.success('已提交审核');
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const handleApprove = async (id: number) => {
    try {
      await approveWeightConfig(id);
      message.success('已通过');
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const handleReject = async (id: number) => {
    const reason = window.prompt('驳回原因');
    if (!reason) return;
    try {
      await rejectWeightConfig(id, reason);
      message.success('已驳回');
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
      message.success('已创建草稿');
      setCreateVisible(false);
      createForm.resetFields();
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const columns = [
    { title: '配置名', dataIndex: 'configName', key: 'configName' },
    { title: '供应链', dataIndex: 'supplyChainWeight', key: 'supplyChainWeight', render: (v: number) => `${v}%` },
    { title: '交易', dataIndex: 'transactionWeight', key: 'transactionWeight', render: (v: number) => `${v}%` },
    { title: '材料', dataIndex: 'materialWeight', key: 'materialWeight', render: (v: number) => `${v}%` },
    { title: '低风险阈值', dataIndex: 'lowRiskThreshold', key: 'lowRiskThreshold' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: string) => {
      const color = v === 'ENABLED' ? 'green' : v === 'PENDING' ? 'orange' : 'default';
      return <Tag color={color}>{v}</Tag>;
    }},
    { title: '经办人', dataIndex: 'makerId', key: 'makerId' },
    { title: '复核人', dataIndex: 'checkerId', key: 'checkerId' },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => formatDate(v) },
    { title: '操作', key: 'action', render: (_: any, r: any) => (
      <>
        {r.status === 'DRAFT' && (
          <Permission perm={['weight', 'submit']}>
            <a onClick={() => handleSubmit(r.id)}>提交</a>
          </Permission>
        )}
        {r.status === 'PENDING' && r.makerId && (
          <Permission perm={['weight', 'approve']}>
            <a onClick={() => handleApprove(r.id)} style={{ marginRight: 8 }}>通过</a>
            <a style={{ color: '#ff4d4f' }} onClick={() => handleReject(r.id)}>驳回</a>
          </Permission>
        )}
      </>
    )},
  ];

  return (
    <Card title="风险权重配置" extra={
      <Permission perm={['weight', 'submit']}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>新建权重</Button>
      </Permission>
    }>
      <Button icon={<ReloadOutlined />} onClick={load} style={{ marginBottom: 16 }}>刷新</Button>
      <Table columns={columns} dataSource={list} rowKey="id" loading={loading} />

      <Modal title="新建风险权重配置" open={createVisible} onOk={handleCreate} onCancel={() => { setCreateVisible(false); createForm.resetFields(); }}>
        <Form form={createForm} layout="vertical" initialValues={{ supplyChainWeight: 40, transactionWeight: 30, materialWeight: 30, lowRiskThreshold: 85, midRiskThreshold: 70, highRiskThreshold: 50 }}>
          <Form.Item name="configName" label="配置名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="supplyChainWeight" label="供应链权重 (%)" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
          <Form.Item name="transactionWeight" label="交易权重 (%)" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
          <Form.Item name="materialWeight" label="材料权重 (%)" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
          <Form.Item name="lowRiskThreshold" label="低风险阈值" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
          <Form.Item name="midRiskThreshold" label="中风险阈值" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
          <Form.Item name="highRiskThreshold" label="高风险阈值" rules={[{ required: true }]}><InputNumber min={0} max={100} /></Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default WeightConfig;
