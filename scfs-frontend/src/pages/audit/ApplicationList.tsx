/**
 * 融资申请列表
 */
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Space, Input, Select, message, Modal, Form, InputNumber, Tag } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { history } from '@umijs/max';
import { pageApplications, createApplication } from '@/api/application';
import { ApplicationStatusTag } from '@/components/common/StatusTag';
import { Permission } from '@/components/common/Permission';
import { BUSINESS_TYPE_MAP, formatAmount, formatDate } from '@/utils';
import { ApplicationStatus } from '@/types';

const statusOptions = Object.values(ApplicationStatus).map((v) => ({ label: v, value: v }));

const ApplicationList: React.FC = () => {
  const [list, setList] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState({ page: 1, size: 10, keyword: '', status: undefined });
  const [createVisible, setCreateVisible] = useState(false);
  const [createForm] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const result = await pageApplications(query);
      setList(result.list || []);
      setTotal(result.total);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [query.page, query.size, query.status]);

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      const result = await createApplication(values);
      message.success('草稿创建成功');
      setCreateVisible(false);
      createForm.resetFields();
      load();
      history.push(`/audit/application/detail?appId=${result.id}`);
    } catch (e: any) {
      message.error(e.message);
    }
  };

  const columns = [
    { title: '申请编号', dataIndex: 'appNo', key: 'appNo', width: 180 },
    { title: '企业ID', dataIndex: 'enterpriseId', key: 'enterpriseId' },
    { title: '业务类型', dataIndex: 'businessType', key: 'businessType', render: (v: string) => BUSINESS_TYPE_MAP[v] || v },
    { title: '融资金额', dataIndex: 'financingAmount', key: 'financingAmount', render: (v: number) => formatAmount(v) },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: ApplicationStatus) => <ApplicationStatusTag status={v} /> },
    { title: '提交时间', dataIndex: 'submittedAt', key: 'submittedAt', render: (v: string) => formatDate(v) },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => formatDate(v) },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: any) => (
        <a onClick={() => history.push(`/audit/application/detail?appId=${record.id}`)}>详情</a>
      ),
    },
  ];

  return (
    <Card
      title="融资申请列表"
      extra={
        <Space>
          {/* 客户经理的申请创建权限对应后端 VERIFY.create */}
          <Permission perm={['VERIFY', 'create']}>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>新建申请</Button>
          </Permission>
          <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
        </Space>
      }
    >
      <Space style={{ marginBottom: 16 }}>
        <Input.Search
          placeholder="申请编号/企业"
          allowClear
          onSearch={(v) => setQuery({ ...query, keyword: v, page: 1 })}
          style={{ width: 240 }}
        />
        <Select
          placeholder="状态"
          allowClear
          style={{ width: 160 }}
          options={statusOptions}
          onChange={(v) => setQuery({ ...query, status: v, page: 1 })}
        />
      </Space>

      <Table
        columns={columns}
        dataSource={list}
        rowKey="id"
        loading={loading}
        pagination={{
          current: query.page,
          pageSize: query.size,
          total,
          onChange: (page, size) => setQuery({ ...query, page, size }),
        }}
      />

      <Modal
        title="新建融资申请"
        open={createVisible}
        onOk={handleCreate}
        onCancel={() => { setCreateVisible(false); createForm.resetFields(); }}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="enterpriseId" label="企业ID" rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="businessType" label="业务类型" rules={[{ required: true }]}>
            <Select options={Object.entries(BUSINESS_TYPE_MAP).map(([k, v]) => ({ label: v, value: k }))} />
          </Form.Item>
          <Form.Item name="financingAmount" label="融资金额" rules={[{ required: true }]}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default ApplicationList;
