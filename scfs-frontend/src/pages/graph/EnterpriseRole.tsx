/**
 * 企业角色页 - 简化实现
 */
import React, { useState } from 'react';
import { Card, Input, Button, Empty, Descriptions, Tag, message } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { pageEnterprises, getEnterpriseRole } from '@/api/graph';
import { maskName } from '@/utils';

const EnterpriseRole: React.FC = () => {
  const [keyword, setKeyword] = useState('');
  const [role, setRole] = useState<any>(null);

  const handleSearch = async () => {
    if (!keyword) return;
    try {
      const result = await pageEnterprises({ page: 1, size: 1, keyword });
      if (!result.list?.length) {
        message.warning('未找到企业');
        return;
      }
      const r = await getEnterpriseRole(result.list[0].id);
      setRole(r);
    } catch (e: any) {
      message.error(e.message);
    }
  };

  return (
    <Card title="企业角色识别" extra={
      <Input.Search placeholder="企业名称/USCC" style={{ width: 240 }} prefix={<SearchOutlined />}
        onChange={(e) => setKeyword(e.target.value)} onSearch={handleSearch} />
    }>
      {role ? (
        <Descriptions bordered column={2}>
          <Descriptions.Item label="企业ID">{role.enterpriseId}</Descriptions.Item>
          <Descriptions.Item label="角色"><Tag color="blue">{role.role}</Tag></Descriptions.Item>
          <Descriptions.Item label="核心企业ID">{role.coreEnterpriseId || '-'}</Descriptions.Item>
          <Descriptions.Item label="合作年限">{role.coopDurationYears || 0} 年</Descriptions.Item>
          <Descriptions.Item label="合作企业数">{role.coopEnterpriseCount || 0}</Descriptions.Item>
          <Descriptions.Item label="影响力等级">{role.influenceLevel || '-'}</Descriptions.Item>
          <Descriptions.Item label="信誉等级">{role.credibilityLevel || '-'}</Descriptions.Item>
          <Descriptions.Item label="计算时间">{role.calculatedAt || '-'}</Descriptions.Item>
        </Descriptions>
      ) : (
        <Empty description="请搜索企业查看角色信息" />
      )}
    </Card>
  );
};

export default EnterpriseRole;
