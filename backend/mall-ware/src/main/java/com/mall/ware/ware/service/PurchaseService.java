package com.mall.ware.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.ware.ware.entity.PurchaseEntity;

import java.util.Map;

/**
 * 采购信息
 *
 * @author preludqian
 * @email 2551932043@qq.com
 * @date 2026-05-20 14:04:15
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

