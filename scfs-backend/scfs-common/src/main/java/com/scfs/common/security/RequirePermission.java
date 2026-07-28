package com.scfs.common.security;

import com.scfs.common.constant.ScfsConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解 - 对应 RFC 4.1.3 PermissionChecker
 *
 * <p>使用方式：</p>
 * <pre>
 * &#64;PostMapping("/applications")
 * &#64;RequirePermission(module = "VERIFY", action = "create")
 * public Result createApplication(&#64;RequestBody ApplicationDTO dto) { ... }
 * </pre>
 *
 * <p>由 {@link PermissionCheckerAspect} AOP 切面统一拦截校验</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** 模块：GRAPH/VERIFY/PREAUDIT/RISK/RULE/USER/AUDIT */
    String module();

    /** 操作：view/create/update/delete/export/approve/reject */
    String action() default ScfsConstants.ACTION_VIEW;

    /** 权限标识，默认与 action 一致 */
    String permission() default ScfsConstants.ACTION_VIEW;
}
