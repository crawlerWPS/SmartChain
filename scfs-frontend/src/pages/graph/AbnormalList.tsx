/**
 * 异常预警列表页
 */
import React, { useEffect, useState } from 'react';
import { Card, Table, Button, message, Modal, Descriptions } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { listAbnormals, resolveAbnormal } from '@/api/graph';
import { formatDate } from '@/utils';
import { CodeTag } from '@/components/common/CodeTag';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';

const AbnormalList: React.FC = () => {
  const [list, setList] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<any>(null);
  const dictionary = useCodeDictionary();

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
    { title: '企业ID', dataIndex: 'enterpriseId', key: 'enterpriseId', width: 80 },
    { title: '企业名称', dataIndex: 'enterpriseName', key: 'enterpriseName' },
    { title: '异常类型', dataIndex: 'abnormalType', key: 'abnormalType', render: (v: string) => dictionary.label('ABNORMAL_TYPE', v) },
    { title: '严重度', dataIndex: 'severity', key: 'severity', render: (v: string) => <CodeTag type="ABNORMAL_SEVERITY" code={v} /> },
    { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: string) => <CodeTag type="ABNORMAL_STATUS" code={v} /> },
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
            <Descriptions.Item label="异常类型">{dictionary.label('ABNORMAL_TYPE', detail.abnormalType)}</Descriptions.Item>
            <Descriptions.Item label="严重度">{dictionary.label('ABNORMAL_SEVERITY', detail.severity)}</Descriptions.Item>
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
