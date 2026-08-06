/**
 * 企业关系图谱页 - G6 画布
 * 默认展示全部企业关系图谱，支持按关键词搜索聚焦到单个企业
 */
import React, { useState } from 'react';
import { Card, Input, InputNumber, Space, Button, message } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import GraphCanvas from '@/components/graph/GraphCanvas';
import { pageEnterprises } from '@/api/graph';

const RelationGraph: React.FC = () => {
  const [keyword, setKeyword] = useState('');
  const [enterpriseId, setEnterpriseId] = useState<number | undefined>(undefined);
  const [level, setLevel] = useState(2);
  const [refreshKey, setRefreshKey] = useState(0);

  const handleSearch = async () => {
    if (!keyword) {
      // 关键词为空时，展示全部
      setEnterpriseId(undefined);
      setRefreshKey((k) => k + 1);
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

  const handleReset = () => {
    setKeyword('');
    setEnterpriseId(undefined);
    setRefreshKey((k) => k + 1);
  };

  return (
    <Card title="企业关系图谱" extra={
      <Space>
        <Input
          placeholder="企业名称/USCC（留空展示全部）"
          allowClear
          onPressEnter={handleSearch}
          onChange={(e) => setKeyword(e.target.value)}
          value={keyword}
          style={{ width: 260 }}
          prefix={<SearchOutlined />}
        />
        {enterpriseId ? (
          <>
            <span>层级：</span>
            <InputNumber min={1} max={2} value={level} onChange={(v) => setLevel(v || 2)} />
          </>
        ) : null}
        <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
      </Space>
    }>
      <GraphCanvas key={refreshKey} enterpriseId={enterpriseId} level={level} height={600} />
      <div style={{ marginTop: 16, color: '#999', fontSize: 12 }}>
        <p>说明：</p>
        <ul>
          <li>默认展示全部企业供应链关系图谱</li>
          <li>输入企业名称搜索可聚焦到该企业的 N 跳关系</li>
          <li>红色节点为核心企业，蓝色为上下游节点</li>
          <li>绿色线为供应关系，黄色线为采购关系</li>
          <li>支持拖拽、缩放、框选、图片导出</li>
        </ul>
      </div>
    </Card>
  );
};

export default RelationGraph;
