package com.sudies.devassist.common.aspect;

/**
 * 数据范围上下文：传递当前方法 @DataScope 的 project 列名给 SQL 拦截器。
 */
public final class DataScopeContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static void set(String projectColumn) {
        HOLDER.set(projectColumn);
    }

    public static String get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
