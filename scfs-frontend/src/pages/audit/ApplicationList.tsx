/** 融资申请列表 */
import React, { useEffect, useMemo, useState } from 'react';
import { Card, Table, Button, Space, Input, Select, message, Modal, Form, InputNumber } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { history } from '@umijs/max';
import {
  pageApplications,
  createApplication,
  listApplicationCustomers,
  type ApplicationCustomer,
} from '@/api/application';
import { ApplicationStatusTag } from '@/components/common/StatusTag';
import { Permission } from '@/components/common/Permission';
import { formatAmount, formatDate } from '@/utils';
import { ApplicationStatus } from '@/types';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';

const ApplicationList: React.FC = () => {
  const [list, setList] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState({ page: 1, size: 10, keyword: '', status: undefined });
  const [createVisible, setCreateVisible] = useState(false);
  const [customers, setCustomers] = useState<ApplicationCustomer[]>([]);
  const [buyerCustomers, setBuyerCustomers] = useState<ApplicationCustomer[]>([]);
  const [customerLoading, setCustomerLoading] = useState(false);
  const [createForm] = Form.useForm();
  const dictionary = useCodeDictionary();

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

  const loadCustomers = async () => {
    setCustomerLoading(true);
    try {
      const [all, buyers] = await Promise.all([
        listApplicationCustomers(),
        listApplicationCustomers(undefined, true),
      ]);
      setCustomers(all);
      setBuyerCustomers(buyers);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setCustomerLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [query.page, query.size, query.status]);

  const customerOptions = useMemo(() => customers.map((customer) => ({
    value: customer.enterpriseId,
    label: `${customer.name}（客户号：${customer.enterpriseId}）`,
    searchText: `${customer.name} ${customer.enterpriseId} ${customer.uscc}`,
  })), [customers]);

  const buyerOptions = useMemo(() => buyerCustomers.map((customer) => ({
    value: customer.enterpriseId,
    label: `${customer.name}（客户号：${customer.enterpriseId}）`,
    searchText: `${customer.name} ${customer.enterpriseId} ${customer.uscc}`,
  })), [buyerCustomers]);

  const openCreate = () => {
    setCreateVisible(true);
    if (customers.length === 0) loadCustomers();
  };

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      const id = await createApplication(values);
      message.success('融资申请创建成功');
      setCreateVisible(false);
      createForm.resetFields();
      load();
      history.push(`/audit/application/detail?appId=${id}`);
    } catch (e: any) {
      message.error(e.message);
    }
  };

  const customerSelectProps = {
    showSearch: true,
    loading: customerLoading,
    options: customerOptions,
    optionFilterProp: 'searchText',
    placeholder: '请选择或搜索客户名称/客户号/信用代码',
  } as const;

  const columns = [
    { title: '申请编号', dataIndex: 'appNo', key: 'appNo', width: 180 },
    { title: '买方名称', dataIndex: 'buyerName', key: 'buyerName', render: (v: string) => v || '-' },
    { title: '卖方名称', dataIndex: 'sellerName', key: 'sellerName', render: (v: string) => v || '-' },
    { title: '业务类型', dataIndex: 'businessType', key: 'businessType', render: (v: string) => dictionary.label('BUSINESS_TYPE', v) },
    { title: '融资金额', dataIndex: 'financingAmount', key: 'financingAmount', render: (v: number) => formatAmount(v) },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: ApplicationStatus) => <ApplicationStatusTag status={v} /> },
    { title: '提交时间', dataIndex: 'submittedAt', key: 'submittedAt', render: (v: string) => formatDate(v) },
    {
      title: '操作', key: 'action', render: (_: any, record: any) => (
        <a onClick={() => history.push(`/audit/application/detail?appId=${record.id}`)}>详情</a>
      ),
    },
  ];

  return (
    <Card title="融资申请列表" extra={<Space>
      <Permission perm={['VERIFY', 'create']}>
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建申请</Button>
      </Permission>
      <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
    </Space>}>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search placeholder="申请编号/买卖方名称" allowClear
          onSearch={(v) => setQuery({ ...query, keyword: v, page: 1 })} style={{ width: 240 }} />
        <Select placeholder="状态" allowClear style={{ width: 160 }}
          options={dictionary.options('APPLICATION_STATUS')}
          onChange={(v) => setQuery({ ...query, status: v, page: 1 })} />
      </Space>
      <Table columns={columns} dataSource={list} rowKey="id" loading={loading}
        pagination={{ current: query.page, pageSize: query.size, total,
          onChange: (page, size) => setQuery({ ...query, page, size }) }} />
      <Modal title="新建融资申请" open={createVisible} onOk={handleCreate}
        onCancel={() => { setCreateVisible(false); createForm.resetFields(); }}>
        <Form form={createForm} layout="vertical">
          <Form.Item name="buyerEnterpriseId" label="买方客户" rules={[{ required: true, message: '请选择买方客户' }]}>
            <Select {...customerSelectProps} options={buyerOptions} placeholder="请选择或搜索买方企业名称/客户号/信用代码" notFoundContent="暂无买方企业" />
          </Form.Item>
          <Form.Item name="sellerEnterpriseId" label="卖方客户" dependencies={['buyerEnterpriseId']}
            rules={[{ required: true, message: '请选择卖方客户' }, ({ getFieldValue }) => ({
              validator(_, value) {
                return !value || value !== getFieldValue('buyerEnterpriseId')
                  ? Promise.resolve() : Promise.reject(new Error('买方和卖方不能相同'));
              },
            })]}>
            <Select {...customerSelectProps} />
          </Form.Item>
          <Form.Item name="businessType" label="业务类型" rules={[{ required: true, message: '请选择业务类型' }]}>
            <Select options={dictionary.options('BUSINESS_TYPE')} />
          </Form.Item>
          <Form.Item name="financingAmount" label="融资金额" rules={[{ required: true, message: '请输入融资金额' }]}>
            <InputNumber min={0.01} precision={2} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default ApplicationList;
