package com.mall.order.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.order.order.entity.OrderEntity;

import java.util.Map;

/**
 * 订单
 *
 * @author preludqian
 * @email 2551932043@qq.com
 * @date 2026-05-20 13:53:42
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

