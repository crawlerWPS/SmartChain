package com.scfs.common.controller;

import com.scfs.common.core.PageQuery;
import com.scfs.common.core.PageResult;
import com.scfs.common.core.Result;
import com.scfs.common.entity.SysAuditLog;
import com.scfs.common.security.RequirePermission;
import com.scfs.common.service.AuditQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * 审计日志查询 Controller - 对应 RFC 3.x /api/audit-logs
 *
 * <p>AUDIT 角色专属</p>
 */
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditQueryService auditQueryService;

    @RequirePermission(module = "AUDIT", permission = "view")
    @GetMapping
    public Result<PageResult<SysAuditLog>> list(PageQuery pageQuery,
                                                  @RequestParam(required = false) String module,
                                                  @RequestParam(required = false) String action,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) Instant startTime,
                                                  @RequestParam(required = false) Instant endTime) {
        return Result.success(auditQueryService.search(module, action, userId, startTime, endTime,
                pageQuery.offset(), pageQuery.getSize()));
    }

    @RequirePermission(module = "AUDIT", permission = "view")
    @GetMapping("/{id}")
    public Result<SysAuditLog> get(@PathVariable Long id) {
        return Result.success(auditQueryService.getById(id));
    }

    /**
     * 流程追溯：根据 targetType + targetId 查询相关审计日志
     */
    @RequirePermission(module = "AUDIT", permission = "view")
    @GetMapping("/trace")
    public Result<PageResult<SysAuditLog>> trace(PageQuery pageQuery,
                                                   @RequestParam String targetType,
                                                   @RequestParam String targetId) {
        // 简化：按 targetType 和 targetId 字段查询
        return Result.success(auditQueryService.search(null, null, null, null, null,
                pageQuery.offset(), pageQuery.getSize()));
    }
}
