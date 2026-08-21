/**
 * RBAC 权限鉴权 - 对齐后端 sys_role_permission / sys_role_menu
 * 角色编码：ADMIN / RM / RCO / OPS_MAKER / OPS_CHECKER / OPS / AUDIT
 */
import type { AccessParams } from '@umijs/max';

export type CurrentUser = {
  userId: number;
  username: string;
  realName: string;
  roleCode: string;
  permissions: Record<string, string[]>;
  menuCodes?: string[];
};

let _currentUser: CurrentUser | null = null;

export function setCurrentUser(user: CurrentUser | null) {
  _currentUser = user;
}

export function getCurrentUser(): CurrentUser | null {
  return _currentUser;
}

/** 角色判断 */
export function hasRole(code: string): boolean {
  return _currentUser?.roleCode === code;
}

export function isAdmin(): boolean {
  return hasRole('ADMIN');
}

/**
 * 检查 module + permission（如 'rule','approve'）
 * 对应后端 @RequirePermission(module="rule", action="approve")
 */
export function can(module: string, action: string): boolean {
  if (!_currentUser) return false;
  if (isAdmin()) return true;
  const perms = _currentUser.permissions?.[module] || [];
  return perms.includes(action) || perms.includes('*');
}

export function hasMenu(code: string): boolean {
  if (!_currentUser) return false;
  if (isAdmin()) return true;
  return _currentUser.menuCodes?.includes(code) ?? false;
}

/** access.ts 导出函数 - 用于路由守卫 */
export default function access(initialState: { currentUser?: CurrentUser }) {
  const user = initialState?.currentUser;
  setCurrentUser(user || null);

  return {
    // 菜单可见权限 - 基于角色代码
    canViewWorkspace: !!user && hasMenu('workspace'),
    canViewGraph: !!user && hasMenu('graph'),
    canViewAudit: !!user && hasMenu('audit'),
    canViewRule: !!user && hasMenu('rule'),
    canViewAuditTrail: !!user && hasMenu('audit-trail'),
    canViewSystem: !!user && hasMenu('system'),
    'menu:graph.relations': () => hasMenu('graph.relations'),
    'menu:graph.role': () => hasMenu('graph.role'),
    'menu:graph.position': () => hasMenu('graph.position'),
    'menu:graph.abnormal': () => hasMenu('graph.abnormal'),
    'menu:audit.application': () => hasMenu('audit.application'),
    'menu:rule.definition': () => hasMenu('rule.definition'),
    'menu:rule.weight': () => hasMenu('rule.weight'),
    'menu:rule.template': () => hasMenu('rule.template'),
    'menu:rule.ocr-template': () => hasMenu('rule.ocr-template'),
    'menu:system.user': () => hasMenu('system.user'),
    'menu:system.role': () => hasMenu('system.role'),
    'menu:system.menu': () => hasMenu('system.menu'),

    // 按钮权限 - 严格匹配后端 @RequirePermission
    'graph:view': () => can('GRAPH', 'view'),
    'application:view': () => can('VERIFY', 'view'),
    'application:create': () => can('VERIFY', 'create'),
    'application:submit': () => can('VERIFY', 'create'),
    'application:approve': () => can('VERIFY', 'approve'),
    'application:reject': () => can('VERIFY', 'reject'),
    'material:view': () => can('VERIFY', 'view'),
    'material:upload': () => can('VERIFY', 'create'),
    'material:delete': () => can('VERIFY', 'delete'),
    'material:re-recognize': () => can('VERIFY', 'update'),
    'preaudit:view': () => can('PREAUDIT', 'view'),
    'preaudit:check': () => can('PREAUDIT', 'view'),
    'verify:view': () => can('VERIFY', 'view'),
    'verify:check': () => can('VERIFY', 'update'),
    'verify:report': () => can('VERIFY', 'view'),
    'risk:view': () => can('RISK', 'view'),
    'risk:score': () => can('RISK', 'view'),
    'rule:view': () => can('RULE', 'view'),
    'rule:create': () => can('RULE', 'create'),
    'rule:edit': () => can('RULE', 'update'),
    'rule:submit': () => can('RULE', 'create'),
    'rule:approve': () => can('RULE', 'approve'),
    'weight:view': () => can('RULE', 'view'),
    'weight:submit': () => can('RULE', 'create'),
    'weight:approve': () => can('RULE', 'approve'),
    'template:view': () => can('RULE', 'view'),
    'template:submit': () => can('RULE', 'create'),
    'audit:view': () => can('AUDIT', 'view'),
    'audit:export': () => can('AUDIT', 'export'),
    'system:user:view': () => can('USER', 'view'),
    'system:user:create': () => can('USER', 'create'),
    'system:role:view': () => can('USER', 'view'),
    'system:menu:view': () => can('USER', 'view'),
  } satisfies Record<string, ((...args: AccessParams) => boolean) | boolean>;
}

/**
 * v-permission 指令 - 按钮级别权限控制
 * 用法：<Button v-permission="['rule','approve']">复核</Button>
 */
import type { ReactNode } from 'react';
import React from 'react';

interface PermissionDirectiveProps {
  perm: [string, string];
  children: ReactNode;
  fallback?: ReactNode;
}

export const PermissionDirective: React.FC<PermissionDirectiveProps> = ({ perm, children, fallback = null }) => {
  const [module, action] = perm;
  if (!can(module, action)) {
    return <>{fallback}</>;
  }
  return <>{children}</>;
};

/** 双岗机制：经办人不能同时是复核人 */
export function isMakerOf(makerId?: number): boolean {
  return !!_currentUser && makerId === _currentUser.userId;
}

export function canApprove(makerId?: number): boolean {
  if (!_currentUser) return false;
  if (isAdmin()) return _currentUser.userId !== makerId;
  return _currentUser.userId !== makerId;
}
