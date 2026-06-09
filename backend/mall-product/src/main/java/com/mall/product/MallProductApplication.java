package com.mall.product;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 1.整合MyBatis-Plus
 * 		导入依赖
 * 		<dependency>
 * 			<groupId>com.baomidou</groupId>
 * 			<artifactId>mybatis-plus-boot-starter</artifactId>
 * 			<version>3.2.0</version>
 * 		</dependency>
 * 		配置
 * 			配置数据源
 * 				导入数据库的驱动
 *				在application.yml配置数据源相关信息
 * 			配置MyBatis-Plus
 *				使用@MapperScan
 *				告诉MyBatis-Plus，sql映射文件位置
 */
@MapperScan("com.mall.product.product.dao")
@SpringBootApplication (scanBasePackages = "com.mall.product")
@EnableDiscoveryClient
public class MallProductApplication {

	public static void main(String[] args) {
		org.springframework.boot.SpringApplication.run(MallProductApplication.class, args);
	}

}
