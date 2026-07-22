/**
 * 角色管理页 - 简化实现
 */
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Tag, Modal, Form, Input, message, Tree } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { listRoles, createRole } from '@/api/system';

const RoleList: React.FC = () => {
  const [list, setList] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [createVisible, setCreateVisible] = useState(false);
  const [createForm] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try { setList(await listRoles()); }
    catch (e: any) { message.error(e.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      await createRole(values);
      message.success('已创建');
      setCreateVisible(false);
      createForm.resetFields();
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '角色编码', dataIndex: 'roleCode', key: 'roleCode', render: (v: string) => <Tag color="blue">{v}</Tag> },
    { title: '角色名称', dataIndex: 'roleName', key: 'roleName' },
    { title: '类型', dataIndex: 'roleType', key: 'roleType' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: number) => <Tag color={v === 1 ? 'green' : 'red'}>{v === 1 ? '启用' : '禁用'}</Tag> },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => v || '-' },
  ];

  return (
    <Card title="角色管理" extra={<Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>新建角色</Button>}>
      <Button icon={<ReloadOutlined />} onClick={load} style={{ marginBottom: 16 }}>刷新</Button>
      <Table columns={columns} dataSource={list} rowKey="id" loading={loading} />
      <Modal title="新建角色" open={createVisible} onOk={handleCreate} onCancel={() => { setCreateVisible(false); createForm.resetFields(); }}>
        <Form form={createForm} layout="vertical">
          <Form.Item name="roleCode" label="角色编码" rules={[{ required: true }]}><Input placeholder="如 RISK_OFFICER" /></Form.Item>
          <Form.Item name="roleName" label="角色名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default RoleList;
