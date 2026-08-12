/**
 * 风险画像页 - 雷达图（用 antd Progress 简化展示三维评分）
 */
import React, { useState, useEffect } from 'react';
import { Card, Button, Descriptions, Progress, Row, Col, Tag, message, Empty } from 'antd';
import { history, useParams } from '@umijs/max';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { getRiskProfileByApplication, calculateRiskScore } from '@/api/risk';
import { RiskLevelTag } from '@/components/common/StatusTag';
import { formatDate } from '@/utils';
import { RiskLevel } from '@/types';

const RiskProfile: React.FC = () => {
  const params = useParams();
  const appId = Number(params?.appId || 0);
  const [profile, setProfile] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    if (!appId) return;
    try {
      const r = await getRiskProfileByApplication(appId);
      setProfile(r);
    } catch (e) {
      setProfile(null);
    }
  };

  useEffect(() => { load(); }, [appId]);

  const handleScore = async () => {
    setLoading(true);
    try {
      const r = await calculateRiskScore(appId);
      setProfile(r);
      message.success('评分完成');
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title={<div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <Button icon={<ArrowLeftOutlined />} onClick={() => history.push(`/audit/application/detail?appId=${appId}`)}>
        返回申请详情
      </Button>
      <span>风险画像 - 申请 #{appId}</span>
    </div>} extra={<Button type="primary" onClick={handleScore} loading={loading}>执行评分</Button>}>
      {profile ? (
        <>
          <Descriptions bordered column={2}>
            <Descriptions.Item label="版本">v{profile.version}</Descriptions.Item>
            <Descriptions.Item label="风险等级"><RiskLevelTag level={profile.riskLevel as RiskLevel} /></Descriptions.Item>
            <Descriptions.Item label="综合得分">
              <span style={{ fontSize: 18, fontWeight: 'bold' }}>{profile.overallScore}</span>
            </Descriptions.Item>
            <Descriptions.Item label="生成时间">{formatDate(profile.generatedAt)}</Descriptions.Item>
            <Descriptions.Item label="内容哈希" span={2}>{profile.contentHash?.slice(0, 24) || '-'}...</Descriptions.Item>
          </Descriptions>

          <Row gutter={24} style={{ marginTop: 24 }}>
            <Col span={8}>
              <Card size="small" title="供应链得分">
                <Progress type="dashboard" percent={profile.supplyChainScore} />
              </Card>
            </Col>
            <Col span={8}>
              <Card size="small" title="交易稳定性得分">
                <Progress type="dashboard" percent={profile.transactionScore} />
              </Card>
            </Col>
            <Col span={8}>
              <Card size="small" title="材料质量得分">
                <Progress type="dashboard" percent={profile.materialScore} />
              </Card>
            </Col>
          </Row>

          {profile.riskReasons?.length > 0 && (
            <Card size="small" title="风险原因" style={{ marginTop: 16 }}>
              <ul>{profile.riskReasons.map((r: string, i: number) => <li key={i}>{r}</li>)}</ul>
            </Card>
          )}
          {profile.suggestions?.length > 0 && (
            <Card size="small" title="建议" style={{ marginTop: 12 }}>
              <ul>{profile.suggestions.map((s: string, i: number) => <li key={i}>{s}</li>)}</ul>
            </Card>
          )}
        </>
      ) : (
        <Empty description="尚未生成风险画像，请点击「执行评分」按钮" />
      )}
    </Card>
  );
};

export default RiskProfile;
