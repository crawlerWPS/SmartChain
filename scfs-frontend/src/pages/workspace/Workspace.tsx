/**
 * 工作台 - 我的待办 + 运营统计
 */
import React, { useEffect, useState } from 'react';
import { Button, Card, Row, Col, Statistic, Table, Typography, message } from 'antd';
import { AuditOutlined, CheckCircleOutlined, ClockCircleOutlined, WarningOutlined } from '@ant-design/icons';
import { history } from '@umijs/max';
import { useModel } from '@umijs/max';
import { pageApplications } from '@/api/application';
import { can } from '@/access/access';
import { ApplicationStatusTag } from '@/components/common/StatusTag';
import { formatDate, formatAmount } from '@/utils';
import { ApplicationStatus } from '@/types';

const { Title } = Typography;

const Workspace: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;
  const canViewApplications = can('VERIFY', 'view');
  const canViewRules = can('RULE', 'view');
  const [list, setList] = useState<any[]>([]);
  const [stats, setStats] = useState({ pending: 0, approved: 0, rejected: 0, abnormal: 0 });

  const loadData = async () => {
    try {
      const result = await pageApplications({ page: 1, size: 10 });
      const items = result.list || [];
      setList(items);
      setStats({
        pending: items.filter((x) => [ApplicationStatus.SUBMITTED, ApplicationStatus.MATERIAL_REVIEW, ApplicationStatus.PREAUDIT, ApplicationStatus.VERIFYING].includes(x.status)).length,
        approved: items.filter((x) => x.status === ApplicationStatus.APPROVED).length,
        rejected: items.filter((x) => x.status === ApplicationStatus.REJECTED).length,
        abnormal: items.filter((x) => [ApplicationStatus.OCR_FAILED, ApplicationStatus.PREAUDIT_FAILED, ApplicationStatus.VERIFY_FAILED].includes(x.status)).length,
      });
    } catch (e: any) {
      message.error(e.message || '数据加载失败');
    }
  };

  useEffect(() => {
    if (canViewApplications) {
      loadData();
    }
  }, []);

  const columns = [
    { title: '申请编号', dataIndex: 'appNo', key: 'appNo' },
    { title: '企业ID', dataIndex: 'enterpriseId', key: 'enterpriseId' },
    { title: '融资金额', dataIndex: 'financingAmount', key: 'financingAmount', render: (v: number) => formatAmount(v) },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: ApplicationStatus) => <ApplicationStatusTag status={v} /> },
    { title: '提交时间', dataIndex: 'submittedAt', key: 'submittedAt', render: (v: string) => formatDate(v) },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: any) => (
        <a onClick={() => history.push(`/audit/application?appId=${record.id}`)}>详情</a>
      ),
    },
  ];

  return (
    <div>
      <Title level={4}>工作台 - 欢迎，{currentUser?.realName}</Title>

      {canViewApplications ? <>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic title="待办申请" value={stats.pending} prefix={<ClockCircleOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="已通过" value={stats.approved} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#3f8600' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="已驳回" value={stats.rejected} prefix={<AuditOutlined />} valueStyle={{ color: '#cf1322' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="异常申请" value={stats.abnormal} prefix={<WarningOutlined />} valueStyle={{ color: '#faad14' }} />
          </Card>
        </Col>
      </Row>

      <Card title="最近申请" extra={<a onClick={() => history.push('/audit/application')}>查看全部</a>}>
        <Table columns={columns} dataSource={list} rowKey="id" pagination={false} />
      </Card>
      </> : (
        <Card title="我的工作台">
          <Typography.Paragraph type="secondary">
            当前角色没有融资申请查看权限，工作台不会加载融资申请数据。
          </Typography.Paragraph>
          {canViewRules && (
            <Button type="primary" onClick={() => history.push('/rule/list')}>进入规则配置</Button>
          )}
        </Card>
      )}
    </div>
  );
};

export default Workspace;
