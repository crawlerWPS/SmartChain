package com.scfs.common.mapper;

import com.scfs.common.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper - 对应 RFC 表1 sys_user
 */
@Mapper
public interface SysUserMapper {

    SysUser selectById(@Param("id") Long id);

    SysUser selectByUsername(@Param("username") String username);

    List<SysUser> selectPage(@Param("keyword") String keyword,
                             @Param("roleCode") String roleCode,
                             @Param("offset") long offset,
                             @Param("size") int size);

    long countAll(@Param("keyword") String keyword, @Param("roleCode") String roleCode);

    int insert(SysUser user);

    int update(SysUser user);

    int updateStatus(@Param("id") Long id, @Param("status") Short status);
}
