package com.example.mall_search.controller;

import com.example.mall_search.service.ProductSaveService;
import com.exception.BizCodeEnum;
import com.mall.common.utils.R;
import com.to.es.SkuEsModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequestMapping("/search/save")
@RestController
public class ElasticSaveController {

    @Autowired
    ProductSaveService productSaveService;

    // 上架商品
    @PostMapping("/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels) {
        // b 用来接收批量写入 ES 的执行结果：true= 存在失败条目，false= 全部成功
        boolean b = false;
        try{
            // 调用业务层 productSaveService 的商品上架方法，内部执行 ES Bulk 批量新增文档
            b = productSaveService.productStatusUp(skuEsModels);
        }catch (Exception e){
            log.error("ElasticSaveController商品上架错误：{}", e);
            return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        }

        return !b ? R.ok() : R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
    }
}
