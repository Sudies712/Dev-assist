package com.sudies.devassist;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * dev-assist 平台启动类。
 *
 * <p>包名 {@code com.sudies.devassist}（与 pom groupId 对齐）；
 * {@code @MapperScan} 扫描所有模块（含 modules.* 与 ai）下的 mapper 子包；
 * {@code @EnableAsync} 开启异步支持，供文档异步解析（RAG 摄入）使用。
 */
@SpringBootApplication
@EnableAsync
@MapperScan("com.sudies.devassist.**.mapper")
public class DevAssistApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevAssistApplication.class, args);
    }
}
