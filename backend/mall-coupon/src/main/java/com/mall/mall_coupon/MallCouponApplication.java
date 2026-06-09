package com.mall.mall_coupon;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 如何使用Nacos作为配置中心统一管理配置
 * 		引入依赖
 *		<dependency>
 * 			<groupId>com.alibaba.cloud</groupId>
 * 			<artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
 * 		</dependency>
 *
 *		在application.properties中配置
 *		spring.application.name=mall-coupon
 * 		spring.cloud.nacos.config.server-addr=127.0.0.1:8848
 * 		spring.config.import=nacos:/${spring.application.name}.properties
 *
 *		在Nacos 服务端的管理后台 配置列表中 添加一个 相对应的Data ID
 *
 *		在Controller 类上加 @RefreshScope注解
 */
@SpringBootApplication(scanBasePackages = "com.mall.coupon.coupon")
@MapperScan("com.mall.coupon.coupon.dao")
@EnableDiscoveryClient
public class MallCouponApplication {

	public static void main(String[] args) {
		SpringApplication.run(MallCouponApplication.class, args);
	}

}
