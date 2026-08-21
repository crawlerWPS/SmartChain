package com.scfs.common.controller;

import com.scfs.common.core.PageResult;
import com.scfs.common.core.PageQuery;
import com.scfs.common.core.Result;
import com.scfs.common.entity.SysUser;
import com.scfs.common.security.RequirePermission;
import com.scfs.common.security.SecurityContextHelper;
import com.scfs.common.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理 Controller - 对应 RFC 3.x /api/users
 *
 * <p>ADMIN 角色专属</p>
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;
    private final SecurityContextHelper securityContextHelper;

    @RequirePermission(module = "USER", permission = "view")
    @GetMapping
    public Result<PageResult<SysUser>> list(PageQuery pageQuery,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String roleCode) {
        return Result.success(userService.search(keyword, roleCode, pageQuery.offset(), pageQuery.getSize()));
    }

    @RequirePermission(module = "USER", permission = "view")
    @GetMapping("/{id}")
    public Result<SysUser> get(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @RequirePermission(module = "USER", permission = "create")
    @PostMapping
    public Result<Long> create(@RequestBody SysUser user) {
        return Result.success(userService.createUser(user));
    }

    @RequirePermission(module = "USER", permission = "update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        userService.updateUser(user);
        return Result.success();
    }

    @RequirePermission(module = "USER", permission = "update")
    @PutMapping("/{id}/toggle-status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        userService.toggleStatus(id);
        return Result.success();
    }

    @RequirePermission(module = "USER", permission = "delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id, securityContextHelper.getCurrentUserIdOrThrow());
        return Result.success();
    }
}
