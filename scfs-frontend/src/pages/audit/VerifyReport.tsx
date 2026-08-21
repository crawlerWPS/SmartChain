/**
 * 核验报告页
 */
import React, { useState, useEffect } from 'react';
import { Card, Button, Descriptions, Tag, message, Empty, Typography, Row, Col, Statistic, Alert, Space, Divider } from 'antd';
import { ArrowLeftOutlined, ExportOutlined, FileTextOutlined, CheckCircleOutlined, WarningOutlined } from '@ant-design/icons';
import { history, useParams } from '@umijs/max';
import { getReport, generateReport, exportReportPdf } from '@/api/verify';
import { formatDate, downloadBlob } from '@/utils';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';
import { CodeTag } from '@/components/common/CodeTag';
import type { VerifyCheckResult, VerifyReport as VerifyReportType } from '@/types';
import { Permission } from '@/components/common/Permission';

const { Paragraph, Text, Title } = Typography;

const checkTypeLabels: Record<string, string> = {
  SUBJECT: '主体一致性',
  AMOUNT: '金额一致性',
  TIME: '时间逻辑',
  REPEAT: '重复融资',
};

const conclusionLabels: Record<string, string> = {
  SUBJECT: '买卖双方主体信息一致，未发现异常。',
  AMOUNT: '合同、订单及发票金额未发现明显差异。',
  TIME: '交易材料时间顺序合理，未发现明显异常。',
  REPEAT: '未发现同企业已审批的重复融资记录。',
};

const VerifyReport: React.FC = () => {
  const params = useParams();
  const appId = Number(params?.appId || 0);
  const [report, setReport] = useState<VerifyReportType | null>(null);
  const [loading, setLoading] = useState(false);
  const dictionary = useCodeDictionary();
  const results = (report?.contentSnapshot?.results || []) as VerifyCheckResult[];
  const passedCount = results.filter(item => item.result === 'PASS').length;
  const assessmentColor = report?.overallAssessment === 'HIGH' ? '#cf1322'
    : report?.overallAssessment === 'MID' ? '#d46b08' : '#389e0d';

  const load = async () => {
    if (!appId) return;
    try {
      const r = await getReport(appId);
      setReport(r);
    } catch (e) {
      setReport(null);
    }
  };

  useEffect(() => { load(); }, [appId]);

  const handleGenerate = async () => {
    setLoading(true);
    try {
      const r = await generateReport(appId);
      setReport(r);
      message.success('报告已生成');
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async () => {
    if (!report?.reportNo) return;
    try {
      const blob = await exportReportPdf(report.reportNo);
      downloadBlob(blob, `verify_report_${report.reportNo}.pdf`);
      message.success('PDF 已下载');
    } catch (e: any) {
      message.error(e.message);
    }
  };

  return (
    <Card title={<div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <Button icon={<ArrowLeftOutlined />} onClick={() => history.push(`/audit/application/detail?appId=${appId}`)}>
        返回申请详情
      </Button>
      <span>核验报告 - 申请 #{appId}</span>
    </div>} extra={
      <div>
        <Permission perm={['VERIFY', 'update']} menuCode="report:generate"><Button type="primary" onClick={handleGenerate} loading={loading} style={{ marginRight: 8 }}>生成报告</Button></Permission>
        {report && <Permission perm={['VERIFY', 'view']} menuCode="report:export"><Button icon={<ExportOutlined />} onClick={handleExport}>导出 PDF</Button></Permission>}
      </div>
    }>
      {report ? (
        <div style={{ maxWidth: 1280, margin: '0 auto' }}>
          <Card
            bordered={false}
            style={{ background: 'linear-gradient(135deg, #f0f5ff 0%, #ffffff 70%)', border: '1px solid #d6e4ff' }}
          >
            <Row gutter={[24, 20]} align="middle">
              <Col xs={24} lg={10}>
                <Space align="start">
                  <FileTextOutlined style={{ fontSize: 34, color: '#1677ff', marginTop: 4 }} />
                  <div>
                    <Title level={3} style={{ margin: 0 }}>真实性核验报告</Title>
                    <Text type="secondary">报告编号：{report.reportNo} · 版本 v{report.version}</Text>
                  </div>
                </Space>
              </Col>
              <Col xs={12} sm={8} lg={5}><Statistic title="总体评估" value={dictionary.label('REPORT_ASSESSMENT', report.overallAssessment)} valueStyle={{ color: assessmentColor }} /></Col>
              <Col xs={12} sm={8} lg={4}><Statistic title="核验通过" value={passedCount} suffix={`/ ${results.length}`} prefix={<CheckCircleOutlined />} valueStyle={{ color: '#389e0d' }} /></Col>
              <Col xs={12} sm={8} lg={5}><Statistic title="异常项目" value={report.abnormalCount} prefix={<WarningOutlined />} valueStyle={{ color: report.abnormalCount ? '#cf1322' : '#389e0d' }} /></Col>
            </Row>
          </Card>

          <Descriptions bordered size="small" column={{ xs: 1, sm: 2, lg: 3 }} style={{ marginTop: 16 }}>
            <Descriptions.Item label="申请编号">#{report.applicationId}</Descriptions.Item>
            <Descriptions.Item label="报告版本">v{report.version}</Descriptions.Item>
            <Descriptions.Item label="生成时间">{formatDate(report.generatedAt)}</Descriptions.Item>
          </Descriptions>

          {report.riskHints?.length ? <Alert
            style={{ marginTop: 16 }}
            type={report.abnormalCount ? 'warning' : 'success'}
            showIcon
            message={report.abnormalCount ? '风险提示' : '核验结论'}
            description={<ul style={{ margin: 0, paddingLeft: 20 }}>{report.riskHints.map((hint, index) => <li key={index}>{hint}</li>)}</ul>}
          /> : null}

          <Title level={4} style={{ marginTop: 24 }}>核验结果</Title>
          {results.length ? <Row gutter={[16, 16]}>
            {results.map((item, index) => {
              const hints = (item.details?.hints || []) as string[];
              const passed = item.result === 'PASS';
              return <Col xs={24} md={12} key={item.id || `${item.checkType}-${index}`}>
                <Card
                  size="small"
                  title={<Space><span>{checkTypeLabels[item.checkType] || item.checkType}</span><CodeTag type="VERIFY_RESULT" code={item.result} /></Space>}
                  style={{ height: '100%', borderTop: `3px solid ${passed ? '#52c41a' : '#ff4d4f'}` }}
                >
                  <Paragraph style={{ minHeight: 44, marginBottom: 8 }}>
                    {passed ? conclusionLabels[item.checkType] || '该核验项目已通过。' : hints[0] || '该核验项目存在异常，请人工复核。'}
                  </Paragraph>
                  {hints.length > 0 && <>
                    <Divider style={{ margin: '10px 0' }} />
                    <Text type="secondary">检查说明</Text>
                    <ul style={{ margin: '6px 0 8px', paddingLeft: 20 }}>{hints.map((hint, i) => <li key={i}>{hint}</li>)}</ul>
                  </>}
                  <Space size={[4, 4]} wrap>
                    {item.executedRules?.map(rule => <Tag key={rule}>{rule}</Tag>)}
                  </Space>
                  <div style={{ marginTop: 10 }}><Text type="secondary">核验时间：{formatDate(item.executedAt)}</Text></div>
                </Card>
              </Col>;
            })}
          </Row> : <Empty description="暂无核验明细" />}
        </div>
      ) : (
        <Empty description="尚未生成报告，请点击「生成报告」按钮" />
      )}
    </Card>
  );
};

export default VerifyReport;
