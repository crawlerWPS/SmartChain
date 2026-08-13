/**
 * 规则管理页 - 双岗经办/复核
 */
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Space, Input, Select, Tag, message, Modal, Form, Descriptions, Typography } from 'antd';
import { PlusOutlined, ReloadOutlined, EyeOutlined } from '@ant-design/icons';
import { pageRules, getRule, createRule, submitRuleChange, toggleRuleStatus, pagePendingChanges, approveRuleChange, rejectRuleChange } from '@/api/rule';
import { Permission } from '@/components/common/Permission';
import { formatDate } from '@/utils';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';
import { CodeTag } from '@/components/common/CodeTag';

const RuleList: React.FC = () => {
  const formatDrlContent = (content?: string) => content ? content.replace(/\\n/g, '\n').replace(/\\r/g, '\r') : '暂无规则内容';
  const [list, setList] = useState<any[]>([]);
  const [pendingList, setPendingList] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState({ page: 1, size: 10, keyword: '', category: undefined, status: undefined });
  const [createVisible, setCreateVisible] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [selectedRule, setSelectedRule] = useState<any>(null);
  const [submitMode, setSubmitMode] = useState(false);
  const [createForm] = Form.useForm();
  const dictionary = useCodeDictionary();

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

  const openRuleDetail = async (id: number, forSubmit = false) => {
    setDetailLoading(true);
    setSubmitMode(forSubmit);
    setDetailVisible(true);
    try {
      setSelectedRule(await getRule(id));
    } catch (e: any) {
      setDetailVisible(false);
      message.error(e.message);
    } finally {
      setDetailLoading(false);
    }
  };

  const handleSubmit = async () => {
    if (!selectedRule) return;
    try {
      await submitRuleChange(selectedRule.id, 'UPDATE', { remark: '已查看完整规则内容，提交审核' });
      message.success('已提交，等待复核');
      setDetailVisible(false);
      setSelectedRule(null);
      setSubmitMode(false);
      load();
    } catch (e: any) {
      message.error(e.message);
    }
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
    { title: '规则编码', dataIndex: 'ruleCode', key: 'ruleCode', render: (v: string, r: any) => <a onClick={() => openRuleDetail(r.id)}>{v}</a> },
    { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', render: (v: string, r: any) => <a onClick={() => openRuleDetail(r.id)}>{v}</a> },
    { title: '分类', dataIndex: 'category', key: 'category', render: (v: string) => <CodeTag type="RULE_CATEGORY" code={v} /> },
    { title: '版本', dataIndex: 'version', key: 'version', render: (v: number) => `v${v}` },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: number) => <CodeTag type="ENABLE_STATUS" code={v} /> },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => formatDate(v) },
    { title: '操作', key: 'action', render: (_: any, r: any) => (
      <Space>
        <a onClick={() => openRuleDetail(r.id)}><EyeOutlined /> 查看详情</a>
        <Permission perm={['RULE', 'create']}>
          <a onClick={() => openRuleDetail(r.id, true)}>提交</a>
        </Permission>
        <Permission perm={['RULE', 'update']}>
          <a onClick={() => handleToggleStatus(r.id, r.status === 1 ? 0 : 1)}>{r.status === 1 ? '禁用' : '启用'}</a>
        </Permission>
      </Space>
    )},
  ];

  const pendingColumns = [
    { title: '规则编码', dataIndex: 'ruleCode', key: 'ruleCode' },
    { title: '变更类型', dataIndex: 'changeType', key: 'changeType', render: (v: string) => dictionary.label('RULE_CHANGE_TYPE', v) },
    { title: '原版本', dataIndex: 'oldVersion', key: 'oldVersion' },
    { title: '新版本', dataIndex: 'newVersion', key: 'newVersion' },
    { title: '经办人', dataIndex: 'makerId', key: 'makerId' },
    { title: '操作', key: 'action', render: (_: any, r: any) => (
      <Space>
        <Permission perm={['RULE', 'approve']}>
          <a onClick={() => handleApprove(r.id)}>通过</a>
        </Permission>
        <Permission perm={['RULE', 'approve']}>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleReject(r.id)}>驳回</a>
        </Permission>
      </Space>
    )},
  ];

  return (
    <Card title="规则管理" extra={
      <Permission perm={['RULE', 'create']}>
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
            <Select options={dictionary.options('RULE_CATEGORY')} />
          </Form.Item>
          <Form.Item name="drlContent" label="DRL 内容" rules={[{ required: true }]}><Input.TextArea rows={8} placeholder="package com.scfs.rules; ..." /></Form.Item>
        </Form>
      </Modal>

      <Modal
        title={submitMode ? '提交前确认：完整规则内容' : '规则详情'}
        open={detailVisible}
        confirmLoading={detailLoading}
        width={900}
        onCancel={() => { setDetailVisible(false); setSelectedRule(null); setSubmitMode(false); }}
        footer={submitMode ? [
          <Button key="cancel" onClick={() => { setDetailVisible(false); setSubmitMode(false); }}>取消</Button>,
          <Button key="submit" type="primary" onClick={handleSubmit} disabled={!selectedRule}>确认提交审核</Button>,
        ] : [<Button key="close" onClick={() => setDetailVisible(false)}>关闭</Button>]}
      >
        {selectedRule && <>
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="规则编码">{selectedRule.ruleCode}</Descriptions.Item>
            <Descriptions.Item label="规则名称">{selectedRule.ruleName}</Descriptions.Item>
            <Descriptions.Item label="分类">{dictionary.label('RULE_CATEGORY', selectedRule.category)}</Descriptions.Item>
            <Descriptions.Item label="版本">v{selectedRule.version}</Descriptions.Item>
            <Descriptions.Item label="状态"><CodeTag type="ENABLE_STATUS" code={selectedRule.status} /></Descriptions.Item>
            <Descriptions.Item label="创建人">{selectedRule.createdBy}</Descriptions.Item>
          </Descriptions>
          <Card size="small" title="规则参数" style={{ marginTop: 16 }}>
            <pre style={{ margin: 0, whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
              {selectedRule.params ? JSON.stringify(selectedRule.params, null, 2) : '无参数'}
            </pre>
          </Card>
          <Card size="small" title="DRL 规则内容" style={{ marginTop: 16 }}>
            <pre style={{ margin: 0, maxHeight: 320, overflow: 'auto', whiteSpace: 'pre-wrap', wordBreak: 'break-word', background: '#fafafa', padding: 16 }}>
              {formatDrlContent(selectedRule.drlContent)}
            </pre>
          </Card>
          {submitMode && <Typography.Text type="warning" style={{ display: 'block', marginTop: 16 }}>
            请确认已查看规则编码、分类、版本、参数和完整 DRL 内容；确认后才会提交复核。
          </Typography.Text>}
        </>}
      </Modal>
    </Card>
  );
};

export default RuleList;
