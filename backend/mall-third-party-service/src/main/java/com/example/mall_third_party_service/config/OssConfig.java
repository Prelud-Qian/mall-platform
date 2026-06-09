package com.example.mall_third_party_service.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OssConfig {
    @Value("${spring.cloud.alicloud.access-key}")
    private String accessKey;

    @Value("${spring.cloud.alicloud.secret-key}")
    private String secretKey;

    @Value("${spring.cloud.alicloud.oss.endpoint}")
    private String endpoint;

    @Bean
    public OSS ossClient() {
        // 手动构建客户端，替代原来starter自动注入
        return new OSSClientBuilder().build(endpoint, accessKey, secretKey);
    }
}
