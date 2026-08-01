package com.sudies.devassist.framework.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.sudies.devassist.common.utils.SecurityUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充：createTime/updateTime/createBy/updateBy。
 * updateBy/createBy 取当前登录用户（未登录时跳过，如系统初始化）。
 */
@Component
public class MetaObjectHandlerImpl implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        Long uid = SecurityUtils.currentUserIdOrNull();
        if (uid != null) {
            this.strictInsertFill(metaObject, "createBy", Long.class, uid);
            this.strictInsertFill(metaObject, "updateBy", Long.class, uid);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        Long uid = SecurityUtils.currentUserIdOrNull();
        if (uid != null) {
            this.strictUpdateFill(metaObject, "updateBy", Long.class, uid);
        }
    }
}
