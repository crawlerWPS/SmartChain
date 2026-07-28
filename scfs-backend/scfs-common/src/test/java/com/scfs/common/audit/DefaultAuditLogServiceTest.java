package com.scfs.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scfs.common.entity.SysAuditLog;
import com.scfs.common.mapper.SysAuditLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DefaultAuditLogService 单元测试
 *
 * <p>验证审计日志异步写入逻辑：字段映射正确、异常不外抛。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("审计日志服务测试")
class DefaultAuditLogServiceTest {

    @Mock
    private SysAuditLogMapper auditLogMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private DefaultAuditLogService service;

    @Test
    @DisplayName("log 应正确映射 AuditEntry 到 SysAuditLog 并插入")
    void log_shouldMapEntryAndInsert() {
        Map<String, Object> detail = new HashMap<>();
        detail.put("action", "test");

        AuditEntry entry = AuditEntry.builder()
                .userId(1L)
                .username("admin")
                .module("RULE")
                .action("CREATE")
                .targetType("RULE_DEFINITION")
                .targetId("42")
                .detail(detail)
                .ipAddress("192.168.1.1")
                .build();

        service.log(entry);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogMapper).insert(captor.capture());

        SysAuditLog saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals("admin", saved.getUsername());
        assertEquals("RULE", saved.getModule());
        assertEquals("CREATE", saved.getAction());
        assertEquals("RULE_DEFINITION", saved.getTargetType());
        assertEquals("42", saved.getTargetId());
        assertEquals("192.168.1.1", saved.getIpAddress());
        assertNotNull(saved.getDetail());
        assertEquals("test", saved.getDetail().get("action"));
    }

    @Test
    @DisplayName("log 在 Mapper 抛异常时不应外抛")
    void log_shouldSwallowMapperException() {
        AuditEntry entry = AuditEntry.builder()
                .userId(1L)
                .username("admin")
                .module("RULE")
                .action("CREATE")
                .build();

        doThrow(new RuntimeException("DB 连接失败"))
                .when(auditLogMapper).insert(any(SysAuditLog.class));

        // 不应抛异常
        assertDoesNotThrow(() -> service.log(entry));

        verify(auditLogMapper).insert(any(SysAuditLog.class));
    }

    @Test
    @DisplayName("log 在 detail 为 null 时应正常处理")
    void log_shouldHandleNullDetail() {
        AuditEntry entry = AuditEntry.builder()
                .userId(2L)
                .username("operator")
                .module("USER")
                .action("LOGIN")
                .detail(null)
                .build();

        service.log(entry);

        ArgumentCaptor<SysAuditLog> captor = ArgumentCaptor.forClass(SysAuditLog.class);
        verify(auditLogMapper).insert(captor.capture());

        SysAuditLog saved = captor.getValue();
        assertNull(saved.getDetail());
        assertEquals("LOGIN", saved.getAction());
    }
}
