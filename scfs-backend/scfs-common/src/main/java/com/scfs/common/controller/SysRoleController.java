package com.scfs.common.controller;

import com.scfs.common.core.Result;
import com.scfs.common.entity.SysMenu;
import com.scfs.common.entity.SysRole;
import com.scfs.common.entity.SysRolePermission;
import com.scfs.common.security.RequirePermission;
import com.scfs.common.security.SecurityContextHelper;
import com.scfs.common.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色与权限 Controller - 对应 RFC 3.x /api/roles /api/menus
 */
@RestController
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;
    private final SecurityContextHelper securityContextHelper;

    // ========== 角色 ==========
    @RequirePermission(module = "USER", permission = "view")
    @GetMapping("/api/roles")
    public Result<List<SysRole>> listRoles() {
        return Result.ok(roleService.listAll());
    }

    @RequirePermission(module = "USER", permission = "view")
    @GetMapping("/api/roles/{id}")
    public Result<SysRole> getRole(@PathVariable Long id) {
        return Result.ok(roleService.getById(id));
    }

    @RequirePermission(module = "USER", permission = "create")
    @PostMapping("/api/roles")
    public Result<Long> createRole(@RequestBody SysRole role) {
        return Result.ok(roleService.createRole(role));
    }

    @RequirePermission(module = "USER", permission = "update")
    @PutMapping("/api/roles/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        role.setId(id);
        roleService.updateRole(role);
        return Result.ok();
    }

    @RequirePermission(module = "USER", permission = "view")
    @GetMapping("/api/roles/{id}/permissions")
    public Result<List<SysRolePermission>> getRolePermissions(@PathVariable Long id) {
        return Result.ok(roleService.getPermissionsByRoleId(id));
    }

    @RequirePermission(module = "USER", permission = "update")
    @PutMapping("/api/roles/{id}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody List<SysRolePermission> permissions) {
        roleService.assignPermissions(id, permissions);
        return Result.ok();
    }

    // ========== 菜单 ==========
    @RequirePermission(module = "USER", permission = "view")
    @GetMapping("/api/menus")
    public Result<List<Map<String, Object>>> listMenuTree() {
        return Result.ok(roleService.buildMenuTree(roleService.listAllMenus()));
    }

    @RequirePermission(module = "USER", permission = "create")
    @PostMapping("/api/menus")
    public Result<Long> createMenu(@RequestBody SysMenu menu) {
        return Result.ok(roleService.createMenu(menu));
    }

    @RequirePermission(module = "USER", permission = "update")
    @PutMapping("/api/menus/{id}")
    public Result<Void> updateMenu(@PathVariable Long id, @RequestBody SysMenu menu) {
        menu.setId(id);
        roleService.updateMenu(menu);
        return Result.ok();
    }

    @RequirePermission(module = "USER", permission = "delete")
    @DeleteMapping("/api/menus/{id}")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        roleService.deleteMenu(id);
        return Result.ok();
    }

    @RequirePermission(module = "USER", permission = "view")
    @GetMapping("/api/roles/{id}/menus")
    public Result<List<Map<String, Object>>> getRoleMenus(@PathVariable Long id) {
        return Result.ok(roleService.buildMenuTree(roleService.getMenusByRoleId(id)));
    }

    @RequirePermission(module = "USER", permission = "update")
    @PutMapping("/api/roles/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        List<Long> menuIds = body.get("menuIds");
        roleService.assignMenus(id, menuIds);
        return Result.ok();
    }

    /**
     * 当前登录用户的菜单（前端 RBAC 导航）
     */
    @GetMapping("/api/auth/menus")
    public Result<List<Map<String, Object>>> currentUserMenus() {
        String roleCode = securityContextHelper.getCurrentRoleCodeOrThrow();
        SysRole role = roleService.getByCode(roleCode);
        if (role == null) {
            return Result.ok(List.of());
        }
        return Result.ok(roleService.buildMenuTree(roleService.getMenusByRoleId(role.getId())));
    }
}
