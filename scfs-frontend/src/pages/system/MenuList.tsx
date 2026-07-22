/**
 * 菜单管理页 - 树形展示
 */
import React, { useEffect, useState } from 'react';
import { Card, Tree, Button, Tag, message, Modal, Form, Input, Select, InputNumber, Space } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { getMenuTree, createMenu, updateMenu, deleteMenu } from '@/api/system';
import type { SysMenu } from '@/types';

const MenuList: React.FC = () => {
  const [tree, setTree] = useState<SysMenu[]>([]);
  const [loading, setLoading] = useState(false);
  const [editVisible, setEditVisible] = useState(false);
  const [editForm] = Form.useForm();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [parentId, setParentId] = useState<number | null>(null);

  const buildTree = (list: SysMenu[]): SysMenu[] => {
    const map = new Map<number, SysMenu & { children?: SysMenu[] }>();
    list.forEach((m) => map.set(m.id, { ...m, children: [] }));
    const roots: SysMenu[] = [];
    map.forEach((node) => {
      if (node.parentId && map.has(node.parentId)) {
        (map.get(node.parentId) as any).children.push(node);
      } else {
        roots.push(node);
      }
    });
    return roots;
  };

  const load = async () => {
    setLoading(true);
    try {
      const result = await getMenuTree();
      setTree(buildTree(result || []));
    } catch (e: any) { message.error(e.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const handleAdd = (parent?: SysMenu) => {
    setEditingId(null);
    setParentId(parent?.id || null);
    editForm.resetFields();
    editForm.setFieldsValue({ parentId: parent?.id, menuType: 'MENU', visible: 1, status: 1, sort: 1 });
    setEditVisible(true);
  };

  const handleEdit = (record: SysMenu) => {
    setEditingId(record.id);
    editForm.setFieldsValue(record);
    setEditVisible(true);
  };

  const handleSave = async () => {
    const values = await editForm.validateFields();
    try {
      if (editingId) {
        await updateMenu(editingId, values);
        message.success('已更新');
      } else {
        await createMenu(values);
        message.success('已创建');
      }
      setEditVisible(false);
      editForm.resetFields();
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: '确认删除菜单？',
      content: '删除后子菜单也将一并删除',
      onOk: async () => {
        try {
          await deleteMenu(id);
          message.success('已删除');
          load();
        } catch (e: any) { message.error(e.message); }
      },
    });
  };

  const renderNode = (node: any) => ({
    key: node.id,
    title: (
      <Space>
        <span>{node.menuName}</span>
        <Tag color={node.menuType === 'DIRECTORY' ? 'blue' : node.menuType === 'MENU' ? 'green' : 'orange'}>{node.menuType}</Tag>
        {node.path && <span style={{ color: '#999' }}>{node.path}</span>}
        <a onClick={() => handleAdd(node)}><PlusOutlined /></a>
        <a onClick={() => handleEdit(node)}><EditOutlined /></a>
        <a style={{ color: '#ff4d4f' }} onClick={() => handleDelete(node.id)}><DeleteOutlined /></a>
      </Space>
    ),
    children: node.children?.map(renderNode),
  });

  return (
    <Card title="菜单管理" extra={
      <Space>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => handleAdd()}>新增根菜单</Button>
        <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
      </Space>
    }>
      <Tree treeData={tree.map(renderNode)} defaultExpandAll />
      <Modal title={editingId ? '编辑菜单' : '新增菜单'} open={editVisible} onOk={handleSave} onCancel={() => { setEditVisible(false); editForm.resetFields(); }} width={640}>
        <Form form={editForm} layout="vertical">
          <Form.Item name="parentId" label="父菜单ID"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="menuName" label="菜单名称" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="menuCode" label="菜单编码" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="menuType" label="类型" rules={[{ required: true }]}>
            <Select options={[
              { label: '目录', value: 'DIRECTORY' },
              { label: '菜单', value: 'MENU' },
              { label: '按钮', value: 'BUTTON' },
            ]} />
          </Form.Item>
          <Form.Item name="path" label="路由路径"><Input placeholder="/workspace" /></Form.Item>
          <Form.Item name="component" label="组件路径"><Input placeholder="@/pages/workspace/Workspace" /></Form.Item>
          <Form.Item name="permission" label="权限标识"><Input placeholder="rule:approve" /></Form.Item>
          <Form.Item name="icon" label="图标"><Input placeholder="SettingOutlined" /></Form.Item>
          <Form.Item name="sort" label="排序"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="visible" label="是否可见"><InputNumber min={0} max={1} /></Form.Item>
          <Form.Item name="status" label="状态"><InputNumber min={0} max={1} /></Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default MenuList;
