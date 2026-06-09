package com.mall.mall_ware;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mall.ware.ware")
@MapperScan("com.mall.ware.ware.dao")
public class MallWareApplication {

	public static void main(String[] args) {
		SpringApplication.run(MallWareApplication.class, args);
	}

}
