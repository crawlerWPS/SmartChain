/**
 * 路由配置 - 1:1 复刻 RFC §2.3b 菜单树
 * 注：access 字段对应 access.ts 中权限函数
 */
import { RouteConfig } from '@umijs/max';

const routes: RouteConfig[] = [
  {
    path: '/login',
    layout: false,
    component: '@/pages/auth/Login',
  },
  {
    path: '/',
    redirect: '/workspace',
  },
  // 工作台
  {
    name: '工作台',
    path: '/workspace',
    icon: 'DashboardOutlined',
    access: 'canViewWorkspace',
    component: '@/pages/workspace/Workspace',
  },
  // 供应链图谱
  {
    name: '供应链图谱',
    path: '/graph',
    icon: 'ShareAltOutlined',
    access: 'canViewGraph',
    routes: [
      {
        name: '企业关系图谱',
        path: '/graph/relation',
        component: '@/pages/graph/RelationGraph',
        access: 'menu:graph.relations',
      },
      {
        name: '企业角色',
        path: '/graph/role',
        component: '@/pages/graph/EnterpriseRole',
        access: 'menu:graph.role',
      },
      {
        name: '位置分析',
        path: '/graph/position',
        component: '@/pages/graph/PositionAnalysis',
        access: 'menu:graph.position',
      },
      {
        name: '异常预警',
        path: '/graph/abnormal',
        component: '@/pages/graph/AbnormalList',
        access: 'menu:graph.abnormal',
      },
    ],
  },
  // 审核中心
  {
    name: '审核中心',
    path: '/audit',
    icon: 'AuditOutlined',
    access: 'canViewAudit',
    routes: [
      {
        name: '融资申请',
        path: '/audit/application',
        component: '@/pages/audit/ApplicationList',
        access: 'menu:audit.application',
      },
      {
        name: '融资申请详情',
        path: '/audit/application/detail',
        component: '@/pages/audit/ApplicationDetail',
        access: 'application:view',
        hideInMenu: true,
      },
      {
        name: '材料核验',
        path: '/audit/material/:appId',
        component: '@/pages/audit/MaterialVerify',
        access: 'material:view',
        hideInMenu: true,
      },
      {
        name: '预审补正',
        path: '/audit/preaudit/:appId',
        component: '@/pages/audit/PreAuditCheck',
        access: 'preaudit:view',
        hideInMenu: true,
      },
      {
        name: '核验报告',
        path: '/audit/report/:appId',
        component: '@/pages/audit/VerifyReport',
        access: 'verify:view',
        hideInMenu: true,
      },
      {
        name: '风险画像',
        path: '/audit/risk/:appId',
        component: '@/pages/audit/RiskProfile',
        access: 'risk:view',
        hideInMenu: true,
      },
    ],
  },
  // 规则配置
  {
    name: '规则配置',
    path: '/rule',
    icon: 'SettingOutlined',
    access: 'canViewRule',
    routes: [
      {
        name: '规则管理',
        path: '/rule/list',
        component: '@/pages/rule/RuleList',
        access: 'menu:rule.definition',
      },
      {
        name: '风险权重',
        path: '/rule/weight',
        component: '@/pages/rule/WeightConfig',
        access: 'menu:rule.weight',
      },
      {
        name: '材料模板',
        path: '/rule/template',
        component: '@/pages/rule/TemplateList',
        access: 'menu:rule.template',
      },
      {
        name: 'OCR识别模板',
        path: '/rule/ocr-template',
        component: '@/pages/rule/OcrTemplateList',
        access: 'menu:rule.ocr-template',
      },
    ],
  },
  // 审计查询
  {
    name: '审计查询',
    path: '/audit-trail',
    icon: 'FileSearchOutlined',
    access: 'canViewAuditTrail',
    component: '@/pages/audit-trail/AuditLogList',
  },
  // 系统管理
  {
    name: '系统管理',
    path: '/system',
    icon: 'ToolOutlined',
    access: 'canViewSystem',
    routes: [
      {
        name: '用户管理',
        path: '/system/user',
        component: '@/pages/system/UserList',
        access: 'menu:system.user',
      },
      {
        name: '角色管理',
        path: '/system/role',
        component: '@/pages/system/RoleList',
        access: 'menu:system.role',
      },
      {
        name: '菜单管理',
        path: '/system/menu',
        component: '@/pages/system/MenuList',
        access: 'menu:system.menu',
      },
    ],
  },
  // 403
  {
    path: '/403',
    layout: false,
    component: '@/pages/error/Forbidden',
  },
  {
    path: '*',
    layout: false,
    component: '@/pages/error/NotFound',
  },
];

export default routes;
