package com.sudies.devassist.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置（开发期前后端分离）。
 * 同时挂载 /api/uploads/** 静态资源映射，使上传的头像等文件可通过 URL 直接访问。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${dev-assist.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 末尾必须带 "/"，否则资源定位失败
        String location = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations("file:" + location);
    }
}
