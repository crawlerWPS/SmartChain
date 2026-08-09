/**
 * 用户管理页
 */
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Space, Input, Tag, Modal, Form, Select, message } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { pageUsers, createUser, updateUser, toggleUserStatus } from '@/api/system';
import { Permission } from '@/components/common/Permission';
import { formatDate, maskPhone, maskName } from '@/utils';

const UserList: React.FC = () => {
  const [list, setList] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState<any>({ page: 1, size: 10, keyword: '', roleCode: undefined });
  const [editVisible, setEditVisible] = useState(false);
  const [editForm] = Form.useForm();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [roles, setRoles] = useState<any[]>([]);

  const load = async () => {
    setLoading(true);
    try {
      const result = await pageUsers(query);
      setList(result.list || []);
      setTotal(result.total);
    } catch (e: any) { message.error(e.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, [query.page, query.size]);

  const handleEdit = (record?: any) => {
    setEditingId(record?.id || null);
    if (record) {
      editForm.setFieldsValue(record);
    } else {
      editForm.resetFields();
    }
    setEditVisible(true);
  };

  const handleSave = async () => {
    const values = await editForm.validateFields();
    try {
      if (editingId) {
        await updateUser(editingId, values);
        message.success('已更新');
      } else {
        await createUser(values);
        message.success('已创建');
      }
      setEditVisible(false);
      editForm.resetFields();
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const handleToggle = async (id: number, status: number) => {
    try {
      await toggleUserStatus(id, status);
      message.success('状态已切换');
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '真实姓名', dataIndex: 'realName', key: 'realName', render: (v: string) => maskName(v) },
    { title: '角色', dataIndex: 'roleCode', key: 'roleCode', render: (v: string) => <Tag color="blue">{v}</Tag> },
    { title: '邮箱', dataIndex: 'email', key: 'email' },
    { title: '手机', dataIndex: 'phone', key: 'phone', render: (v: string) => maskPhone(v) },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: number) => <Tag color={v === 1 ? 'green' : 'red'}>{v === 1 ? '启用' : '禁用'}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => formatDate(v) },
    { title: '操作', key: 'action', render: (_: any, r: any) => (
      <Space>
        <Permission perm={['USER', 'update']}>
          <a onClick={() => handleEdit(r)}>编辑</a>
        </Permission>
        <Permission perm={['USER', 'update']}>
          <a onClick={() => handleToggle(r.id, r.status === 1 ? 0 : 1)}>{r.status === 1 ? '禁用' : '启用'}</a>
        </Permission>
      </Space>
    )},
  ];

  return (
    <Card title="用户管理" extra={
      <Permission perm={['USER', 'create']}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => handleEdit()}>新建用户</Button>
      </Permission>
    }>
      <Space style={{ marginBottom: 16 }}>
        <Input.Search placeholder="用户名/真实姓名" allowClear onSearch={(v) => setQuery({ ...query, keyword: v, page: 1 })} style={{ width: 240 }} />
        <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
      </Space>

      <Table columns={columns} dataSource={list} rowKey="id" loading={loading}
        pagination={{ current: query.page, pageSize: query.size, total, onChange: (p, s) => setQuery({ ...query, page: p, size: s }) }} />

      <Modal title={editingId ? '编辑用户' : '新建用户'} open={editVisible} onOk={handleSave} onCancel={() => { setEditVisible(false); editForm.resetFields(); }}>
        <Form form={editForm} layout="vertical">
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}><Input disabled={!!editingId} /></Form.Item>
          <Form.Item name="realName" label="真实姓名" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="roleCode" label="角色" rules={[{ required: true }]}>
            <Select options={[
              { label: '管理员', value: 'ADMIN' },
              { label: '风控经理', value: 'RISK_MANAGER' },
              { label: '合规官', value: 'COMPLIANCE_OFFICER' },
              { label: '业务用户', value: 'BUSINESS_USER' },
            ]} />
          </Form.Item>
          <Form.Item name="email" label="邮箱"><Input /></Form.Item>
          <Form.Item name="phone" label="手机"><Input /></Form.Item>
          {!editingId && <Form.Item name="password" label="初始密码" rules={[{ required: true }]}><Input.Password /></Form.Item>}
        </Form>
      </Modal>
    </Card>
  );
};

export default UserList;
