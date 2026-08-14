/**
 * 企业角色识别页 - 列表展示 + 搜索过滤
 */
import React, { useEffect, useState } from 'react';
import { Card, Input, Table, message, Button } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { history } from '@umijs/max';
import { listEnterpriseRoles, recalculateAnalysis } from '@/api/graph';
import type { EnterpriseRole } from '@/types';
import { CodeTag } from '@/components/common/CodeTag';

const EnterpriseRolePage: React.FC = () => {
  const [keyword, setKeyword] = useState('');
  const [list, setList] = useState<EnterpriseRole[]>([]);
  const [loading, setLoading] = useState(false);
  const [recalculating, setRecalculating] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const result = await listEnterpriseRoles();
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

  const handleRecalculate = async () => {
    setRecalculating(true);
    try {
      const result = await recalculateAnalysis();
      message.success(`${result.message}，已处理 ${result.calculatedCount} 家企业`);
      await load();
    } catch (e: any) {
      message.error(e.message || '预计算失败');
    } finally {
      setRecalculating(false);
    }
  };

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
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (v: string) => <CodeTag type="ENTERPRISE_ROLE" code={v} />,
    },
    { title: '核心企业ID', dataIndex: 'coreEnterpriseId', key: 'coreEnterpriseId' },
    { title: '合作年限', dataIndex: 'coopDurationYears', key: 'coopDurationYears' },
    { title: '合作企业数', dataIndex: 'coopEnterpriseCount', key: 'coopEnterpriseCount' },
    { title: '影响力', dataIndex: 'influenceLevel', key: 'influenceLevel', render: (v: string) => <CodeTag type="INFLUENCE_LEVEL" code={v} /> },
    { title: '信誉等级', dataIndex: 'credibilityLevel', key: 'credibilityLevel', render: (v: string) => <CodeTag type="CREDIBILITY_LEVEL" code={v} /> },
    { title: '操作', key: 'action', render: (_: unknown, record: EnterpriseRole) => <Button type="link" onClick={() => history.push(`/graph/relation?enterpriseId=${record.enterpriseId}`)}>查看图谱</Button> },
  ];

  return (
    <Card
      title="企业角色识别"
      extra={<>
        <Button loading={recalculating} onClick={handleRecalculate} style={{ marginRight: 12 }}>重新计算分析</Button>
        <Input.Search
          placeholder="搜索企业名称/ID"
          style={{ width: 240 }}
          prefix={<SearchOutlined />}
          allowClear
          onChange={(e) => setKeyword(e.target.value)}
        />
      </>}
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

export default EnterpriseRolePage;
