package com.mall.order.order.dao;

import com.mall.order.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author preludqian
 * @email 2551932043@qq.com
 * @date 2026-05-20 13:53:42
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
