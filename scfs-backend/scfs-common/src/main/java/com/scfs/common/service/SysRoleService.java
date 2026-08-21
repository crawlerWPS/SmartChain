package com.scfs.common.service;

import com.scfs.common.constant.ScfsConstants;
import com.scfs.common.entity.SysMenu;
import com.scfs.common.entity.SysRole;
import com.scfs.common.entity.SysRolePermission;
import com.scfs.common.mapper.SysMenuMapper;
import com.scfs.common.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色与权限服务 - 对应 RFC 4.1.1 RoleService + PermissionService
 *
 * <p>关键策略：</p>
 * <ul>
 *   <li>角色权限缓存到 Redis（key: role:perm:{roleCode}）</li>
 *   <li>菜单构建树形结构返回前端</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final com.scfs.common.mapper.SysUserMapper userMapper;

    // ========== 角色 CRUD ==========
    public SysRole getById(Long id) {
        return roleMapper.selectById(id);
    }

    public SysRole getByCode(String roleCode) {
        return roleMapper.selectByCode(roleCode);
    }

    public List<SysRole> listAll() {
        return roleMapper.selectAll();
    }

    @Transactional
    public Long createRole(SysRole role) {
        if (role == null || !StringUtils.hasText(role.getRoleCode())) {
            throw new IllegalArgumentException("角色编码不能为空");
        }
        if (!StringUtils.hasText(role.getRoleName())) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        if (!StringUtils.hasText(role.getRoleType())) {
            throw new IllegalArgumentException("角色类型不能为空");
        }
        role.setRoleCode(role.getRoleCode().trim().toUpperCase());
        if (roleMapper.selectByCode(role.getRoleCode()) != null) {
            throw new IllegalArgumentException("角色编码已存在");
        }
        roleMapper.insert(role);
        return role.getId();
    }

    @Transactional
    public void updateRole(SysRole role) {
        roleMapper.update(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        if (userMapper.countByRoleCode(role.getRoleCode()) > 0) {
            throw new IllegalStateException("该角色仍有关联用户，无法删除");
        }
        menuMapper.deleteRoleMenuByRoleId(id);
        roleMapper.deletePermissionsByRoleId(id);
        roleMapper.deleteById(id);
    }

    // ========== API 权限 ==========
    public List<SysRolePermission> getPermissionsByRoleId(Long roleId) {
        return roleMapper.selectPermissionsByRoleId(roleId);
    }

    @Transactional
    public void assignPermissions(Long roleId, List<SysRolePermission> permissions) {
        roleMapper.deletePermissionsByRoleId(roleId);
        for (SysRolePermission perm : permissions) {
            perm.setRoleId(roleId);
            roleMapper.insertPermission(perm);
        }
    }

    /**
     * 检查角色是否具有指定模块的权限
     * @param roleCode 角色编码
     * @param module 模块名
     * @param permission 权限项（view/create/update/delete/export/approve/reject）
     */
    public boolean hasPermission(String roleCode, String module, String permission) {
        SysRole role = roleMapper.selectByCode(roleCode);
        if (role == null) {
            return false;
        }
        // ADMIN 默认拥有全部权限
        if (ScfsConstants.ROLE_ADMIN.equals(roleCode)) {
            return true;
        }
        List<SysRolePermission> perms = roleMapper.selectPermissionsByRoleId(role.getId());
        return perms.stream()
                .filter(p -> p.getModule().equalsIgnoreCase(module))
                .anyMatch(p -> p.getPermissions() != null && p.getPermissions().contains(permission));
    }

    // ========== 菜单管理 ==========
    public List<SysMenu> listAllMenus() {
        return menuMapper.selectAllTree();
    }

    public SysMenu getMenuByCode(String menuCode) {
        return menuMapper.selectByCode(menuCode);
    }

    @Transactional
    public Long createMenu(SysMenu menu) {
        if (menuMapper.selectByCode(menu.getMenuCode()) != null) {
            throw new IllegalArgumentException("菜单编码已存在");
        }
        menuMapper.insert(menu);
        return menu.getId();
    }

    @Transactional
    public void updateMenu(SysMenu menu) {
        menuMapper.update(menu);
    }

    @Transactional
    public void deleteMenu(Long id) {
        menuMapper.deleteById(id);
    }

    /**
     * 查询角色关联的菜单（完整树形）
     */
    public List<SysMenu> getMenusByRoleId(Long roleId) {
        return menuMapper.selectMenusByRoleId(roleId);
    }

    public List<Long> getMenuIdsByRoleId(Long roleId) {
        requireRole(roleId);
        return menuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        requireRole(roleId);
        List<Long> requestedIds = menuIds == null ? List.of() : menuIds.stream()
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        if (!requestedIds.isEmpty()
                && menuMapper.selectExistingIds(requestedIds).size() != requestedIds.size()) {
            throw new IllegalArgumentException("菜单或按钮不存在");
        }
        Set<Long> normalizedIds = includeAncestorMenus(requestedIds);
        menuMapper.deleteRoleMenuByRoleId(roleId);
        for (Long menuId : normalizedIds) {
            // 通过 SysRoleMenu 临时对象传递
            com.scfs.common.entity.SysRoleMenu rm = new com.scfs.common.entity.SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            menuMapper.insertRoleMenu(rm);
        }
    }

    /** 任一子菜单或按钮被授权时，自动补齐其全部父级目录。 */
    private Set<Long> includeAncestorMenus(List<Long> menuIds) {
        Set<Long> result = new LinkedHashSet<>(menuIds);
        Map<Long, SysMenu> menuById = menuMapper.selectAllTree().stream()
                .collect(Collectors.toMap(SysMenu::getId, menu -> menu));
        for (Long menuId : menuIds) {
            SysMenu current = menuById.get(menuId);
            while (current != null && current.getParentId() != null && current.getParentId() > 0) {
                Long parentId = current.getParentId();
                if (!result.add(parentId)) {
                    break;
                }
                current = menuById.get(parentId);
            }
        }
        return result;
    }

    private void requireRole(Long roleId) {
        if (roleId == null || roleMapper.selectById(roleId) == null) {
            throw new IllegalArgumentException("角色不存在");
        }
    }

    /**
     * 构建菜单树形结构（前端 RBAC）
     */
    public List<Map<String, Object>> buildMenuTree(List<SysMenu> menus) {
        Map<Long, List<SysMenu>> childrenMap = menus.stream()
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        List<Map<String, Object>> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (menu.getParentId() == null || menu.getParentId() == 0L) {
                tree.add(toMenuNode(menu, childrenMap));
            }
        }
        return tree;
    }

    private Map<String, Object> toMenuNode(SysMenu menu, Map<Long, List<SysMenu>> childrenMap) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", menu.getId());
        node.put("menuName", menu.getMenuName());
        node.put("menuCode", menu.getMenuCode());
        node.put("menuType", menu.getMenuType());
        node.put("path", menu.getPath());
        node.put("component", menu.getComponent());
        node.put("permission", menu.getPermission());
        node.put("icon", menu.getIcon());
        node.put("sort", menu.getSort());
        node.put("visible", menu.getVisible());

        List<SysMenu> children = childrenMap.getOrDefault(menu.getId(), List.of());
        if (!children.isEmpty()) {
            List<Map<String, Object>> childNodes = children.stream()
                    .sorted((a, b) -> Integer.compare(
                            a.getSort() == null ? 0 : a.getSort(),
                            b.getSort() == null ? 0 : b.getSort()))
                    .map(c -> toMenuNode(c, childrenMap))
                    .collect(Collectors.toList());
            node.put("children", childNodes);
        }
        return node;
    }
}
