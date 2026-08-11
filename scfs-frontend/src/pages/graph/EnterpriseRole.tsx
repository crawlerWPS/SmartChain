/**
 * 企业角色识别页 - 列表展示 + 搜索过滤
 */
import React, { useEffect, useState } from 'react';
import { Card, Input, Table, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { listEnterpriseRoles } from '@/api/graph';
import type { EnterpriseRole } from '@/types';
import { CodeTag } from '@/components/common/CodeTag';

const EnterpriseRolePage: React.FC = () => {
  const [keyword, setKeyword] = useState('');
  const [list, setList] = useState<EnterpriseRole[]>([]);
  const [loading, setLoading] = useState(false);

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
  ];

  return (
    <Card
      title="企业角色识别"
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

export default EnterpriseRolePage;
