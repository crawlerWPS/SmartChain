/**
 * ExportBtn - XLSX/PDF 导出组件
 */
import React, { useState } from 'react';
import { Button, message } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import { utils as xlsxUtils, write as xlsxWrite } from 'xlsx';
import { downloadBlob } from '@/utils';

interface XlsxProps {
  filename: string;
  sheets: { name: string; data: any[][] }[];
  buttonText?: string;
}

export const ExportXlsx: React.FC<XlsxProps> = ({ filename, sheets, buttonText = '导出 Excel' }) => {
  const handleExport = () => {
    const wb = xlsxUtils.book_new();
    sheets.forEach((s) => {
      const ws = xlsxUtils.aoa_to_sheet(s.data);
      xlsxUtils.book_append_sheet(wb, ws, s.name);
    });
    const wbout = xlsxWrite(wb, { type: 'array', bookType: 'xlsx' });
    downloadBlob(new Blob([wbout], { type: 'application/octet-stream' }), `${filename}.xlsx`);
    message.success('导出成功');
  };
  return (
    <Button icon={<DownloadOutlined />} onClick={handleExport}>
      {buttonText}
    </Button>
  );
};

interface PdfProps {
  fetchBlob: () => Promise<Blob>;
  filename: string;
  buttonText?: string;
}

export const ExportPdf: React.FC<PdfProps> = ({ fetchBlob, filename, buttonText = '导出 PDF' }) => {
  const [loading, setLoading] = useState(false);
  const handleExport = async () => {
    setLoading(true);
    try {
      const blob = await fetchBlob();
      downloadBlob(blob, `${filename}.pdf`);
      message.success('导出成功');
    } catch (e: any) {
      message.error(e.message || '导出失败');
    } finally {
      setLoading(false);
    }
  };
  return (
    <Button icon={<DownloadOutlined />} loading={loading} onClick={handleExport}>
      {buttonText}
    </Button>
  );
};

export default ExportXlsx;
