/**
 * 企业关系图谱页 - G6 画布
 * 默认展示全部企业关系图谱，支持按关键词搜索聚焦到单个企业
 */
import React, { useState } from 'react';
import { Card, Input, InputNumber, Space, Button, message, Modal, Upload, Table, Alert, Tag } from 'antd';
import { SearchOutlined, ReloadOutlined, UploadOutlined, DownloadOutlined } from '@ant-design/icons';
import * as XLSX from 'xlsx';
import GraphCanvas from '@/components/graph/GraphCanvas';
import { importRelations, pageEnterprises, type RelationImportRow } from '@/api/graph';
import { Permission } from '@/components/common/Permission';

const relationTypes = new Set(['SUPPLY', 'PURCHASE', 'LOGISTICS', 'FINANCING', 'CUSTOMER', 'OTHER']);
const columns = [
  { title: '行号', dataIndex: 'rowNumber', width: 60 },
  { title: '买方企业', dataIndex: 'buyerName' },
  { title: '卖方企业', dataIndex: 'sellerName' },
  { title: '关系类型', dataIndex: 'relationType', render: (v: string) => relationTypes.has(v) ? <Tag color="blue">{v}</Tag> : <Tag color="red">{v || '缺失'}</Tag> },
  { title: '交易日期', dataIndex: 'transactionDate' },
];

const headerMap: Record<string, keyof RelationImportRow> = {
  买方企业名称: 'buyerName', 买方统一社会信用代码: 'buyerUscc',
  卖方企业名称: 'sellerName', 卖方统一社会信用代码: 'sellerUscc',
  关系类型: 'relationType', 交易日期: 'transactionDate', 备注: 'remark',
  buyerName: 'buyerName', buyerUscc: 'buyerUscc', sellerName: 'sellerName', sellerUscc: 'sellerUscc',
  relationType: 'relationType', transactionDate: 'transactionDate', remark: 'remark',
};

