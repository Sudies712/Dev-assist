package com.sudies.devassist.common.annotation;

import com.sudies.devassist.common.aspect.DataScopeAspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据范围隔离：标注在 Service 查询方法上，由 {@link DataScopeAspect} 配合
 * DataScopeHandler 自动追加 {@code project_id IN (当前用户可见项目)} 过滤条件。
 * <p>对应关键流程详细设计 §1.3：ADMIN 全局；其余角色按 project_member 收敛。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /**
     * SQL 中 project 列名（默认 project_id），支持 "别名.project_id" 形式
     */
    String value() default "project_id";
}
