package com.scfs.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解 - 对应 RFC 4.1.4 AuditLogService
 *
 * <p>使用方式：</p>
 * <pre>
 * &#64;Audit(module = "RULE", action = "CREATE", targetType = "RULE_DEFINITION")
 * public Long createRule(RuleCreateDTO dto) { ... }
 * </pre>
 *
 * <p>由 {@link AuditLogAspect} AOP 切面统一拦截，异步写入 sys_audit_log</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /** 模块：GRAPH/VERIFY/PREAUDIT/RISK/RULE/USER */
    String module();

    /** 操作：LOGIN/CREATE/UPDATE/DELETE/EXPORT/APPROVE/REJECT */
    String action();

    /** 操作对象类型，可使用 SpEL 表达式（如 #dto.id） */
    String targetType() default "";

    /** 操作对象 ID 表达式（SpEL，如 #id 或 #result.id） */
    String targetIdExpr() default "";

    /** 是否记录变更前后快照 */
    boolean snapshot() default false;
}
