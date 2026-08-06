/**
 * 企业位置分析页 - 列表展示 + 搜索过滤
 */
import React, { useEffect, useState } from 'react';
import { Card, Input, Table, Tag, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { listPositionAnalyses } from '@/api/graph';
import type { EnterprisePositionAnalysis } from '@/types';

const PositionAnalysisPage: React.FC = () => {
  const [keyword, setKeyword] = useState('');
  const [list, setList] = useState<EnterprisePositionAnalysis[]>([]);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const result = await listPositionAnalyses();
      setList(result || []);
    } catch (e: any) {
      message.error(e.message || '加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const filtered = list.filter(
    (r) =>
      !keyword ||
      r.enterpriseName?.toLowerCase().includes(keyword.toLowerCase()) ||
      String(r.enterpriseId).includes(keyword),
  );

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '企业ID', dataIndex: 'enterpriseId', key: 'enterpriseId', width: 80 },
    { title: '企业名称', dataIndex: 'enterpriseName', key: 'enterpriseName' },
    {
      title: '核心链路',
      dataIndex: 'inCoreChain',
      key: 'inCoreChain',
      render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? '是' : '否'}</Tag>,
    },
    { title: '距核心层级', dataIndex: 'distanceToCore', key: 'distanceToCore' },
    {
      title: '上游稳定',
      dataIndex: 'upstreamStable',
      key: 'upstreamStable',
      render: (v: boolean) => <Tag color={v ? 'green' : 'red'}>{v ? '稳定' : '不稳定'}</Tag>,
    },
    {
      title: '下游稳定',
      dataIndex: 'downstreamStable',
      key: 'downstreamStable',
      render: (v: boolean) => <Tag color={v ? 'green' : 'red'}>{v ? '稳定' : '不稳定'}</Tag>,
    },
    {
      title: '可靠性',
      dataIndex: 'credibility',
      key: 'credibility',
      render: (v: string) => {
        const color = v === 'HIGH' ? 'green' : v === 'MID' ? 'orange' : 'red';
        return <Tag color={color}>{v}</Tag>;
      },
    },
    { title: '分析原因', dataIndex: 'credibilityReason', key: 'credibilityReason', ellipsis: true },
  ];

  return (
    <Card
      title="企业位置分析"
      extra={
        <Input.Search
          placeholder="搜索企业名称/ID"
          style={{ width: 240 }}
          prefix={<SearchOutlined />}
          allowClear
          onChange={(e) => setKeyword(e.target.value)}
        />
      }
    >
      <Table
        columns={columns}
        dataSource={filtered}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10 }}
      />
    </Card>
  );
};

export default PositionAnalysisPage;
