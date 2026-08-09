/**
 * 材料核验页
 */
import React, { useState } from 'react';
import { Card, Tabs, Button, message, List, Tag, Empty, Row, Col, Progress } from 'antd';
import { ReloadOutlined } from '@ant-design/icons';
import { useParams } from '@umijs/max';
import FileUpload from '@/components/upload/FileUpload';
import { listMaterials, getRecognitionResult } from '@/api/application';
import { verifyAll } from '@/api/verify';
import { MATERIAL_TYPE_MAP, formatFileSize, formatDate } from '@/utils';

const MaterialVerify: React.FC = () => {
  const params = useParams();
  const appId = Number(params?.appId || 0);
  const [list, setList] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [verifyResults, setVerifyResults] = useState<any[]>([]);

  const load = async () => {
    if (!appId) return;
    setLoading(true);
    try {
      const result = await listMaterials(appId);
      setList(result || []);
    } catch (e: any) {
      message.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async () => {
    if (!appId) return;
    try {
      const results = await verifyAll(appId);
      setVerifyResults(results || []);
      message.success('核验完成');
    } catch (e: any) {
      message.error(e.message);
    }
  };

  React.useEffect(() => { load(); }, [appId]);

  return (
    <Card title={`材料核验 - 申请 #${appId}`} extra={<Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>}>
      <Tabs
        items={[
          { key: 'upload', label: '材料上传', children: (
            <div>
              <FileUpload applicationId={appId} onUploaded={load} />
              <List
                style={{ marginTop: 24 }}
                loading={loading}
                dataSource={list}
                renderItem={(item) => (
                  <List.Item actions={[
                    <a onClick={() => message.info('查看识别结果')}>查看</a>,
                    <a onClick={() => message.info('重新识别')}>重新识别</a>,
                  ]}>
                    <List.Item.Meta
                      title={<>{item.fileName || '未命名'} ({formatFileSize(item.fileSize)})</>}
                      description={
                        <div>
                          <Tag color="blue">{MATERIAL_TYPE_MAP[item.materialType] || '未识别'}</Tag>
                          {item.confidence != null && (
                            <span style={{ marginLeft: 8 }}>
                              置信度：<Progress percent={item.confidence} size="small" style={{ width: 100, display: 'inline-flex' }} />
                            </span>
                          )}
                          <span style={{ marginLeft: 12, color: '#999' }}>上传于 {formatDate(item.createdAt)}</span>
                        </div>
                      }
                    />
                  </List.Item>
                )}
              />
            </div>
          )},
          { key: 'verify', label: '真实性核验', children: (
            <div>
              <Button type="primary" onClick={handleVerify} style={{ marginBottom: 16 }}>执行全部核验</Button>
              {verifyResults.length === 0 ? (
                <Empty description="尚无核验结果" />
              ) : (
                <Row gutter={16}>
                  {verifyResults.map((r, i) => (
                    <Col span={8} key={i}>
                      <Card size="small" title={r.checkType}>
                        <Tag color={r.result === 'PASS' ? 'green' : r.result === 'FAIL' ? 'red' : 'orange'}>{r.result}</Tag>
                        {r.executedRules?.map((rule: string, idx: number) => (
                          <Tag key={idx} style={{ marginTop: 8 }}>{rule}</Tag>
                        ))}
                      </Card>
                    </Col>
                  ))}
                </Row>
              )}
            </div>
          )},
        ]}
      />
    </Card>
  );
};

export default MaterialVerify;
