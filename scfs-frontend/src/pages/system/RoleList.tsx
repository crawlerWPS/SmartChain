import React, { useEffect, useMemo, useState } from 'react';
import { Button, Card, Form, Input, message, Modal, Select, Space, Table, Tag, Tree } from 'antd';
import { PlusOutlined, ReloadOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import type { DataNode } from 'antd/es/tree';
import type { SysMenu, SysRole } from '@/types';
import { Permission } from '@/components/common/Permission';
import { assignRoleMenus, createRole, deleteRole, getMenuTree, getRoleMenus, listRoles } from '@/api/system';

const toTreeData = (menus: SysMenu[]): DataNode[] => menus.map((menu) => ({
  key: menu.id,
  title: (
    <Space size={8}>
      <span>{menu.menuName}</span>
      <Tag color={menu.menuType === 'BUTTON' ? 'gold' : menu.menuType === 'MENU' ? 'blue' : 'default'}>
        {menu.menuType === 'BUTTON' ? '按钮' : menu.menuType === 'MENU' ? '菜单' : '目录'}
      </Tag>
      {menu.permission && <span style={{ color: '#999' }}>{menu.permission}</span>}
    </Space>
  ),
  children: menu.children ? toTreeData(menu.children) : undefined,
}));

const RoleList: React.FC = () => {
  const [roles, setRoles] = useState<SysRole[]>([]);
  const [menus, setMenus] = useState<SysMenu[]>([]);
  const [loading, setLoading] = useState(false);
  const [createVisible, setCreateVisible] = useState(false);
  const [permissionVisible, setPermissionVisible] = useState(false);
  const [selectedRole, setSelectedRole] = useState<SysRole>();
  const [checkedKeys, setCheckedKeys] = useState<React.Key[]>([]);
  const [permissionLoading, setPermissionLoading] = useState(false);
  const [createForm] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const [roleList, menuTree] = await Promise.all([listRoles(), getMenuTree()]);
      setRoles(roleList || []);
      setMenus(menuTree || []);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);
  const treeData = useMemo(() => toTreeData(menus), [menus]);

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      await createRole({ ...values, roleCode: values.roleCode.trim().toUpperCase() });
      message.success('角色已创建');
      setCreateVisible(false);
      createForm.resetFields();
      load();
    } catch (e: any) {
      message.error(e.message);
    }
  };

  const openPermission = async (role: SysRole) => {
    setSelectedRole(role);
    setPermissionVisible(true);
    setPermissionLoading(true);
    try {
      const assignment = await getRoleMenus(role.id);
      setCheckedKeys(assignment.menuIds || []);
    } catch (e: any) {
      message.error(e.message);
      setPermissionVisible(false);
    } finally {
      setPermissionLoading(false);
    }
  };

  const savePermission = async () => {
    if (!selectedRole) return;
    setPermissionLoading(true);
    try {
      await assignRoleMenus(selectedRole.id, checkedKeys.map(Number));
      message.success(`已更新“${selectedRole.roleName}”的菜单和按钮权限`);
      setPermissionVisible(false);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setPermissionLoading(false);
    }
  };

  const handleDelete = (role: SysRole) => {
    Modal.confirm({
      title: '确认删除角色？',
      content: `角色“${role.roleName}”删除后，其菜单及 API 权限配置也会被清除。`,
      okText: '删除',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await deleteRole(role.id);
          message.success('角色已删除');
          load();
        } catch (e: any) { message.error(e.message); }
      },
    });
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '角色编码', dataIndex: 'roleCode', render: (value: string) => <Tag color="blue">{value}</Tag> },
    { title: '角色名称', dataIndex: 'roleName' },
    { title: '类型', dataIndex: 'roleType', render: (value?: string) => value || '-' },
    { title: '描述', dataIndex: 'description', render: (value?: string) => value || '-' },
    { title: '状态', dataIndex: 'status', render: (value: number) => <Tag color={value === 1 ? 'green' : 'red'}>{value === 1 ? '启用' : '禁用'}</Tag> },
    {
      title: '操作', key: 'action', width: 170,
      render: (_: unknown, role: SysRole) => (
        <Space size={0}>
          <Permission perm={['USER', 'update']} menuCode="system:role:configure"><Button type="link" icon={<SafetyCertificateOutlined />} onClick={() => openPermission(role)}>配置菜单权限</Button></Permission>
          <Permission perm={['USER', 'delete']} menuCode="system:role:delete">
            <Button type="link" danger onClick={() => handleDelete(role)}>删除</Button>
          </Permission>
        </Space>
      ),
    },
  ];

  return (
    <Card title="角色管理" extra={<Permission perm={['USER', 'create']} menuCode="system:role:create"><Button type="primary" icon={<PlusOutlined />} onClick={() => { createForm.setFieldsValue({ roleType: 'BUSINESS' }); setCreateVisible(true); }}>新建角色</Button></Permission>}>
      <Button icon={<ReloadOutlined />} onClick={load} style={{ marginBottom: 16 }}>刷新</Button>
      <Table columns={columns} dataSource={roles} rowKey="id" loading={loading} />

      <Modal title="新建角色" open={createVisible} onOk={handleCreate} onCancel={() => { setCreateVisible(false); createForm.resetFields(); }}>
        <Form form={createForm} layout="vertical">
          <Form.Item name="roleCode" label="角色编码" rules={[{ required: true, message: '请输入角色编码' }]}><Input placeholder="如 RISK_OFFICER" /></Form.Item>
          <Form.Item name="roleName" label="角色名称" rules={[{ required: true, message: '请输入角色名称' }]}><Input /></Form.Item>
          <Form.Item name="roleType" label="角色类型" rules={[{ required: true, message: '请选择角色类型' }]}>
            <Select options={[
              { label: '业务角色', value: 'BUSINESS' },
              { label: '风险控制', value: 'RISK_CONTROL' },
              { label: '配置经办', value: 'CONFIG_MAKER' },
              { label: '配置复核', value: 'CONFIG_CHECKER' },
              { label: '运营管理', value: 'OPS' },
              { label: '审计合规', value: 'AUDIT' },
              { label: '系统管理', value: 'SYSTEM' },
            ]} />
          </Form.Item>
          <Form.Item name="description" label="描述"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>

      <Modal
        title={`配置菜单权限${selectedRole ? ` - ${selectedRole.roleName}` : ''}`}
        open={permissionVisible}
        onOk={savePermission}
        confirmLoading={permissionLoading}
        onCancel={() => setPermissionVisible(false)}
        width={680}
      >
        <p style={{ color: '#666' }}>勾选该角色可见的目录、菜单和按钮。勾选子项时会自动包含父级，保存后用户刷新或重新登录即可生效。</p>
        <Tree
          checkable
          selectable={false}
          defaultExpandAll
          disabled={permissionLoading}
          checkedKeys={checkedKeys}
          onCheck={(keys) => setCheckedKeys(keys as React.Key[])}
          treeData={treeData}
        />
      </Modal>
    </Card>
  );
};

export default RoleList;
