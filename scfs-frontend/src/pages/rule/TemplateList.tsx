/**
 * 材料清单模板维护页
 */
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Modal, Form, Select, message, Popconfirm, Space } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { listTemplates, createTemplate, updateTemplate, deleteTemplate } from '@/api/risk';
import { Permission } from '@/components/common/Permission';
import { formatDate } from '@/utils';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';

const TemplateList: React.FC = () => {
  const [list, setList] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [createVisible, setCreateVisible] = useState(false);
  const [editing, setEditing] = useState<any>();
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const dictionary = useCodeDictionary();

  const load = async () => {
    setLoading(true);
    try {
      const result = await listTemplates();
      setList(result || []);
    } catch (e: any) { message.error(e.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      await createTemplate({ ...values, requiredMaterials: values.requiredMaterials });
      message.success('模板已创建并生效');
      setCreateVisible(false);
      createForm.resetFields();
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const openEdit = (record: any) => {
    setEditing(record);
    editForm.setFieldsValue(record);
  };

  const handleUpdate = async () => {
    const values = await editForm.validateFields();
    try {
      await updateTemplate(editing.id, values);
      message.success('修改成功，模板已生效');
      setEditing(undefined);
      editForm.resetFields();
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const handleDelete = async (id: number) => {
    try { await deleteTemplate(id); message.success('删除成功'); load(); }
    catch (e: any) { message.error(e.message); }
  };

  const columns = [
    { title: '业务类型', dataIndex: 'businessType', key: 'businessType', render: (v: string) => dictionary.label('BUSINESS_TYPE', v) },
    { title: '必需材料', dataIndex: 'requiredMaterials', key: 'requiredMaterials', render: (v: string[]) => v?.map((x) => dictionary.label('MATERIAL_TYPE', x)).join('、') },
    { title: '版本', dataIndex: 'version', key: 'version', render: (v: number) => `v${v}` },
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => formatDate(v) },
    { title: '操作', key: 'action', render: (_: any, r: any) => (
      <Space>
        <Permission perm={['RULE', 'update']}>
          <a onClick={() => openEdit(r)}><EditOutlined /> 修改</a>
        </Permission>
        <Permission perm={['RULE', 'delete']}>
          <Popconfirm title="确认删除该材料清单模板？" description="删除后不可恢复" onConfirm={() => handleDelete(r.id)}>
            <a style={{ color: '#ff4d4f' }}><DeleteOutlined /> 删除</a>
          </Popconfirm>
        </Permission>
      </Space>
    )},
  ];

  const materialOptions = dictionary.options('MATERIAL_TYPE');

  return (
    <Card title="材料清单模板" extra={
      <Permission perm={['RULE', 'create']}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>新建模板</Button>
      </Permission>
    }>
      <Button icon={<ReloadOutlined />} onClick={load} style={{ marginBottom: 16 }}>刷新</Button>
      <Table columns={columns} dataSource={list} rowKey="id" loading={loading} />

      <Modal title="新建材料清单模板" open={createVisible} onOk={handleCreate} onCancel={() => { setCreateVisible(false); createForm.resetFields(); }}>
        <Form form={createForm} layout="vertical">
          <Form.Item name="businessType" label="业务类型" rules={[{ required: true }]}>
            <Select options={dictionary.options('BUSINESS_TYPE')} />
          </Form.Item>
          <Form.Item name="requiredMaterials" label="必需材料" rules={[{ required: true }]}>
            <Select mode="multiple" options={materialOptions} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="修改材料清单模板" open={!!editing} onOk={handleUpdate} onCancel={() => { setEditing(undefined); editForm.resetFields(); }}>
        <Form form={editForm} layout="vertical">
          <Form.Item name="businessType" label="业务类型" rules={[{ required: true }]}>
            <Select options={dictionary.options('BUSINESS_TYPE')} />
          </Form.Item>
          <Form.Item name="requiredMaterials" label="必需材料" rules={[{ required: true }]}>
            <Select mode="multiple" options={materialOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default TemplateList;
