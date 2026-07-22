package com.scfs.common.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 审计日志条目 - 对应 RFC 4.1.4 AuditEntry
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String module;
    private String action;
    private String targetType;
    private String targetId;
    private Map<String, Object> detail;
    private String ipAddress;
}
