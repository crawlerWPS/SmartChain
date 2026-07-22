package com.scfs.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scfs.common.entity.SysAuditLog;
import com.scfs.common.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务默认实现 - 对应 RFC 4.1.4 AuditLogService
 *
 * <p>异步写入 sys_audit_log（按月分区），失败仅记录日志不抛异常</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAuditLogService implements AuditLogService {

    private final SysAuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    @Async
    @Override
    public void log(AuditEntry entry) {
        try {
            SysAuditLog auditLog = new SysAuditLog();
            auditLog.setUserId(entry.getUserId());
            auditLog.setUsername(entry.getUsername());
            auditLog.setModule(entry.getModule());
            auditLog.setAction(entry.getAction());
            auditLog.setTargetType(entry.getTargetType());
            auditLog.setTargetId(entry.getTargetId());
            auditLog.setDetail(entry.getDetail());
            auditLog.setIpAddress(entry.getIpAddress());

            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            log.warn("[Audit] 写入 sys_audit_log 失败: {}", e.getMessage());
        }
    }
}
