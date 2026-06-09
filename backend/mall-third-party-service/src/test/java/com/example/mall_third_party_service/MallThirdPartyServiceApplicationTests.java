package com.example.mall_third_party_service;

import com.aliyun.oss.OSS;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
@TestPropertySource(locations = "classpath:/application.yml")
class MallThirdPartyServiceApplicationTests {

	@Autowired
	private OSS ossClient;

	@Test
	void contextLoads() {
	}

	@Test
	public void testUpload() throws FileNotFoundException {
//		String endpoint = "http://oss-cn-shanghai.aliyuncs.com";
//		String accessKeyId = "";
//		String accessKeySecret = "";
//		OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
		InputStream inputStream = new FileInputStream("C:\\Users\\zqne\\Desktop\\image\\微信图片_20260524193834_1197_1.jpg");
		ossClient.putObject("mall-prelud", "kin.jpg", inputStream);
		ossClient.shutdown();
		System.out.println("上传完成...");
	}
}
