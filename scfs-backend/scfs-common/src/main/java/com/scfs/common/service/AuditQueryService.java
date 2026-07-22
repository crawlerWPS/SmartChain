package com.scfs.common.service;

import com.scfs.common.core.PageResult;
import com.scfs.common.entity.SysAuditLog;
import com.scfs.common.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * 审计日志查询服务 - 对应 RFC 4.1.4 AuditQueryService
 *
 * <p>AUDIT 角色可查询 sys_audit_log，支持按时间/模块/操作/用户筛选</p>
 */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final SysAuditLogMapper auditLogMapper;

    public SysAuditLog getById(Long id) {
        return auditLogMapper.selectById(id);
    }

    public PageResult<SysAuditLog> search(String module, String action, Long userId,
                                           Instant startTime, Instant endTime,
                                           long offset, int size) {
        long total = auditLogMapper.countAll(module, action, userId, startTime, endTime);
        if (total == 0) {
            return PageResult.empty();
        }
        return PageResult.of(auditLogMapper.selectPage(module, action, userId, startTime, endTime, offset, size),
                total);
    }
}
