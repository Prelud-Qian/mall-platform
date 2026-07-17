package com.mall.product.product.service.impl;

import com.mall.product.product.entity.AttrEntity;
import com.mall.product.product.service.AttrService;
import com.mall.product.product.vo.AttrGroupWithAttrsVo;
import com.mall.product.product.vo.SkuItemVo;
import com.mall.product.product.vo.SpuItemAttrGroupVo;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.product.dao.AttrGroupDao;
import com.mall.product.product.entity.AttrGroupEntity;
import com.mall.product.product.service.AttrGroupService;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        // select * from pms_attr_group where catelog id=? and (attr_group_id = key or attr_group_name like %key%)
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if (!StringUtils.isEmpty(key)){
            wrapper.and((obj) -> {
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }
        if (catelogId == 0){
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);
        }else {
            wrapper.eq("catelog_id", catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);
        }
    }

    /**
     * 根据分类id查出所有的分组以及这些组里面的属性
     * @param catelogId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {

        // 查询分组信息
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));

        // 查询所有属性
        List<AttrGroupWithAttrsVo> collect = attrGroupEntities.stream().map(group -> {
            AttrGroupWithAttrsVo attrVo = new AttrGroupWithAttrsVo();
            try {
                BeanUtils.copyProperties(attrVo, group);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            List<AttrEntity> attrs = attrService.getRelationAttr(attrVo.getAttrGroupId());
            attrVo.setAttrs(attrs);
            return attrVo;
        }).collect(Collectors.toList());

        return collect;
    }

    @Override
    public List<SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        // 查出当前spu对应的所有属性的分组信息以及当前分组下的所有属性对应的值
        AttrGroupDao baseMapper = this.getBaseMapper();
        List<SpuItemAttrGroupVo> vos = baseMapper.getAttrGroupWithAttrsBySpuId(spuId, catalogId);

        return vos;
    }
}