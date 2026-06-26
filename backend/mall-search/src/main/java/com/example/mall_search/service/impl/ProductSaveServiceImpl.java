package com.example.mall_search.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.mall_search.config.ElasticSearchConfig;
import com.example.mall_search.constant.EsConstant;
import com.example.mall_search.service.ProductSaveService;
import com.to.es.SkuEsModel;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("ProductSaveService")
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    RestHighLevelClient restHighLevelClient;


    /**
     * 批量写入 ES
     * @param skuEsModels
     * @return
     * @throws IOException
     */
    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        // 保存到es中
        // 给es中建立索引 product，建立好映射关系

        // 给es中保存这些数据
        // 创建 ES 批量请求容器，用来收纳多条新增文档请求，一次性发送，减少多次网络请求损耗。
        BulkRequest bulkRequest = new BulkRequest();
        // 循环遍历每一个待上架 SKU，逐个构建单条新增请求
        for (SkuEsModel skuEsModel : skuEsModels) {
            // 构造保存请求
            // 创建单条文档写入请求，指定目标索引 product
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            // 指定 ES 文档唯一 ID 为当前 SKU 的 skuId
            /**
             * 无此 ID：新增文档；
             * 已有此 ID：覆盖更新
             */
            indexRequest.id(skuEsModel.getSkuId().toString());
            // FastJSON 把 SKU 实体转为 JSON 字符串，ES 只接收 JSON 格式数据。
            String s = JSON.toJSONString(skuEsModel);
            // 把 JSON 数据放入请求体，声明数据类型为 JSON。
            indexRequest.source(s, XContentType.JSON);

            // 将当前 SKU 的写入请求存入批量容器，等待统一提交。
            bulkRequest.add(indexRequest);
        }

        /**
         * 调用 ES 客户端发送批量请求；
         * 参数 1：封装好的所有写入请求；
         * 参数 2：全局统一 ES 请求配置；
         */
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, ElasticSearchConfig.COMMON_OPTIONS);

        // 判断是否有失败条目
        // 只要这批请求里任意一条 SKU 写入失败，就返回 true
        boolean b = bulk.hasFailures();
        // 进入条件：存在写入失败的 SKU
        if (b){
            // 只过滤失败的item，提取失败id
            List<String> failSkuIds = Arrays.stream(bulk.getItems())
                    .filter(item -> item.isFailed())
                    .map(item -> item.getId())
                    .collect(Collectors.toList());
            log.error("商品上架失败，失败skuId列表：{}", failSkuIds);
        }

        return b;
    }
}
