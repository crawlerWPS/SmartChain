package com.scfs.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 角色 API 权限 - 对应 RFC 表3 sys_role_permission（schema_common）
 *
 * <p>permissions 为 JSONB，存储如 ["view","create","update","delete","export","approve","reject"]</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRolePermission extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 关联角色 */
    private Long roleId;
    /** 模块：GRAPH/VERIFY/PREAUDIT/RISK/RULE/USER/AUDIT */
    private String module;
    /** 权限列表 JSONB（MyBatis TypeHandler 转换） */
    private List<String> permissions;
}
