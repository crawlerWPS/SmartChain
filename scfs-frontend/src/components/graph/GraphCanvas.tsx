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
import { ZoomInOutlined, ZoomOutOutlined, ExportOutlined, ReloadOutlined, SwapOutlined, DragOutlined } from '@ant-design/icons';
import { getGraphData, getAllGraphData, type GraphData } from '@/api/graph';

interface Props {
  enterpriseId?: number;
  level?: number;
  height?: number;
}

const GraphCanvas: React.FC<Props> = ({ enterpriseId, level = 2, height = 600 }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const graphRef = useRef<Graph | null>(null);
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<GraphData>({ nodes: [], edges: [] });
  const [layoutMode, setLayoutMode] = useState<'force' | 'radial'>('force');

  const getLayoutConfig = (g6Data: GraphData) => {
    const coreNode = g6Data.nodes.find((n) => n.isCore);
    const focusNodeId = coreNode?.id || g6Data.nodes[0]?.id;

    if (layoutMode === 'force') {
      return {
        type: 'force',
        nodeStrength: -400,
        linkDistance: 220,
        preventOverlap: true,
        nodeSize: 60,
        nodeSpacing: 50,
        edgeStrength: 0.4,
        gravity: 8,
        alphaDecay: 0.028,
        maxIteration: 800,
      };
    }

    return {
      type: 'radial',
      focusNode: focusNodeId,
      unitRadius: 180,
      linkDistance: 200,
      preventOverlap: true,
      nodeSize: 60,
      nodeSpacing: 40,
      maxIteration: 1000,
      strictRadial: true,
    };
  };

  const buildGraph = (g6Data: GraphData) => {
    if (!containerRef.current) return;
    if (graphRef.current) {
      graphRef.current.destroy();
      graphRef.current = null;
    }

    const containerWidth = containerRef.current.offsetWidth || 1000;

    const graph = new Graph({
      container: containerRef.current,
      width: containerWidth,
      height,
      autoFit: 'view',
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-node', 'brush-select'],
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
      layout: getLayoutConfig(g6Data) as any,
    });

    graph.setData({
      nodes: g6Data.nodes.map((n) => ({
        id: n.id,
        data: { ...n, ...(n.data || {}) },
        // 将 isCore/relationType 放到顶层，供 style 回调读取
        isCore: n.isCore,
        label: n.label,
      })),
      edges: g6Data.edges.map((e, i) => ({
        id: `e${i}-${e.source}-${e.target}`,
        source: e.source,
        target: e.target,
        data: { ...e },
        relationType: e.relationType,
        label: e.label,
      })),
    });

    graph.render();

    // PaperConnect 自由拖动：初始布局结束后停止 force 动画，避免节点被拉回
    graph.on('afterlayout', () => graph.stopLayout());
    // 拖动开始时若布局仍在运行也停止
    graph.on('node:dragstart', () => graph.stopLayout());
    // 拖动结束后固定节点位置
    graph.on('node:dragend', (e: any) => {
      const model = e.item?.getModel?.();
      if (!model) return;
      graph.updateData('node', [{ id: model.id, fx: model.x, fy: model.y }]);
    });

    graphRef.current = graph;
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const result = enterpriseId
        ? await getGraphData(enterpriseId, level)
        : await getAllGraphData();
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
  }, [enterpriseId, level, layoutMode]);

  const handleZoomIn = () => graphRef.current?.zoomTo(1.5);
  const handleZoomOut = () => graphRef.current?.zoomTo(0.5);
  const handleReload = () => loadData();
  const handleExport = () => {
    if (!graphRef.current) return;
    const url = (graphRef.current as any).toDataURL?.('image/png', 1);
    if (url) {
      const a = document.createElement('a');
      a.href = url;
      a.download = `graph_${enterpriseId || 'all'}.png`;
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
          <Button icon={<SwapOutlined />} onClick={() => setLayoutMode(m => m === 'force' ? 'radial' : 'force')}>
            {layoutMode === 'force' ? '径向布局' : '自由布局'}
          </Button>
          <Button icon={<DragOutlined />} onClick={() => message.info('当前为自由布局模式，可直接拖动节点')}>
            拖动说明
          </Button>
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
