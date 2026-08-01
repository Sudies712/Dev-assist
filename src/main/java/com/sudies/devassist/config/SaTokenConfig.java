package com.sudies.devassist.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器：/api/** 默认登录校验，排除登录、刷新 Token 与文档入口。
 * 双 Token、登录失败锁定、数据范围隔离在认证切片完整实现（见关键流程详细设计 §1）。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/login",
                        "/api/refresh-token",
                        "/api/uploads/**",
                        "/v3/api-docs/**",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/webjars/**"
                );
    }
}
