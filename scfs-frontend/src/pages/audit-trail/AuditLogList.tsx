/**
 * 审计日志查询页
 */
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Space, Input, Select, DatePicker, Tag, message, Modal, Descriptions } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { pageAuditLogs } from '@/api/audit';
import { Permission } from '@/components/common/Permission';
import { ExportXlsx } from '@/components/export/ExportBtn';
import { formatDate } from '@/utils';
import dayjs from 'dayjs';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';

const { RangePicker } = DatePicker;

const AuditLogList: React.FC = () => {
  const [list, setList] = useState<any[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState<any>({ page: 1, size: 20, module: undefined, action: undefined, userId: undefined, startTime: undefined, endTime: undefined });
  const [detail, setDetail] = useState<any>(null);
  const dictionary = useCodeDictionary();

  const load = async () => {
    setLoading(true);
    try {
      const result = await pageAuditLogs(query);
      setList(result.list || []);
      setTotal(result.total);
    } catch (e: any) { message.error(e.message); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, [query.page, query.size]);

  const handleExportXlsx = () => {
    const data = list.map((x) => [
      x.id,
      x.username,
      dictionary.label('PERMISSION_MODULE', x.module),
      dictionary.label('AUDIT_ACTION', x.action),
      `${dictionary.label('AUDIT_TARGET_TYPE', x.targetType)}#${x.targetId || '-'}`,
      x.ipAddress || '-',
      formatDate(x.createdAt),
    ]);
    return [{ name: '审计日志', data: [['ID', '用户', '模块', '操作', '对象', 'IP', '时间'], ...data] }];
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '用户', dataIndex: 'username', key: 'username' },
    { title: '模块', dataIndex: 'module', key: 'module', render: (v: string) => <Tag color="blue">{dictionary.label('PERMISSION_MODULE', v)}</Tag> },
    { title: '操作', dataIndex: 'action', key: 'action', render: (v: string) => dictionary.label('AUDIT_ACTION', v) },
    { title: '对象', key: 'target', render: (_: any, r: any) => `${dictionary.label('AUDIT_TARGET_TYPE', r.targetType)}#${r.targetId || '-'}` },
    { title: 'IP', dataIndex: 'ipAddress', key: 'ipAddress' },
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => formatDate(v) },
    { title: '操作', key: 'action_btn', render: (_: any, r: any) => <a onClick={() => setDetail(r)}>详情</a> },
  ];

  return (
    <Card title="审计日志查询" extra={
      <Space>
        <Permission perm={['AUDIT', 'export']} menuCode="audit:export">
          <ExportXlsx
            filename={`audit_logs_${Date.now()}`}
            sheets={handleExportXlsx()}
            buttonText="导出 Excel"
          />
        </Permission>
        <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
      </Space>
    }>
      <Space style={{ marginBottom: 16 }}>
        <Select placeholder="模块" allowClear style={{ width: 160 }} onChange={(v) => setQuery({ ...query, module: v, page: 1 })} options={dictionary.options('PERMISSION_MODULE')} />
        <Select placeholder="操作" allowClear style={{ width: 160 }} onChange={(v) => setQuery({ ...query, action: v, page: 1 })} options={dictionary.options('AUDIT_ACTION')} />
        <Input placeholder="用户ID" allowClear style={{ width: 100 }} onChange={(e) => setQuery({ ...query, userId: e.target.value ? Number(e.target.value) : undefined, page: 1 })} />
        <RangePicker showTime onChange={(v, str) => setQuery({ ...query, startTime: str[0], endTime: str[1], page: 1 })} />
      </Space>

      <Table columns={columns} dataSource={list} rowKey="id" loading={loading}
        pagination={{ current: query.page, pageSize: query.size, total, onChange: (p, s) => setQuery({ ...query, page: p, size: s }) }} />

      <Modal title="审计详情" open={!!detail} footer={null} onCancel={() => setDetail(null)} width={640}>
        {detail && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="ID">{detail.id}</Descriptions.Item>
            <Descriptions.Item label="用户">{detail.username} (#{detail.userId})</Descriptions.Item>
            <Descriptions.Item label="模块">{dictionary.label('PERMISSION_MODULE', detail.module)}</Descriptions.Item>
            <Descriptions.Item label="操作">{dictionary.label('AUDIT_ACTION', detail.action)}</Descriptions.Item>
            <Descriptions.Item label="对象类型">{dictionary.label('AUDIT_TARGET_TYPE', detail.targetType)}</Descriptions.Item>
            <Descriptions.Item label="对象ID">{detail.targetId || '-'}</Descriptions.Item>
            <Descriptions.Item label="IP">{detail.ipAddress || '-'}</Descriptions.Item>
            <Descriptions.Item label="时间">{formatDate(detail.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="详情">{JSON.stringify(detail.detail, null, 2)}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </Card>
  );
};

export default AuditLogList;
