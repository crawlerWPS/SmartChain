package com.scfs.common.security;

import com.scfs.common.core.BusinessException;
import com.scfs.common.core.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

/**
 * 权限校验切面 - 对应 RFC 4.1.3 PermissionChecker
 *
 * <p>基于 sys_role_permission 校验 module + action 是否允许</p>
 *
 * <p>关键逻辑：</p>
 * <ol>
 *   <li>从 SecurityContext 获取当前用户角色</li>
 *   <li>查 sys_role_permission 获取 API 权限</li>
 *   <li>校验 module + action 是否允许</li>
 *   <li>不通过抛 PermissionDeniedException (code=1003)</li>
 * </ol>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionCheckerAspect {

    private final SecurityContextHelper securityContextHelper;

    @Around("@annotation(requirePermission)")
    public Object check(ProceedingJoinPoint pjp, RequirePermission requirePermission) throws Throwable {
        String module = requirePermission.module();
        String action = requirePermission.action();

        SecurityContextHelper.CurrentUser current = securityContextHelper.getCurrentUser();
        if (current == null) {
            log.warn("[Permission] 未认证用户访问 module={}, action={}", module, action);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // ADMIN 角色直接放行
        if ("ADMIN".equals(current.roleCode())) {
            return pjp.proceed();
        }

        Map<String, List<String>> rolePerms = securityContextHelper.getRolePermissions(current.roleCode());
        List<String> actions = rolePerms.get(module);
        if (actions == null || !actions.contains(action)) {
            log.warn("[Permission] 用户 {} 无权限访问 module={}, action={}",
                    current.username(), module, action);
            throw new BusinessException(ErrorCode.PERMISSION_DENIED,
                    String.format("无权限：%s.%s", module, action));
        }

        // 记录审计请求 IP
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            current.ipAddress(request.getRemoteAddr());
        }

        return pjp.proceed();
    }
}
