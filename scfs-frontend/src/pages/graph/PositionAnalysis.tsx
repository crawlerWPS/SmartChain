/**
 * 位置分析页
 */
import React, { useState } from 'react';
import { Card, Input, Button, Empty, Descriptions, Tag, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { pageEnterprises, getPositionAnalysis } from '@/api/graph';

const PositionAnalysis: React.FC = () => {
  const [keyword, setKeyword] = useState('');
  const [data, setData] = useState<any>(null);

  const handleSearch = async () => {
    if (!keyword) return;
    try {
      const result = await pageEnterprises({ page: 1, size: 1, keyword });
      if (!result.list?.length) {
        message.warning('未找到企业');
        return;
      }
      const r = await getPositionAnalysis(result.list[0].id);
      setData(r);
    } catch (e: any) {
      message.error(e.message);
    }
  };

  return (
    <Card title="企业位置分析" extra={
      <Input.Search placeholder="企业名称/USCC" style={{ width: 240 }} prefix={<SearchOutlined />}
        onChange={(e) => setKeyword(e.target.value)} onSearch={handleSearch} />
    }>
      {data ? (
        <Descriptions bordered column={2}>
          <Descriptions.Item label="是否在核心链">
            <Tag color={data.inCoreChain ? 'green' : 'default'}>{data.inCoreChain ? '是' : '否'}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="距核心企业层级">{data.distanceToCore || 0}</Descriptions.Item>
          <Descriptions.Item label="上游稳定">
            <Tag color={data.upstreamStable ? 'green' : 'red'}>{data.upstreamStable ? '稳定' : '不稳定'}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="下游稳定">
            <Tag color={data.downstreamStable ? 'green' : 'red'}>{data.downstreamStable ? '稳定' : '不稳定'}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="可靠性">{data.credibility || '-'}</Descriptions.Item>
          <Descriptions.Item label="分析原因">{data.credibilityReason || '-'}</Descriptions.Item>
        </Descriptions>
      ) : (
        <Empty description="请搜索企业查看位置分析" />
      )}
    </Card>
  );
};

export default PositionAnalysis;
