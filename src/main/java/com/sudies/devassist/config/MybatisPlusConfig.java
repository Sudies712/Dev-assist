package com.sudies.devassist.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.sudies.devassist.common.aspect.DataScopeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：分页插件 + 乐观锁插件。对应概要设计 §3.4 / §7.1。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DataScopeHandler dataScopeHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        DataPermissionInterceptor dataPermission = new DataPermissionInterceptor();
        dataPermission.setDataPermissionHandler(dataScopeHandler);
        interceptor.addInnerInterceptor(dataPermission);
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
