package com.scfs.common.audit;

/**
 * 审计日志服务接口 - 对应 RFC 4.1.4 AuditLogService
 *
 * <p>异步写入 sys_audit_log，按月分区</p>
 */
public interface AuditLogService {

    /**
     * 记录审计日志（异步）
     */
    void log(AuditEntry entry);
}
