import React, { useEffect, useState } from 'react';
import { Card, Tabs, Button, message, List, Tag, Empty, Row, Col, Progress, Modal, Descriptions } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { useParams } from '@umijs/max';
import FileUpload from '@/components/upload/FileUpload';
import { listMaterials, getRecognitionResult, reRecognizeMaterial } from '@/api/application';
import { verifyAll } from '@/api/verify';
import { formatFileSize, formatDate } from '@/utils';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';
import { CodeTag } from '@/components/common/CodeTag';
import type { ApplicationMaterial, MaterialRecognitionResult } from '@/types';

const MaterialVerify: React.FC = () => {
  const params = useParams();
  const appId = Number(params?.appId || 0);
  const [list, setList] = useState<ApplicationMaterial[]>([]);
  const [loading, setLoading] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [recognizingId, setRecognizingId] = useState<number>();
  const [verifyResults, setVerifyResults] = useState<any[]>([]);
  const [recognition, setRecognition] = useState<MaterialRecognitionResult>();
  const [recognitionVisible, setRecognitionVisible] = useState(false);
  const dictionary = useCodeDictionary();

  const load = async () => {
    if (!appId) return;
    setLoading(true);
    try {
      setList(await listMaterials(appId) || []);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  const handleView = async (materialId: number) => {
    try {
      const result = await getRecognitionResult(materialId);
      if (!result) {
        message.info('OCR 尚未完成，请稍后刷新再查看');
        return;
      }
      setRecognition(result);
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

  useEffect(() => { load(); }, [appId]);

  return (
    <Card title={`材料核验 - 申请 #${appId}`} extra={<Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>}>
      <Tabs items={[
        { key: 'upload', label: '材料上传', children: <div>
          <FileUpload applicationId={appId} onUploaded={load} />
          <List style={{ marginTop: 24 }} loading={loading} dataSource={list}
            renderItem={(item) => <List.Item actions={[
              <a key="view" onClick={() => handleView(item.id)}>查看</a>,
              <a key="recognize" onClick={() => handleReRecognize(item.id)}>
                {recognizingId === item.id ? '识别中...' : '重新识别'}
              </a>,
            ]}>
              <List.Item.Meta
                title={<>{item.fileName || `文件 #${item.fileObjectId}`}（{formatFileSize(item.fileSize || 0)}）</>}
                description={<div>
                  <CodeTag type="MATERIAL_TYPE" code={item.materialType} />
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
            {verifyResults.map((result, index) => <Col span={8} key={result.id || index}>
              <Card size="small" title={dictionary.label('VERIFY_CHECK_TYPE', result.checkType)}>
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
          <Descriptions.Item label="商品">{recognition.commodity || '-'}</Descriptions.Item>
          <Descriptions.Item label="金额">{recognition.amount ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="交易编号">{recognition.transactionNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="识别时间">{formatDate(recognition.recognizedAt)}</Descriptions.Item>
        </Descriptions>}
      </Modal>
    </Card>
  );
};

export default MaterialVerify;
