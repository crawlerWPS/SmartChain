/**
 * 规则管理页 - 双岗经办/复核
 */
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Space, Input, Select, Tag, message, Modal, Form } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { pageRules, createRule, submitRuleChange, toggleRuleStatus, pagePendingChanges, approveRuleChange, rejectRuleChange } from '@/api/rule';
import { Permission } from '@/components/common/Permission';
import { formatDate } from '@/utils';

const RuleList: React.FC = () => {
  const [list, setList] = useState<any[]>([]);
  const [pendingList, setPendingList] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState({ page: 1, size: 10, keyword: '', category: undefined, status: undefined });
  const [createVisible, setCreateVisible] = useState(false);
  const [createForm] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const [result, pending] = await Promise.all([
        pageRules(query),
        pagePendingChanges({ page: 1, size: 10 }).catch(() => ({ list: [], total: 0 })),
      ]);
      setList(result.list || []);
      setTotal(result.total);
      setPendingList(pending.list || []);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [query.page, query.size]);

  const handleSubmit = async (id: number) => {
    Modal.confirm({
      title: '提交规则变更审核？',
      content: '提交后将进入待复核状态，复核通过后规则才生效',
      onOk: async () => {
        try {
          await submitRuleChange(id, 'UPDATE', { remark: '提交审核' });
          message.success('已提交');
          load();
        } catch (e: any) {
          message.error(e.message);
        }
      },
    });
  };

  const handleApprove = async (id: number) => {
    try {
      await approveRuleChange(id);
      message.success('已审批通过');
      load();
    } catch (e: any) {
      message.error(e.message);
    }
  };

  const handleReject = async (id: number) => {
    const reason = window.prompt('驳回原因');
    if (!reason) return;
    try {
      await rejectRuleChange(id, reason);
      message.success('已驳回');
      load();
    } catch (e: any) {
      message.error(e.message);
    }
  };

  const handleToggleStatus = async (id: number, status: number) => {
    try {
      await toggleRuleStatus(id, status);
      message.success('状态已切换');
      load();
    } catch (e: any) {
      message.error(e.message);
    }
  };

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      await createRule(values);
      message.success('规则已创建（草稿）');
      setCreateVisible(false);
      createForm.resetFields();
      load();
    } catch (e: any) {
      message.error(e.message);
    }
  };

  const columns = [
    { title: '规则编码', dataIndex: 'ruleCode', key: 'ruleCode' },
    { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName' },
    { title: '分类', dataIndex: 'category', key: 'category', render: (v: string) => <Tag>{v}</Tag> },
    { title: '版本', dataIndex: 'version', key: 'version', render: (v: number) => `v${v}` },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: number) => <Tag color={v === 1 ? 'green' : 'default'}>{v === 1 ? '启用' : '禁用'}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => formatDate(v) },
    { title: '操作', key: 'action', render: (_: any, r: any) => (
      <Space>
        <Permission perm={['rule', 'submit']}>
          <a onClick={() => handleSubmit(r.id)}>提交</a>
        </Permission>
        <Permission perm={['rule', 'edit']}>
          <a onClick={() => handleToggleStatus(r.id, r.status === 1 ? 0 : 1)}>{r.status === 1 ? '禁用' : '启用'}</a>
        </Permission>
      </Space>
    )},
  ];

  const pendingColumns = [
    { title: '规则编码', dataIndex: 'ruleCode', key: 'ruleCode' },
    { title: '变更类型', dataIndex: 'changeType', key: 'changeType' },
    { title: '原版本', dataIndex: 'oldVersion', key: 'oldVersion' },
    { title: '新版本', dataIndex: 'newVersion', key: 'newVersion' },
    { title: '经办人', dataIndex: 'makerId', key: 'makerId' },
    { title: '操作', key: 'action', render: (_: any, r: any) => (
      <Space>
        <Permission perm={['rule', 'approve']}>
          <a onClick={() => handleApprove(r.id)}>通过</a>
        </Permission>
        <Permission perm={['rule', 'approve']}>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleReject(r.id)}>驳回</a>
        </Permission>
      </Space>
    )},
  ];

  return (
    <Card title="规则管理" extra={
      <Permission perm={['rule', 'create']}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>新建规则</Button>
      </Permission>
    }>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search placeholder="规则编码/名称" allowClear onSearch={(v) => setQuery({ ...query, keyword: v, page: 1 })} style={{ width: 240 }} />
        <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
      </Space>

      <Table columns={columns} dataSource={list} rowKey="id" loading={loading} pagination={{ current: query.page, pageSize: query.size, total, onChange: (p, s) => setQuery({ ...query, page: p, size: s }) }} />

      {pendingList.length > 0 && (
        <Card size="small" title="待复核变更" style={{ marginTop: 24 }}>
          <Table columns={pendingColumns} dataSource={pendingList} rowKey="id" pagination={false} size="small" />
        </Card>
      )}

      <Modal title="新建规则" open={createVisible} onOk={handleCreate} onCancel={() => { setCreateVisible(false); createForm.resetFields(); }} width={640}>
        <Form form={createForm} layout="vertical">
          <Form.Item name="ruleCode" label="规则编码" rules={[{ required: true }]}><Input placeholder="如 RULE_001" /></Form.Item>
          <Form.Item name="ruleName" label="规则名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="category" label="分类" rules={[{ required: true }]}>
            <Select options={[
              { label: '完整性', value: 'COMPLETENESS' },
              { label: '有效性', value: 'VALIDITY' },
              { label: '一致性', value: 'CONSISTENCY' },
              { label: '逻辑检查', value: 'LOGIC_CHECK' },
            ]} />
          </Form.Item>
          <Form.Item name="drlContent" label="DRL 内容" rules={[{ required: true }]}><Input.TextArea rows={8} placeholder="package com.scfs.rules; ..." /></Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default RuleList;
