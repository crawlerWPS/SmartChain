/**
 * RBAC 权限鉴权 - 对齐后端 sys_role_permission / sys_role_menu
 * 角色编码：ADMIN / RISK_MANAGER / COMPLIANCE_OFFICER / BUSINESS_USER
 */
import type { AccessParams } from '@umijs/max';

export type CurrentUser = {
  userId: number;
  username: string;
  realName: string;
  roleCode: string;
  permissions: Record<string, string[]>;
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

/** access.ts 导出函数 - 用于路由守卫 */
export default function access(initialState: { currentUser?: CurrentUser }) {
  const user = initialState?.currentUser;
  setCurrentUser(user || null);

  return {
    // 菜单可见权限
    canViewWorkspace: true, // 所有登录用户可见
    canViewGraph: !!user,
    canViewAudit: !!user,
    canViewRule: !!user && (isAdmin() || hasRole('RISK_MANAGER') || hasRole('COMPLIANCE_OFFICER')),
    canViewAuditTrail: !!user && (isAdmin() || hasRole('COMPLIANCE_OFFICER')),
    canViewSystem: !!user && isAdmin(),

    // 按钮权限 - 严格匹配后端 @RequirePermission
    'graph:view': () => can('graph', 'view'),
    'application:view': () => can('application', 'view'),
    'application:create': () => can('application', 'create'),
    'application:submit': () => can('application', 'submit'),
    'application:approve': () => can('application', 'approve'),
    'application:reject': () => can('application', 'reject'),
    'material:view': () => can('material', 'view'),
    'material:upload': () => can('material', 'upload'),
    'material:re-recognize': () => can('material', 're-recognize'),
    'preaudit:view': () => can('preaudit', 'view'),
    'preaudit:check': () => can('preaudit', 'check'),
    'verify:view': () => can('verify', 'view'),
    'verify:check': () => can('verify', 'check'),
    'verify:report': () => can('verify', 'report'),
    'risk:view': () => can('risk', 'view'),
    'risk:score': () => can('risk', 'score'),
    'rule:view': () => can('rule', 'view'),
    'rule:create': () => can('rule', 'create'),
    'rule:edit': () => can('rule', 'edit'),
    'rule:submit': () => can('rule', 'submit'),
    'rule:approve': () => can('rule', 'approve'),
    'weight:view': () => can('weight', 'view'),
    'weight:submit': () => can('weight', 'submit'),
    'weight:approve': () => can('weight', 'approve'),
    'template:view': () => can('template', 'view'),
    'template:submit': () => can('template', 'submit'),
    'template:approve': () => can('template', 'approve'),
    'audit:view': () => can('audit', 'view'),
    'audit:export': () => can('audit', 'export'),
    'system:user:view': () => can('system', 'user:view'),
    'system:user:create': () => can('system', 'user:create'),
    'system:role:view': () => can('system', 'role:view'),
    'system:menu:view': () => can('system', 'menu:view'),
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
