package com.scfs.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 用户实体 - 对应 RFC 表1 sys_user（schema_common）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 登录名 */
    private String username;
    /** 密码哈希（BCrypt） */
    private String passwordHash;
    /** 真实姓名 */
    private String realName;
    /** 关联 sys_role.role_code */
    private String roleCode;
    /** 邮箱 */
    private String email;
    /** 手机号 */
    private String phone;
    /** 1=启用, 0=禁用 */
    private Short status;
}
