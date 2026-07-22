/**
 * 异常预警列表页
 */
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, Tag, message, Modal, Descriptions } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { listAbnormals, resolveAbnormal } from '@/api/graph';
import { formatDate } from '@/utils';

const AbnormalList: React.FC = () => {
  const [list, setList] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<any>(null);

  const load = async () => {
    setLoading(true);
    try {
      const result = await listAbnormals();
      setList(result || []);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleResolve = async (id: number) => {
    Modal.confirm({
      title: '确认解除该异常？',
      onOk: async () => {
        await resolveAbnormal(id);
        message.success('已解除');
        load();
      },
    });
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '企业ID', dataIndex: 'enterpriseId', key: 'enterpriseId' },
    { title: '异常类型', dataIndex: 'abnormalType', key: 'abnormalType' },
    { title: '严重度', dataIndex: 'severity', key: 'severity', render: (v: string) => {
      const color = v === 'DANGER' ? 'red' : v === 'WARN' ? 'orange' : 'blue';
      return <Tag color={color}>{v}</Tag>;
    }},
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: string) => <Tag color={v === 'OPEN' ? 'red' : 'green'}>{v === 'OPEN' ? '未处理' : '已解除'}</Tag> },
    { title: '检测时间', dataIndex: 'detectedAt', key: 'detectedAt', render: (v: string) => formatDate(v) },
    { title: '操作', key: 'action', render: (_: any, r: any) => (
      <>
        <a onClick={() => setDetail(r)} style={{ marginRight: 12 }}>详情</a>
        {r.status === 'OPEN' && <a onClick={() => handleResolve(r.id)}>解除</a>}
      </>
    ) },
  ];

  return (
    <Card title="异常关系预警列表" extra={<Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>}>
      <Table columns={columns} dataSource={list} rowKey="id" loading={loading} />
      <Modal title="异常详情" open={!!detail} footer={null} onCancel={() => setDetail(null)}>
        {detail && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="企业ID">{detail.enterpriseId}</Descriptions.Item>
            <Descriptions.Item label="异常类型">{detail.abnormalType}</Descriptions.Item>
            <Descriptions.Item label="严重度">{detail.severity}</Descriptions.Item>
            <Descriptions.Item label="描述">{detail.description}</Descriptions.Item>
            <Descriptions.Item label="证据">{JSON.stringify(detail.evidence, null, 2)}</Descriptions.Item>
            <Descriptions.Item label="检测时间">{formatDate(detail.detectedAt)}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </Card>
  );
};

export default AbnormalList;
