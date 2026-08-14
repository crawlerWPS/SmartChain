import React, { useState } from 'react';
import { Upload, message, Progress, Select, Space, Alert } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { listSelectableOcrTemplates, uploadMaterial } from '@/api/application';
import type { OcrTemplate } from '@/api/ocrTemplate';
import { isFileAllowed } from '@/api/file';
import { useCodeDictionary } from '@/hooks/useCodeDictionary';

interface Props {
  applicationId: number;
  onUploaded?: (materialId: number) => void;
}

const FileUpload: React.FC<Props> = ({ applicationId, onUploaded }) => {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [materialType, setMaterialType] = useState<string>();
  const [ocrTemplateId, setOcrTemplateId] = useState<number>();
  const [templates, setTemplates] = useState<OcrTemplate[]>([]);
  const [templateLoading, setTemplateLoading] = useState(false);
  const dictionary = useCodeDictionary();

  const handleUpload = async (file: File) => {
    const check = isFileAllowed(file);
    if (!check.ok) {
      message.error(check.reason || '文件不符合上传要求');
      return false;
    }
    if (!materialType) {
      message.error('请先选择材料类型');
      return false;
    }
    if (!ocrTemplateId) {
      message.error('请选择OCR识别模板');
      return false;
    }

    setUploading(true);
    setProgress(0);
    try {
      const materialId = await uploadMaterial(applicationId, file, materialType, ocrTemplateId, setProgress);
      message.success('上传成功，OCR 识别已启动');
      onUploaded?.(materialId);
    } catch (e: any) {
      message.error(e.message || '上传失败');
    } finally {
      setUploading(false);
    }
    return false;
  };

  const handleMaterialTypeChange = async (value: string) => {
    setMaterialType(value);
    setOcrTemplateId(undefined);
    setTemplates([]);
    setTemplateLoading(true);
    try {
      const options = await listSelectableOcrTemplates(value);
      setTemplates(options || []);
      if (options?.length) setOcrTemplateId(options[0].id);
    } catch (e: any) {
      message.error(e.message || 'OCR模板加载失败');
    } finally {
      setTemplateLoading(false);
    }
  };

  return (
    <div>
      <Space style={{ marginBottom: 12 }}>
        <span>材料类型：</span>
        <Select
          value={materialType}
          onChange={handleMaterialTypeChange}
          options={dictionary.options('MATERIAL_TYPE')}
          placeholder="请选择材料类型"
          style={{ width: 220 }}
        />
        <span>识别模板：</span>
        <Select
          value={ocrTemplateId}
          onChange={setOcrTemplateId}
          loading={templateLoading}
          disabled={!materialType || templateLoading}
          options={templates.map(t => ({ value: t.id!, label: `${t.templateCode}｜${t.templateName}` }))}
          placeholder="请选择OCR识别模板"
          style={{ width: 320 }}
        />
      </Space>
      {materialType && !templateLoading && templates.length === 0 &&
        <Alert type="warning" showIcon message="该材料类型暂无启用的OCR模板，请先到规则配置中维护模板。" style={{ marginBottom: 12 }} />}
      <Upload.Dragger
        accept=".pdf,.jpg,.jpeg,.png,.docx,.xlsx"
        multiple={false}
        showUploadList={false}
        beforeUpload={handleUpload}
        disabled={uploading || !materialType || !ocrTemplateId}
      >
        <p className="ant-upload-drag-icon"><InboxOutlined /></p>
        <p className="ant-upload-text">点击或拖拽文件上传</p>
        <p className="ant-upload-hint">支持 PDF / JPG / PNG / DOCX / XLSX，单文件不超过 50MB</p>
      </Upload.Dragger>
      {uploading && <Progress percent={progress} status="active" style={{ marginTop: 12 }} />}
    </div>
  );
};

export default FileUpload;
