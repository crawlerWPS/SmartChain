package com.scfs.common.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单实体 - 对应 RFC 表3a sys_menu（schema_common）树形结构
 *
 * <p>双层权限：菜单权限（控制前端导航）+ API 权限（控制后端接口）</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysMenu extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 父菜单 ID，0=根节点 */
    private Long parentId;
    /** 菜单名称 */
    private String menuName;
    /** 菜单编码（唯一，如 graph.view） */
    private String menuCode;
    /** DIRECTORY（目录）/ MENU（菜单）/ BUTTON（按钮） */
    private String menuType;
    /** 前端路由路径（如 /graph/relations） */
    private String path;
    /** 前端组件路径（如 graph/relations） */
    private String component;
    /** 按钮权限标识（如 rule:approve） */
    private String permission;
    /** 菜单图标 */
    private String icon;
    /** 排序值（同级内升序） */
    private Integer sort;
    /** 1=显示, 0=隐藏（隐藏后路由仍可访问） */
    private Short visible;
    /** 1=启用, 0=禁用 */
    private Short status;
}
