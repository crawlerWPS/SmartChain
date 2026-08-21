package com.scfs.common.mapper;

import com.scfs.common.entity.SysRole;
import com.scfs.common.entity.SysRolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色 Mapper - 对应 RFC 表2 sys_role / 表3 sys_role_permission
 */
@Mapper
public interface SysRoleMapper {

    SysRole selectById(@Param("id") Long id);

    SysRole selectByCode(@Param("roleCode") String roleCode);

    List<SysRole> selectAll();

    List<SysRolePermission> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    int insert(SysRole role);

    int update(SysRole role);

    int insertPermission(SysRolePermission perm);

    int deletePermissionsByRoleId(@Param("roleId") Long roleId);

    int deleteById(@Param("id") Long id);
}
