package com.mall.ware.ware.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@MapperScan("com.mall.ware.ware.dao")
@Configuration
public class WareMyBatisConfig {
    // 引入分页插件
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 创建分页插件，并配置和老版本一样的参数
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);

        // 1. 当请求的页码超过最大页时，true 调回首页（和 setOverflow(true) 效果一致）
        paginationInnerInterceptor.setOverflow(true);

        // 2. 设置最大单页限制为 1000 条（和 setLimit(1000) 效果一致）
        paginationInnerInterceptor.setMaxLimit(1000L);

        // 把分页插件加入到拦截器中
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        return interceptor;
    }
}
