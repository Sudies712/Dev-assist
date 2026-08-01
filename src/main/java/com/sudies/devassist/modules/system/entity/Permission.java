package com.sudies.devassist.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sudies.devassist.framework.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 权限（菜单 MENU / 按钮 BUTTON，自引用树）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("permission")
public class Permission extends BaseEntity {

    /**
     * 父权限 id，0=顶级
     */
    private Long parentId;

    private String permName;

    /**
     * 权限码：模块:操作 或 menu:xxx
     */
    private String permCode;

    /**
     * MENU / BUTTON
     */
    private String permType;

    /**
     * 路由路径（菜单）
     */
    private String path;

    /**
     * 前端组件路径（菜单）
     */
    private String component;

    private String icon;

    private Integer sort;

    /**
     * 菜单是否可见
     */
    private Integer visible;

    private String status;
}
