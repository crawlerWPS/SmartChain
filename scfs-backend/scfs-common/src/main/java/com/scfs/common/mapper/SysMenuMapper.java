package com.scfs.common.mapper;

import com.scfs.common.entity.SysMenu;
import com.scfs.common.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单 Mapper - 对应 RFC 表3a sys_menu / 表3b sys_role_menu
 */
@Mapper
public interface SysMenuMapper {

    SysMenu selectById(@Param("id") Long id);

    SysMenu selectByCode(@Param("menuCode") String menuCode);

    List<SysMenu> selectAll();

    /** 查询所有菜单（管理员视图） */
    List<SysMenu> selectAllTree();

    /** 查询角色关联的菜单 ID 列表 */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /** 查询角色关联的菜单（含完整字段，用于构建菜单树） */
    List<SysMenu> selectMenusByRoleId(@Param("roleId") Long roleId);

    int insert(SysMenu menu);

    int update(SysMenu menu);

    int deleteById(@Param("id") Long id);

    int insertRoleMenu(SysRoleMenu roleMenu);

    int deleteRoleMenuByRoleId(@Param("roleId") Long roleId);

    int copyRoleMenus(@Param("fromRoleId") Long fromRoleId, @Param("toRoleId") Long toRoleId);
}
