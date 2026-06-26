package com.example.mall_search;

import com.alibaba.fastjson.JSON;
import com.example.mall_search.config.ElasticSearchConfig;
import lombok.Data;
import org.apache.shiro.authc.Account;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
class MallSearchApplicationTests {

	@Autowired
	private RestHighLevelClient client;

	@Test
	void contextLoads() {
		System.out.println(client);
	}


	/**
	 * 测试存储数据到es
	 */

	@Data
	class User{
		private String userName;
		private String gender;
		private Integer age;
	}

	@Test
	public void indexData() throws IOException {
		IndexRequest indexRequest = new IndexRequest("users");
		indexRequest.id("1");
//		indexRequest.source("userName", "zhangsan", "age", 18, "gender", "男");
		User user = new User();
		user.setUserName("zhangsan");
		user.setAge(18);
		user.setGender("男");
		String jsonString = JSON.toJSONString(user);
		indexRequest.source(jsonString, XContentType.JSON);

		// 执行操作
		IndexResponse index = client.index(indexRequest, ElasticSearchConfig.COMMON_OPTIONS);

		// 提取有用的响应数据
		System.out.println(index);
	}

	@Test
	public void searchData() throws IOException{
		// 创建检索请求
		SearchRequest searchRequest = new SearchRequest();
		// 指定索引
		searchRequest.indices("bank");
		// 指定DSL，检索条件
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		// 构造检索条件
//		searchSourceBuilder.query();
//		searchSourceBuilder.from();
//		searchSourceBuilder.size();
//		searchSourceBuilder.aggregation();
		searchSourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));

		// 按照年龄的值分布进行聚合
		TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
		searchSourceBuilder.aggregation(ageAgg);

		// 计算平均薪资
		AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
		searchSourceBuilder.aggregation(balanceAvg);

		System.out.println("检索条件：" + searchSourceBuilder.toString());
		searchRequest.source(searchSourceBuilder);

		// 执行检索
		SearchResponse searchResponse = client.search(searchRequest, ElasticSearchConfig.COMMON_OPTIONS);

		// 分析结果
		System.out.println(searchResponse.toString());
		Map map = JSON.parseObject(searchResponse.toString(), Map.class);
		// 获取所有查到的数据
		SearchHits hits = searchResponse.getHits();
		SearchHit[] searchHits = hits.getHits();
		for (SearchHit hit : searchHits) {
			String string = hit.getSourceAsString();
		}
	}

}
