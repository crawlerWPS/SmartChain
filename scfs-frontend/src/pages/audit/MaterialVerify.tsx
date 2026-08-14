import React, { useEffect, useState } from 'react';
import { Card, Tabs, Button, message, List, Tag, Empty, Row, Col, Progress, Modal, Descriptions, Table, Alert, Typography, Popconfirm } from 'antd';
import { ArrowLeftOutlined, ReloadOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons';
import { history, useParams } from '@umijs/max';
import FileUpload from '@/components/upload/FileUpload';
import { listMaterials, getRecognitionResult, reRecognizeMaterial, downloadMaterial, previewMaterial, deleteMaterial } from '@/api/application';
import { getCheckResults, verifyAll } from '@/api/verify';
import { formatFileSize, formatDate, downloadBlob } from '@/utils';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';
import { CodeTag } from '@/components/common/CodeTag';
import { Permission } from '@/components/common/Permission';
import type { ApplicationMaterial, MaterialRecognitionResult, VerifyCheckResult } from '@/types';

const MaterialVerify: React.FC = () => {
  const params = useParams();
  const appId = Number(params?.appId || 0);
  const [list, setList] = useState<ApplicationMaterial[]>([]);
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [recognizingId, setRecognizingId] = useState<number>();
  const [deletingId, setDeletingId] = useState<number>();
  const [verifyResults, setVerifyResults] = useState<VerifyCheckResult[]>([]);
  const [selectedVerifyResult, setSelectedVerifyResult] = useState<VerifyCheckResult>();
  const [recognition, setRecognition] = useState<MaterialRecognitionResult>();
  const [viewingMaterial, setViewingMaterial] = useState<ApplicationMaterial>();
  const [recognitionVisible, setRecognitionVisible] = useState(false);
  const dictionary = useCodeDictionary();

  const load = async () => {
    if (!appId) return;
    setLoading(true);
    try {
      const [materials, results] = await Promise.all([listMaterials(appId), getCheckResults(appId)]);
      setList(materials || []);
      setVerifyResults(results || []);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  const handleView = async (material: ApplicationMaterial) => {
    try {
      const result = await getRecognitionResult(material.id);
      if (!result) {
        message.info('OCR 尚未完成，请稍后刷新再查看');
        return;
      }
      setRecognition(result);
      setViewingMaterial(material);
      setRecognitionVisible(true);
    } catch (e: any) {
      message.error(e.message);
    }
  };

  const handleReRecognize = async (materialId: number) => {
    setRecognizingId(materialId);
    try {
      await reRecognizeMaterial(materialId);
      message.success('已重新发起 OCR 识别');
      await load();
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setRecognizingId(undefined);
    }
  };

  const handleVerify = async () => {
    if (!appId) return;
    setVerifying(true);
    try {
      const results = await verifyAll(appId);
      setVerifyResults(results || []);
      message.success('核验完成');
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setVerifying(false);
    }
  };

  const handlePreview = async (item: ApplicationMaterial) => {
    try {
      const url = URL.createObjectURL(await previewMaterial(item.fileObjectId));
      window.open(url, '_blank', 'noopener,noreferrer');
      window.setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (e: any) { message.error(e.message); }
  };

  const handleDownload = async (item: ApplicationMaterial) => {
    try { downloadBlob(await downloadMaterial(item.fileObjectId), item.fileName || `material-${item.id}`); }
    catch (e: any) { message.error(e.message); }
  };

  const handleDelete = async (item: ApplicationMaterial) => {
    setDeletingId(item.id);
    try {
      await deleteMaterial(item.id);
      message.success('材料已删除，可重新上传');
      await load();
    } catch (e: any) {
      message.error(e.message || '材料删除失败');
    } finally {
      setDeletingId(undefined);
    }
  };

  useEffect(() => { load(); }, [appId]);

  const value = (input: any) => input === null || input === undefined || input === '' ? '-' : String(input);
  const money = (input: any) => input === null || input === undefined || input === '' ? '-' : `¥${Number(input).toLocaleString('zh-CN')}`;
  const matchTag = (matched: boolean | undefined) => matched === undefined
    ? <Tag>未核对</Tag>
    : <Tag color={matched ? 'success' : 'error'}>{matched ? '一致' : '不一致'}</Tag>;
  const materialName = (row: any) => row.fileName || `材料 #${row.materialId || '-'}`;
  const documentNoLabel = () => (objectValue({ INVOICE: '发票号码', CONTRACT: '合同编号', ORDER: '订单编号' }, viewingMaterial?.materialType) || '单据编号');
  const documentDate = () => {
    if (!recognition) return undefined;
    return ({
      INVOICE: recognition.invoiceDate,
      CONTRACT: recognition.contractDate,
      ORDER: recognition.orderDate,
      LOGISTICS: recognition.logisticsDate,
      LOGISTICS_DOC: recognition.logisticsDate,
      ACCEPTANCE: recognition.acceptanceDate,
      ACCEPTANCE_CERT: recognition.acceptanceDate,
      PAYMENT: recognition.paymentDate,
      PAYMENT_VOUCHER: recognition.paymentDate,
    } as Record<string, string | undefined>)[viewingMaterial?.materialType || ''];
  };
  const documentDateLabel = () => objectValue({
    INVOICE: '开票时间', CONTRACT: '合同日期', ORDER: '订单日期', LOGISTICS: '物流日期',
    LOGISTICS_DOC: '物流日期', ACCEPTANCE: '验收日期', ACCEPTANCE_CERT: '验收日期',
    PAYMENT: '付款日期', PAYMENT_VOUCHER: '付款日期',
  }, viewingMaterial?.materialType) || '材料日期';
  const objectValue = (values: Record<string, string>, key?: string) => key ? values[key] : undefined;

  const renderVerifyDetails = (result?: VerifyCheckResult) => {
    if (!result) return null;
    const details = result.details || {};
    const hints: string[] = details.hints || [];
    const common = <>
      <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="核验类型">{dictionary.label('VERIFY_CHECK_TYPE', result.checkType)}</Descriptions.Item>
        <Descriptions.Item label="核验结论"><CodeTag type="VERIFY_RESULT" code={result.result} /></Descriptions.Item>
      </Descriptions>
      {hints.length > 0 && <Alert type={result.result === 'PASS' ? 'success' : 'warning'} showIcon
        message="比对说明" description={<ul style={{ margin: 0, paddingLeft: 20 }}>{hints.map((hint, i) => <li key={i}>{hint}</li>)}</ul>}
        style={{ marginBottom: 16 }} />}
    </>;

    if (result.checkType === 'SUBJECT') {
      const buyer = details.applicationBuyer || {};
      const seller = details.applicationSeller || {};
      return <>{common}<Typography.Title level={5}>融资申请主体</Typography.Title>
        <Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
          <Descriptions.Item label="买方名称">{value(buyer.name)}</Descriptions.Item><Descriptions.Item label="买方信用代码">{value(buyer.uscc)}</Descriptions.Item>
          <Descriptions.Item label="卖方名称">{value(seller.name)}</Descriptions.Item><Descriptions.Item label="卖方信用代码">{value(seller.uscc)}</Descriptions.Item>
        </Descriptions>
        <Typography.Title level={5}>OCR 材料逐项比对</Typography.Title>
        <Table size="small" pagination={false} rowKey={(r: any) => `${r.materialId}-${r.materialType}`} dataSource={details.comparisons || []} columns={[
          { title: '材料', render: (_: any, r: any) => <>{materialName(r)}<br/><CodeTag type="MATERIAL_TYPE" code={r.materialType}/></> },
          { title: 'OCR买方', render: (_: any, r: any) => <>{value(r.ocrBuyerName)}<br/>{value(r.ocrBuyerUscc)}</> },
          { title: '买方结论', render: (_: any, r: any) => matchTag(r.buyerMatch) },
          { title: 'OCR卖方', render: (_: any, r: any) => <>{value(r.ocrSellerName)}<br/>{value(r.ocrSellerUscc)}</> },
          { title: '卖方结论', render: (_: any, r: any) => matchTag(r.sellerMatch) },
        ]}/></>;
    }
    if (result.checkType === 'AMOUNT') {
      const amounts = details.amounts || {};
      return <>{common}<Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="融资申请金额">{money(details.financingAmount)}</Descriptions.Item>
        <Descriptions.Item label="合同 / 发票一致">{matchTag(details.contractInvoiceMatch)}</Descriptions.Item>
        <Descriptions.Item label="OCR合同金额">{money(amounts.CONTRACT)}</Descriptions.Item><Descriptions.Item label="OCR发票金额">{money(amounts.INVOICE)}</Descriptions.Item>
      </Descriptions><Table size="small" pagination={false} rowKey={(r: any) => `${r.materialId}-${r.materialType}`} dataSource={details.materialAmounts || []} columns={[
        { title: 'OCR材料', render: (_: any, r: any) => materialName(r) },
        { title: '类型', render: (_: any, r: any) => <CodeTag type="MATERIAL_TYPE" code={r.materialType}/> },
        { title: '识别金额', dataIndex: 'ocrAmount', render: money },
        { title: '融资金额未超限', dataIndex: 'financingAmountMatch', render: matchTag },
      ]}/></>;
    }
    if (result.checkType === 'TIME') return <>{common}
      <Descriptions bordered size="small" column={1} style={{ marginBottom: 16 }}><Descriptions.Item label="融资申请提交时间">{formatDate(details.applicationSubmittedAt)}</Descriptions.Item></Descriptions>
      <Table size="small" pagination={false} rowKey={(r: any) => `${r.materialId}-${r.materialType}`} dataSource={details.materialDates || []} columns={[
        { title: 'OCR材料', render: (_: any, r: any) => materialName(r) }, { title: '类型', render: (_: any, r: any) => <CodeTag type="MATERIAL_TYPE" code={r.materialType}/> }, { title: '识别日期', dataIndex: 'ocrDate', render: value },
      ]}/><Typography.Title level={5} style={{ marginTop: 16 }}>时间顺序比对</Typography.Title><Table size="small" pagination={false} rowKey={(_: any, i) => String(i)} dataSource={details.comparisons || []} columns={[
        { title: '比对项', dataIndex: 'message' }, { title: '前项日期', dataIndex: 'leftDate', render: value }, { title: '后项日期', dataIndex: 'rightDate', render: value }, { title: '结论', dataIndex: 'match', render: matchTag },
      ]}/></>;
    if (result.checkType === 'REPEAT') {
      const current = details.currentApplication || {};
      return <>{common}<Typography.Title level={5}>当前融资申请</Typography.Title><Descriptions bordered size="small" column={2} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="申请编号">{value(current.appNo)}</Descriptions.Item><Descriptions.Item label="业务类型">{value(current.businessType)}</Descriptions.Item>
        <Descriptions.Item label="融资金额">{money(current.financingAmount)}</Descriptions.Item><Descriptions.Item label="已匹配历史申请">{details.existingApprovedCount || 0} 笔</Descriptions.Item>
      </Descriptions><Table size="small" pagination={false} rowKey="id" dataSource={details.matchedApplications || []} locale={{ emptyText: '未发现重复融资申请' }} columns={[
        { title: '历史申请编号', dataIndex: 'appNo', render: value }, { title: '业务类型', dataIndex: 'businessType', render: value }, { title: '融资金额', dataIndex: 'financingAmount', render: money }, { title: '状态', dataIndex: 'status', render: value },
      ]}/></>;
    }
    return common;
  };

  return (
    <Card title={<div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <Button icon={<ArrowLeftOutlined />} onClick={() => history.push(`/audit/application/detail?appId=${appId}`)}>
        返回申请详情
      </Button>
      <span>材料核验 - 申请 #{appId}</span>
    </div>} extra={<Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>}>
      <Tabs items={[
        { key: 'upload', label: '材料上传', children: <div>
          <FileUpload applicationId={appId} onUploaded={load} />
          <List style={{ marginTop: 24 }} loading={loading} dataSource={list}
            renderItem={(item) => <List.Item actions={[
              <a key="preview" onClick={() => handlePreview(item)}>在线预览</a>,
              <a key="download" onClick={() => handleDownload(item)}>下载</a>,
              <a key="view" onClick={() => handleView(item)}>查看</a>,
              <a key="recognize" onClick={() => handleReRecognize(item.id)}>
                {recognizingId === item.id ? '识别中...' : '重新识别'}
              </a>,
              <Permission key="delete" perm={['VERIFY', 'delete']}>
                <Popconfirm
                  title="确认删除该材料？"
                  description="材料及其 OCR 识别结果将被清除，之后可以重新上传。"
                  okText="删除"
                  cancelText="取消"
                  okButtonProps={{ danger: true, loading: deletingId === item.id }}
                  onConfirm={() => handleDelete(item)}
                >
                  <a style={{ color: '#ff4d4f' }}><DeleteOutlined /> 删除</a>
                </Popconfirm>
              </Permission>,
            ]}>
              <List.Item.Meta
                title={<>{item.fileName || `文件 #${item.fileObjectId}`}（{formatFileSize(item.fileSize || 0)}）</>}
                description={<div>
                  <CodeTag type="MATERIAL_TYPE" code={item.materialType} />
                  {item.ocrTemplateCode && <Tag color="blue" style={{ marginLeft: 8 }}>
                    OCR模板：{item.ocrTemplateCode}｜{item.ocrTemplateName}
                  </Tag>}
                  {item.confidence != null && <span style={{ marginLeft: 8 }}>
                    置信度：<Progress percent={Number(item.confidence)} size="small" style={{ width: 100, display: 'inline-flex' }} />
                  </span>}
                  <span style={{ marginLeft: 12, color: '#999' }}>上传于 {formatDate(item.createdAt)}</span>
                </div>}
              />
            </List.Item>} />
        </div> },
        { key: 'verify', label: '真实性核验', children: <div>
          <Button type="primary" loading={verifying} onClick={handleVerify} style={{ marginBottom: 16 }}>执行全部核验</Button>
          {verifyResults.length === 0 ? <Empty description="暂无核验结果" /> : <Row gutter={16}>
            {verifyResults.map((result, index) => <Col xs={24} md={12} xl={8} key={result.id || index} style={{ marginBottom: 16 }}>
              <Card hoverable size="small" title={dictionary.label('VERIFY_CHECK_TYPE', result.checkType)} onClick={() => setSelectedVerifyResult(result)} style={{ cursor: 'pointer' }} actions={[<Button key="detail" type="link" icon={<EyeOutlined/>} onClick={e=>{e.stopPropagation();setSelectedVerifyResult(result);}}>查看对比明细</Button>]}>
                <CodeTag type="VERIFY_RESULT" code={result.result} />
                {result.executedRules?.map((rule: string) => <Tag key={rule} style={{ marginTop: 8 }}>{rule}</Tag>)}
              </Card>
            </Col>)}
          </Row>}
        </div> },
      ]} />
      <Modal title="OCR 识别结果" open={recognitionVisible} footer={null}
        onCancel={() => setRecognitionVisible(false)} width={720}>
        {recognition && <Descriptions bordered column={2} size="small">
          <Descriptions.Item label="买方名称">{recognition.buyerName || '-'}</Descriptions.Item>
          <Descriptions.Item label="卖方名称">{recognition.sellerName || '-'}</Descriptions.Item>
          <Descriptions.Item label="买方信用代码">{recognition.buyerUscc || '-'}</Descriptions.Item>
          <Descriptions.Item label="卖方信用代码">{recognition.sellerUscc || '-'}</Descriptions.Item>
          <Descriptions.Item label={documentDateLabel()}>{formatDate(documentDate(), 'YYYY-MM-DD')}</Descriptions.Item>
          <Descriptions.Item label="金额">{recognition.amount ?? '-'}</Descriptions.Item>
          <Descriptions.Item label={documentNoLabel()}>{recognition.transactionNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="识别时间">{formatDate(recognition.recognizedAt)}</Descriptions.Item>
        </Descriptions>}
      </Modal>
      <Modal title="真实性核验比对明细" open={!!selectedVerifyResult} footer={null}
        onCancel={() => setSelectedVerifyResult(undefined)} width={1000} destroyOnClose>
        {renderVerifyDetails(selectedVerifyResult)}
      </Modal>
    </Card>
  );
};

export default MaterialVerify;
