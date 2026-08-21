/**
 * 用户/角色/菜单 API - 对应后端 SysUserController/SysRoleController
 */
import { request } from '@umijs/max';
import type { PageQuery, PageResult, SysUser, SysRole, SysMenu } from '@/types';

export interface UserQuery extends PageQuery {
  roleCode?: string;
}

export interface UserUpsert {
  username: string;
  realName: string;
  roleCode: string;
  email?: string;
  phone?: string;
  password?: string;
}

/** IF-USR-001 用户分页 */
export async function pageUsers(query: UserQuery): Promise<PageResult<SysUser>> {
  return request('/users', { method: 'GET', params: query });
}

/** IF-USR-002 创建用户 */
export async function createUser(data: UserUpsert): Promise<SysUser> {
  return request('/users', { method: 'POST', data });
}

/** IF-USR-003 更新用户 */
export async function updateUser(id: number, data: Partial<UserUpsert>): Promise<SysUser> {
  return request(`/users/${id}`, { method: 'PUT', data });
}

/** IF-USR-004 切换状态 */
export async function toggleUserStatus(id: number, status: number) {
  return request(`/users/${id}/status`, { method: 'PATCH', params: { status } });
}

/** IF-USR-005 删除用户 */
export async function deleteUser(id: number) {
  return request(`/users/${id}`, { method: 'DELETE' });
}

/** IF-ROLE-001 角色列表 */
export async function listRoles(): Promise<SysRole[]> {
  return request('/roles', { method: 'GET' });
}

/** IF-ROLE-002 创建角色 */
export async function createRole(data: { roleCode: string; roleName: string; roleType: string; description?: string }) {
  return request('/roles', { method: 'POST', data });
}

/** IF-ROLE-003 更新角色权限 */
export async function updateRolePermissions(roleId: number, permissions: Record<string, string[]>) {
  return request(`/roles/${roleId}/permissions`, { method: 'PUT', data: permissions });
}

/** IF-ROLE-004 角色菜单授权 */
export async function assignRoleMenus(roleId: number, menuIds: number[]) {
  return request(`/roles/${roleId}/menus`, { method: 'PUT', data: { menuIds } });
}

export async function deleteRole(id: number) {
  return request(`/roles/${id}`, { method: 'DELETE' });
}

export interface RoleMenuAssignment {
  roleId: number;
  menuIds: number[];
}

export async function getRoleMenus(roleId: number): Promise<RoleMenuAssignment> {
  return request(`/roles/${roleId}/menus`, { method: 'GET' });
}

/** IF-MENU-001 菜单树 */
export async function getMenuTree(): Promise<SysMenu[]> {
  return request('/menus', { method: 'GET' });
}

/** IF-MENU-002 创建菜单 */
export async function createMenu(data: Partial<SysMenu>) {
  return request('/menus', { method: 'POST', data });
}

/** IF-MENU-003 更新菜单 */
export async function updateMenu(id: number, data: Partial<SysMenu>) {
  return request(`/menus/${id}`, { method: 'PUT', data });
}

/** IF-MENU-004 删除菜单 */
export async function deleteMenu(id: number) {
  return request(`/menus/${id}`, { method: 'DELETE' });
}
