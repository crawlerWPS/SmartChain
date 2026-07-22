package com.scfs.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色-菜单关联 - 对应 RFC 表3b sys_role_menu（schema_common）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleMenu extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 关联角色 */
    private Long roleId;
    /** 关联菜单（含目录/菜单/按钮） */
    private Long menuId;
}
