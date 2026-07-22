package com.scfs.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体 - 对应 RFC 表2 sys_role（schema_common）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRole extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 角色编码：RM/RCO/OPS_MAKER/OPS_CHECKER/OPS/AUDIT/ADMIN */
    private String roleCode;
    /** 角色名称 */
    private String roleName;
    /** 角色类型：BUSINESS/RISK_CONTROL/CONFIG_MAKER/CONFIG_CHECKER/OPS/AUDIT/SYSTEM */
    private String roleType;
    /** 角色描述 */
    private String description;
    /** 1=启用, 0=禁用 */
    private Short status;
}
