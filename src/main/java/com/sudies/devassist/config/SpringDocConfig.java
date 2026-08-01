package com.sudies.devassist.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 配置（替代 Knife4j）。
 */
@Configuration
public class SpringDocConfig {

    @Bean
    public OpenAPI devAssistOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("dev-assist API")
                .description("软件项目开发辅助管理平台接口文档")
                .version("v1"));
    }
}
