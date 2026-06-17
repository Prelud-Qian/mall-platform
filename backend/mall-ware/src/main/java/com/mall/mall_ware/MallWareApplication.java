package com.mall.mall_ware;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

//@SpringBootApplication(scanBasePackages = "com.mall.ware.ware")
@SpringBootApplication(scanBasePackages = {"com.mall.ware.ware", "com.exception"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.mall.ware.ware.feign")
public class MallWareApplication {

	public static void main(String[] args) {
		SpringApplication.run(MallWareApplication.class, args);
	}

}
