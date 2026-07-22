/**
 * v-permission 自定义指令 - React 版本（HOC 模式）
 * 用法：
 *   import { withPermission } from '@/components/common/Permission';
 *   const Btn = withPermission(() => <Button>审批</Button>, ['rule','approve']);
 */
import React from 'react';
import { can } from '@/access/access';

interface PermProps {
  perm: [string, string];
  fallback?: React.ReactNode;
}

/** 权限组件 - 包裹后无权限自动隐藏 */
export const Permission: React.FC<React.PropsWithChildren<PermProps>> = ({
  perm,
  fallback = null,
  children,
}) => {
  const [module, action] = perm;
  if (!can(module, action)) {
    return <>{fallback}</>;
  }
  return <>{children}</>;
};

/** HOC 版本 */
export function withPermission<P extends object>(
  Component: React.ComponentType<P>,
  perm: [string, string],
  fallback?: React.ReactNode
): React.FC<P> {
  return (props: P) => {
    const [module, action] = perm;
    if (!can(module, action)) {
      return <>{fallback}</>;
    }
    return <Component {...props} />;
  };
}
