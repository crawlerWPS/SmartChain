/**
 * FileUpload - 文件上传组件
 * 限制 50MB、白名单 pdf/jpg/png/docx/xlsx、OCR 置信度展示、手动修正入口
 */
import React, { useState } from 'react';
import { Upload, message, Progress, Tooltip } from 'antd';
import { InboxOutlined, FileTextOutlined, CheckCircleOutlined, WarningOutlined } from '@ant-design/icons';
import type { UploadFile } from 'antd/es/upload/interface';
import { uploadMaterial } from '@/api/application';
import { isFileAllowed } from '@/api/file';
import { formatFileSize } from '@/utils';

interface Props {
  applicationId: number;
  onUploaded?: (material: any) => void;
}

const FileUpload: React.FC<Props> = ({ applicationId, onUploaded }) => {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [lastMaterial, setLastMaterial] = useState<any>(null);

  const handleUpload = async (file: File) => {
    const check = isFileAllowed(file);
    if (!check.ok) {
      message.error(check.reason || '文件不合法');
      return false;
    }
    setUploading(true);
    setProgress(0);
    try {
      const material = await uploadMaterial(applicationId, file, setProgress);
      setLastMaterial(material);
      message.success('上传成功');
      onUploaded?.(material);
    } catch (e: any) {
      message.error(e.message || '上传失败');
    } finally {
      setUploading(false);
    }
    return false;
  };

  return (
    <div>
      <Upload.Dragger
        accept=".pdf,.jpg,.jpeg,.png,.docx,.xlsx"
        multiple={false}
        showUploadList={false}
        beforeUpload={handleUpload}
        disabled={uploading}
      >
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">点击或拖拽文件上传</p>
        <p className="ant-upload-hint">
          支持 PDF / JPG / PNG / DOCX / XLSX，单文件 ≤ 50MB
        </p>
      </Upload.Dragger>

      {uploading && (
        <Progress percent={progress} status="active" style={{ marginTop: 12 }} />
      )}

      {lastMaterial && !uploading && (
        <div style={{ marginTop: 12 }}>
          {lastMaterial.confidence != null && (
            <Tooltip title={`OCR 置信度 ${lastMaterial.confidence}%`}>
              {lastMaterial.confidence < 60 ? (
                <WarningOutlined style={{ color: '#ff4d4f' }} />
              ) : (
                <CheckCircleOutlined style={{ color: '#52c41a' }} />
              )}
            </Tooltip>
          )}
          <FileTextOutlined style={{ marginLeft: 8 }} />
          <span style={{ marginLeft: 4 }}>
            {lastMaterial.fileName || '已上传文件'} - {formatFileSize(lastMaterial.fileSize)}
          </span>
          {lastMaterial.confidence != null && lastMaterial.confidence < 60 && (
            <a style={{ marginLeft: 12, color: '#faad14' }} onClick={() => message.info('请前往材料列表手动指定类型')}>
              置信度低，需人工指定
            </a>
          )}
        </div>
      )}
    </div>
  );
};

export default FileUpload;
