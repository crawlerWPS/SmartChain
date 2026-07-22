package com.scfs.common.mapper;

import com.scfs.common.audit.AuditEntry;
import com.scfs.common.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

/**
 * 审计日志 Mapper - 对应 RFC 表4 sys_audit_log
 */
@Mapper
public interface SysAuditLogMapper {

    int insert(SysAuditLog auditLog);

    SysAuditLog selectById(@Param("id") Long id);

    List<SysAuditLog> selectPage(@Param("module") String module,
                                 @Param("action") String action,
                                 @Param("userId") Long userId,
                                 @Param("startTime") Instant startTime,
                                 @Param("endTime") Instant endTime,
                                 @Param("offset") long offset,
                                 @Param("size") int size);

    long countAll(@Param("module") String module,
                  @Param("action") String action,
                  @Param("userId") Long userId,
                  @Param("startTime") Instant startTime,
                  @Param("endTime") Instant endTime);
}
