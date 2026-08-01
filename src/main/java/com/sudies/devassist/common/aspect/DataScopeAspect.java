package com.sudies.devassist.common.aspect;

import com.sudies.devassist.common.annotation.DataScope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @DataScope 切面：方法执行前把 project 列名放进 ThreadLocal，执行后清理。
 * SQL 拦截器（DataScopeHandler）读取 ThreadLocal 决定是否追加过滤条件。
 */
@Aspect
@Component
public class DataScopeAspect {

    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint pjp, DataScope dataScope) throws Throwable {
        DataScopeContext.set(dataScope.value());
        try {
            return pjp.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }
}
