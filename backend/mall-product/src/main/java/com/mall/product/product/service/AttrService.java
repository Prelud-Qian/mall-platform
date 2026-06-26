package com.mall.product.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mall.common.utils.PageUtils;
import com.mall.product.product.entity.AttrEntity;
import com.mall.product.product.vo.AttrGroupRelationVo;
import com.mall.product.product.vo.AttrRespVo;
import com.mall.product.product.vo.AttrVo;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author preludqian
 * @email 2551932043@qq.com
 * @date 2026-05-19 21:42:34
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr) throws InvocationTargetException, IllegalAccessException;

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type);

    AttrRespVo getAttrInfo(Long attrId) throws InvocationTargetException, IllegalAccessException;

    void updateAttr(AttrVo attr) throws InvocationTargetException, IllegalAccessException;

    List<AttrEntity> getRelationAttr(Long attrgroupId);

    void deleteRelation(AttrGroupRelationVo[] vos);

    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId);

    List<Long> selectSearchAttrIds(List<Long> attrIds);
}

