/**
 * 核验报告页
 */
import React, { useState, useEffect } from 'react';
import { Card, Button, Descriptions, Tag, message, Empty, Typography } from 'antd';
import { ExportOutlined } from '@ant-design/icons';
import { useParams } from '@umijs/max';
import { getReport, generateReport, exportReportPdf } from '@/api/verify';
import { formatDate, downloadBlob } from '@/utils';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';

const { Paragraph } = Typography;

const VerifyReport: React.FC = () => {
  const params = useParams();
  const appId = Number(params?.appId || 0);
  const [report, setReport] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const dictionary = useCodeDictionary();

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
    <Card title={`核验报告 - 申请 #${appId}`} extra={
      <div>
        <Button type="primary" onClick={handleGenerate} loading={loading} style={{ marginRight: 8 }}>生成报告</Button>
        {report && <Button icon={<ExportOutlined />} onClick={handleExport}>导出 PDF</Button>}
      </div>
    }>
      {report ? (
        <>
          <Descriptions bordered column={2}>
            <Descriptions.Item label="报告编号">{report.reportNo}</Descriptions.Item>
            <Descriptions.Item label="版本">v{report.version}</Descriptions.Item>
            <Descriptions.Item label="异常数" span={2}>
              <Tag color={report.abnormalCount > 0 ? 'red' : 'green'}>{report.abnormalCount}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="生成时间">{formatDate(report.generatedAt)}</Descriptions.Item>
            <Descriptions.Item label="内容哈希">{report.contentHash?.slice(0, 16) || '-'}...</Descriptions.Item>
          </Descriptions>
          <Card size="small" title="总体评估" style={{ marginTop: 16 }}>
            <Paragraph>{dictionary.label('REPORT_ASSESSMENT', report.overallAssessment)}</Paragraph>
          </Card>
          {report.riskHints?.length > 0 && (
            <Card size="small" title="风险提示" style={{ marginTop: 12 }}>
              <ul>
                {report.riskHints.map((h: string, i: number) => <li key={i}>{h}</li>)}
              </ul>
            </Card>
          )}
        </>
      ) : (
        <Empty description="尚未生成报告，请点击「生成报告」按钮" />
      )}
    </Card>
  );
};

export default VerifyReport;
