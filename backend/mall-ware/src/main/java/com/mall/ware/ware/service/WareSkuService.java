package com.mall.ware.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.ware.ware.entity.WareSkuEntity;

import java.util.Map;

/**
 * 商品库存
 *
 * @author preludqian
 * @email 2551932043@qq.com
 * @date 2026-05-20 14:04:15
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

