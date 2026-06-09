package com.example.mall_third_party_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(
		exclude = {
				DataSourceAutoConfiguration.class,
				com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure.class,
				com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class
		}
)
@EnableDiscoveryClient
public class MallThirdPartyServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MallThirdPartyServiceApplication.class, args);
	}

}
