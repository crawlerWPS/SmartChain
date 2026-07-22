/**
 * 企业关系图谱页 - G6 画布
 */
import React, { useState } from 'react';
import { Card, Input, InputNumber, Space, message, Empty } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import GraphCanvas from '@/components/graph/GraphCanvas';
import { pageEnterprises } from '@/api/graph';

const RelationGraph: React.FC = () => {
  const [keyword, setKeyword] = useState('');
  const [enterpriseId, setEnterpriseId] = useState<number | null>(null);
  const [level, setLevel] = useState(2);

  const handleSearch = async () => {
    if (!keyword) {
      message.warning('请输入企业名称或统一社会信用代码');
      return;
    }
    try {
      const result = await pageEnterprises({ page: 1, size: 5, keyword });
      if (!result.list || result.list.length === 0) {
        message.warning('未找到匹配企业');
        return;
      }
      setEnterpriseId(result.list[0].id);
    } catch (e: any) {
      message.error(e.message);
    }
  };

  return (
    <Card title="企业关系图谱" extra={
      <Space>
        <Input
          placeholder="企业名称/USCC"
          allowClear
          onPressEnter={handleSearch}
          onChange={(e) => setKeyword(e.target.value)}
          style={{ width: 240 }}
          prefix={<SearchOutlined />}
        />
        <span>层级：</span>
        <InputNumber min={1} max={2} value={level} onChange={(v) => setLevel(v || 2)} />
      </Space>
    }>
      {enterpriseId ? (
        <GraphCanvas enterpriseId={enterpriseId} level={level} height={600} />
      ) : (
        <Empty description="请先搜索企业" />
      )}
      <div style={{ marginTop: 16, color: '#999', fontSize: 12 }}>
        <p>说明：</p>
        <ul>
          <li>红色节点为核心企业，蓝色为上下游节点</li>
          <li>绿色线为供应关系，黄色线为采购关系</li>
          <li>最大支持 2 层展开，防止性能爆炸</li>
          <li>支持拖拽、缩放、框选、图片导出</li>
        </ul>
      </div>
    </Card>
  );
};

export default RelationGraph;
