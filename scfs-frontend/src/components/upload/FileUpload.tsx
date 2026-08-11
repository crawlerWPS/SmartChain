import React, { useState } from 'react';
import { Upload, message, Progress, Select, Space } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { uploadMaterial } from '@/api/application';
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

    setUploading(true);
    setProgress(0);
    try {
      const materialId = await uploadMaterial(applicationId, file, materialType, setProgress);
      message.success('上传成功，OCR 识别已启动');
      onUploaded?.(materialId);
    } catch (e: any) {
      message.error(e.message || '上传失败');
    } finally {
      setUploading(false);
    }
    return false;
  };

  return (
    <div>
      <Space style={{ marginBottom: 12 }}>
        <span>材料类型：</span>
        <Select
          value={materialType}
          onChange={setMaterialType}
          options={dictionary.options('MATERIAL_TYPE')}
          placeholder="请选择材料类型"
          style={{ width: 220 }}
        />
      </Space>
      <Upload.Dragger
        accept=".pdf,.jpg,.jpeg,.png,.docx,.xlsx"
        multiple={false}
        showUploadList={false}
        beforeUpload={handleUpload}
        disabled={uploading || !materialType}
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