const RelationGraph: React.FC = () => {
  const [keyword, setKeyword] = useState('');
  const queryEnterpriseId = typeof window !== 'undefined'
    ? Number(new URLSearchParams(window.location.search).get('enterpriseId')) || undefined
    : undefined;
  const [enterpriseId, setEnterpriseId] = useState<number | undefined>(queryEnterpriseId);
  const [level, setLevel] = useState(2);
  const [refreshKey, setRefreshKey] = useState(0);
  const [importOpen, setImportOpen] = useState(false);
  const [importRows, setImportRows] = useState<RelationImportRow[]>([]);
  const [importing, setImporting] = useState(false);

  const handleSearch = async () => {
    if (!keyword) {
      // 关键词为空时，展示全部
      setEnterpriseId(undefined);
      setRefreshKey((k) => k + 1);
      return;
    }
    try {
      const result = await pageEnterprises({ page: 1, size: 5, keyword });
      if (!result.list || result.list.length === 0) {
        message.warning('未找到匹配企业');
        return;
      }
      setEnterpriseId(result.list[0].id);
    } catch (e: any) {
      message.error(e.message);
    }
  };

  const handleReset = () => {
    setKeyword('');
    setEnterpriseId(undefined);
    setRefreshKey((k) => k + 1);
  };

  const downloadTemplate = () => {
    const sheet = XLSX.utils.aoa_to_sheet([
      ['买方企业名称', '买方统一社会信用代码', '卖方企业名称', '卖方统一社会信用代码', '关系类型', '交易日期', '备注'],
      ['北京中科智造集团', '911100000000000001', '广州锐捷电子有限公司', '914400000000000002', 'SUPPLY', '2026-08-01', '示例行'],
    ]);
    const book = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(book, sheet, '买卖方关系');
    XLSX.writeFile(book, '买卖方关系导入模板.xlsx');
  };

  const parseImportFile = async (file: File) => {
    try {
      const buffer = await file.arrayBuffer();
      const workbook = XLSX.read(buffer, { type: 'array', cellDates: true });
      const sheet = workbook.Sheets[workbook.SheetNames[0]];
      const matrix = XLSX.utils.sheet_to_json<any[]>(sheet, { header: 1, defval: '' });
      const headers = (matrix[0] || []).map((v) => String(v).trim());
      const mapped = headers.map((h) => headerMap[h]);
      if (!mapped.some(Boolean)) throw new Error('未识别到模板表头，请下载并使用系统模板');
      const rows = matrix.slice(1).filter((r) => r.some((v) => String(v).trim() !== '')).map((r, index) => {
        const row: any = { rowNumber: index + 2 };
        mapped.forEach((key, col) => { if (key) row[key] = r[col]; });
        if (row.amount !== '' && row.amount != null) row.amount = Number(row.amount);
        if (row.transactionDate instanceof Date) row.transactionDate = row.transactionDate.toISOString().slice(0, 10);
        else if (row.transactionDate) row.transactionDate = String(row.transactionDate).slice(0, 10);
        row.relationType = String(row.relationType || '').trim().toUpperCase();
        return row as RelationImportRow;
      });
      setImportRows(rows);
      message.success(`已读取 ${rows.length} 条关系，请确认后导入`);
    } catch (e: any) {
      message.error(e.message || '文件解析失败');
    }
    return false;
  };

  const confirmImport = async () => {
    if (!importRows.length) return message.warning('请先上传并解析文件');
    setImporting(true);
    try {
      const result = await importRelations(importRows);
      if (result.errors?.length) {
        Modal.error({ title: '导入存在错误', width: 640, content: <div>{result.errors.map((error) => <div key={error}>{error}</div>)}</div> });
      } else {
        message.success(`导入完成：新增 ${result.createdRelations} 条关系，新增 ${result.createdEnterprises} 家企业`);
        setImportOpen(false);
        setImportRows([]);
        setRefreshKey((k) => k + 1);
      }
    } catch (e: any) {
      message.error(e.message || '导入失败');
    } finally {
      setImporting(false);
    }
  };

  return (
    <Card title="企业关系图谱" extra={
      <Space>
        <Input
          placeholder="企业名称/USCC（留空展示全部）"
          allowClear
          onPressEnter={handleSearch}
          onChange={(e) => setKeyword(e.target.value)}
          value={keyword}
          style={{ width: 260 }}
          prefix={<SearchOutlined />}
        />
        {enterpriseId ? (
          <>
            <span>层级：</span>
            <InputNumber min={1} max={2} value={level} onChange={(v) => setLevel(v || 2)} />
          </>
        ) : null}
        <Permission perm={['GRAPH', 'update']} menuCode="graph:import"><Button icon={<UploadOutlined />} onClick={() => setImportOpen(true)}>导入关系</Button></Permission>
        <Button icon={<ReloadOutlined />} onClick={handleReset}>重置</Button>
      </Space>
    }>
      <GraphCanvas key={refreshKey} enterpriseId={enterpriseId} level={level} height={600} />
      <div style={{ marginTop: 16, color: '#999', fontSize: 12 }}>
        <p>说明：</p>
        <ul>
          <li>默认展示全部企业供应链关系图谱</li>
          <li>输入企业名称搜索可聚焦到该企业的 N 跳关系</li>
          <li>红色节点为核心企业，蓝色为上下游节点</li>
          <li>绿色线为供应关系，黄色线为采购关系</li>
          <li>支持拖拽、缩放、框选、图片导出</li>
        </ul>
      </div>
      <Modal title="导入买卖方关系" open={importOpen} width={1000} onCancel={() => setImportOpen(false)} onOk={confirmImport} confirmLoading={importing} okText="确认导入" cancelText="取消">
        <Space style={{ marginBottom: 16 }}>
          <Button icon={<DownloadOutlined />} onClick={downloadTemplate}>下载导入模板</Button>
          <Upload accept=".xlsx,.xls,.csv" maxCount={1} beforeUpload={parseImportFile} showUploadList={false}>
            <Button icon={<UploadOutlined />}>选择 Excel/CSV 文件</Button>
          </Upload>
        </Space>
        <Alert type="info" showIcon message="买方和卖方统一社会信用代码必填；关系类型支持 SUPPLY、PURCHASE、LOGISTICS、FINANCING、CUSTOMER、OTHER。不存在的企业将自动创建。" style={{ marginBottom: 16 }} />
        <Table rowKey="rowNumber" size="small" scroll={{ x: 800 }} pagination={{ pageSize: 8 }} columns={columns} dataSource={importRows} locale={{ emptyText: '请上传模板文件后预览' }} />
      </Modal>
    </Card>
  );
};

export default RelationGraph;
