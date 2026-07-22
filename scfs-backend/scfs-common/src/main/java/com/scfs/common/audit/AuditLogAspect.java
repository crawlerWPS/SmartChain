package com.scfs.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scfs.common.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志 AOP 切面 - 对应 RFC 4.1.4 AuditLogService
 *
 * <p>关键策略：</p>
 * <ul>
 *   <li>基于 Spring AOP 自动记录 @Audit 注解的方法</li>
 *   <li>异步写入（@Async），不阻塞主业务</li>
 *   <li>detail 字段存 JSONB，包含变更前后差异</li>
 * </ul>
 *
 * <p>对应 RFC S2-9：实现 AuditLogService（异步写入 + @Audit 注解 + AOP）</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final SecurityContextHelper securityContextHelper;
    private final ObjectMapper objectMapper;

    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint pjp, Audit audit) throws Throwable {
        Object result = pjp.proceed();

        try {
            writeAuditLog(pjp, audit, result, null);
        } catch (Exception e) {
            log.warn("[Audit] 写入审计日志失败: {}", e.getMessage());
        }
        return result;
    }

    @Async
    protected void writeAuditLog(ProceedingJoinPoint pjp, Audit audit, Object result, Throwable error) {
        try {
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            Object[] args = pjp.getArgs();

            SecurityContextHelper.CurrentUser user = securityContextHelper.getCurrentUser();
            if (user == null) {
                log.warn("[Audit] 无当前用户，跳过审计记录");
                return;
            }

            // 构造 SpEL 上下文（参数名 -> 参数值）
            EvaluationContext ctx = buildSpelContext(method, args);
            if (result != null) {
                ctx.setVariable("result", result);
            }

            // 解析 targetId 表达式
            String targetId = "";
            if (!audit.targetIdExpr().isEmpty()) {
                try {
                    Expression exp = parser.parseExpression(audit.targetIdExpr());
                    Object targetIdVal = exp.getValue(ctx);
                    targetId = targetIdVal == null ? "" : targetIdVal.toString();
                } catch (Exception e) {
                    log.debug("[Audit] targetIdExpr 解析失败: {}", e.getMessage());
                }
            }

            // 构造审计详情
            Map<String, Object> detail = new HashMap<>();
            if (audit.snapshot()) {
                detail.put("args", sanitizeArgs(args));
                if (result != null) {
                    detail.put("result", result);
                }
                if (error != null) {
                    detail.put("error", error.getMessage());
                }
            }

            AuditEntry entry = AuditEntry.builder()
                    .userId(user.userId())
                    .username(user.username())
                    .module(audit.module())
                    .action(audit.action())
                    .targetType(audit.targetType())
                    .targetId(targetId)
                    .detail(detail)
                    .ipAddress(user.ipAddress())
                    .build();

            auditLogService.log(entry);
        } catch (Exception e) {
            log.warn("[Audit] 写入审计日志异常: {}", e.getMessage());
        }
    }

    private EvaluationContext buildSpelContext(Method method, Object[] args) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length && i < args.length; i++) {
            ctx.setVariable(params[i].getName(), args[i]);
        }
        return ctx;
    }

    private Map<String, Object> sanitizeArgs(Object[] args) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg != null) {
                try {
                    // 脱敏：移除密码等敏感字段
                    String json = objectMapper.writeValueAsString(arg);
                    if (json.toLowerCase().contains("password")) {
                        json = json.replaceAll("(?i)\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"");
                    }
                    map.put("arg" + i, json);
                } catch (Exception e) {
                    map.put("arg" + i, arg.toString());
                }
            }
        }
        return map;
    }
}
