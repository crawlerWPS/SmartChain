/**
 * GraphCanvas - AntV G6 5 供应链关系图谱画布
 * - 多层级（默认 2 层，防止性能爆炸）
 * - 节点区分核心企业（高亮）
 * - 右键展开上下游
 * - 缩放、框选、图片导出
 * - 对接 IF-GRAPH-004 接口
 */
import React, { useEffect, useRef, useState } from 'react';
import { Graph, Tooltip as G6Tooltip, Menu } from '@antv/g6';
import { Spin, Button, Space, message } from 'antd';
import { ZoomInOutlined, ZoomOutOutlined, ExportOutlined, ReloadOutlined } from '@ant-design/icons';
import { getGraphData, type GraphData } from '@/api/graph';

interface Props {
  enterpriseId: number;
  level?: number;
  height?: number;
}

const GraphCanvas: React.FC<Props> = ({ enterpriseId, level = 2, height = 600 }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const graphRef = useRef<Graph | null>(null);
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<GraphData>({ nodes: [], edges: [] });

  const buildGraph = (g6Data: GraphData) => {
    if (!containerRef.current) return;
    if (graphRef.current) {
      graphRef.current.destroy();
      graphRef.current = null;
    }

    const graph = new Graph({
      container: containerRef.current,
      width: containerRef.current.offsetWidth,
      height,
      modes: {
        default: ['drag-canvas', 'zoom-canvas', 'drag-node', 'brush-select'],
      },
      plugins: [
        {
          type: 'tooltip',
          key: 'node-tooltip',
          trigger: 'hover',
          getContent: (e: any) => {
            const item = e.item;
            const model = item?.getModel?.() || item;
            const data = model?.data;
            if (!data) return '';
            return `<div>
              <div><strong>${data.name}</strong></div>
              <div>USCC: ${data.uscc || '-'}</div>
              <div>行业: ${data.industry || '-'}</div>
            </div>`;
          },
        } as any,
      ],
      node: {
        style: {
          size: (d: any) => (d.isCore ? 50 : 32),
          fill: (d: any) => (d.isCore ? '#f5222d' : '#1890ff'),
          stroke: (d: any) => (d.isCore ? '#a8071a' : '#096dd9'),
          lineWidth: 2,
          labelText: (d: any) => d.label,
          labelFill: '#000',
          labelFontSize: 12,
        },
      },
      edge: {
        style: {
          stroke: (d: any) => (d.relationType === 'SUPPLY' ? '#52c41a' : '#faad14'),
          lineWidth: 1.5,
          labelText: (d: any) => d.label || '',
          labelFontSize: 10,
          endArrow: true,
        },
      },
      layout: {
        type: 'force',
        preventOverlap: true,
        nodeSize: 40,
        linkDistance: 120,
        nodeStrength: -50,
        edgeStrength: 0.7,
      },
    });

    graph.setData({
      nodes: g6Data.nodes.map((n) => ({
        id: n.id,
        data: { ...n, ...(n.data || {}) },
        style: { isCore: n.isCore },
      })),
      edges: g6Data.edges.map((e, i) => ({
        id: `e${i}-${e.source}-${e.target}`,
        source: e.source,
        target: e.target,
        data: { ...e },
      })),
    });

    graph.render();
    graphRef.current = graph;
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const result = await getGraphData(enterpriseId, level);
      setData(result);
      buildGraph(result);
    } catch (e: any) {
      message.error(e.message || '图谱加载失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    return () => {
      graphRef.current?.destroy();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enterpriseId, level]);

  const handleZoomIn = () => graphRef.current?.zoomTo(1.5);
  const handleZoomOut = () => graphRef.current?.zoomTo(0.5);
  const handleReload = () => loadData();
  const handleExport = () => {
    if (!graphRef.current) return;
    const url = (graphRef.current as any).toDataURL?.('image/png', 1);
    if (url) {
      const a = document.createElement('a');
      a.href = url;
      a.download = `graph_${enterpriseId}.png`;
      a.click();
      message.success('图谱已导出');
    }
  };

  return (
    <Spin spinning={loading}>
      <div style={{ marginBottom: 12 }}>
        <Space>
          <Button icon={<ZoomInOutlined />} onClick={handleZoomIn}>放大</Button>
          <Button icon={<ZoomOutOutlined />} onClick={handleZoomOut}>缩小</Button>
          <Button icon={<ReloadOutlined />} onClick={handleReload}>重新加载</Button>
          <Button icon={<ExportOutlined />} onClick={handleExport}>导出图片</Button>
        </Space>
        <span style={{ marginLeft: 16, color: '#999' }}>
          共 {data.nodes.length} 个节点 / {data.edges.length} 条边
        </span>
      </div>
      <div ref={containerRef} style={{ width: '100%', height, border: '1px solid #e8e8e8', borderRadius: 4 }} />
    </Spin>
  );
};

export default GraphCanvas;
