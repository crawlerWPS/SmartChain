/**
 * 材料清单模板页 - 双岗
 */
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Tag, Modal, Form, Select, message } from 'antd';
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { listTemplates, createTemplate, submitTemplate, approveTemplate, rejectTemplate } from '@/api/risk';
import { Permission } from '@/components/common/Permission';
import { formatDate } from '@/utils';
import { BUSINESS_TYPE_MAP } from '@/utils';

const TemplateList: React.FC = () => {
  const [list, setList] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [createVisible, setCreateVisible] = useState(false);
  const [createForm] = Form.useForm();

  const load = async () => {
    setLoading(true);
    try {
      const result = await listTemplates();
      setList(result || []);
    } catch (e: any) { message.error(e.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const handleSubmit = async (id: number) => {
    try { await submitTemplate(id); message.success('已提交'); load(); }
    catch (e: any) { message.error(e.message); }
  };
  const handleApprove = async (id: number) => {
    try { await approveTemplate(id); message.success('已通过'); load(); }
    catch (e: any) { message.error(e.message); }
  };
  const handleReject = async (id: number) => {
    const reason = window.prompt('驳回原因');
    if (!reason) return;
    try { await rejectTemplate(id, reason); message.success('已驳回'); load(); }
    catch (e: any) { message.error(e.message); }
  };

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      await createTemplate({ ...values, requiredMaterials: values.requiredMaterials });
      message.success('已创建草稿');
      setCreateVisible(false);
      createForm.resetFields();
      load();
    } catch (e: any) { message.error(e.message); }
  };

  const columns = [
    { title: '业务类型', dataIndex: 'businessType', key: 'businessType', render: (v: string) => BUSINESS_TYPE_MAP[v] || v },
    { title: '必需材料', dataIndex: 'requiredMaterials', key: 'requiredMaterials', render: (v: string[]) => v?.join(', ') },
    { title: '版本', dataIndex: 'version', key: 'version', render: (v: number) => `v${v}` },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: string) => {
      const color = v === 'ENABLED' ? 'green' : v === 'PENDING' ? 'orange' : 'default';
      return <Tag color={color}>{v}</Tag>;
    }},
    { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => formatDate(v) },
    { title: '操作', key: 'action', render: (_: any, r: any) => (
      <>
        {r.status === 'DRAFT' && <Permission perm={['RULE', 'create']}><a onClick={() => handleSubmit(r.id)}>提交</a></Permission>}
        {r.status === 'PENDING' && (
          <Permission perm={['RULE', 'approve']}>
            <a onClick={() => handleApprove(r.id)} style={{ marginRight: 8 }}>通过</a>
            <a style={{ color: '#ff4d4f' }} onClick={() => handleReject(r.id)}>驳回</a>
          </Permission>
        )}
      </>
    )},
  ];

  const materialOptions = ['CONTRACT', 'ORDER', 'INVOICE', 'LOGISTICS_DOC', 'ACCEPTANCE_CERT', 'PAYMENT_VOUCHER', 'BUSINESS_LICENSE'].map((v) => ({ label: v, value: v }));

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
            <Select options={Object.entries(BUSINESS_TYPE_MAP).map(([k, v]) => ({ label: v, value: k }))} />
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
