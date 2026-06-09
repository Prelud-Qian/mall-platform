package com.mall.product.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.constant.ProductConstant;
import com.mall.product.product.dao.AttrAttrgroupRelationDao;
import com.mall.product.product.dao.AttrGroupDao;
import com.mall.product.product.dao.CategoryDao;
import com.mall.product.product.entity.AttrAttrgroupRelationEntity;
import com.mall.product.product.entity.AttrGroupEntity;
import com.mall.product.product.entity.CategoryEntity;
import com.mall.product.product.service.CategoryService;
import com.mall.product.product.vo.AttrRespVo;
import com.mall.product.product.vo.AttrVo;
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

import com.mall.product.product.dao.AttrDao;
import com.mall.product.product.entity.AttrEntity;
import com.mall.product.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Autowired
    CategoryService categoryService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) throws InvocationTargetException, IllegalAccessException {
        AttrEntity attrEntity = new AttrEntity();
        // copyProperties(目标对象, 源对象)
        BeanUtils.copyProperties(attrEntity, attr);
        // 保存基本数据
        this.save(attrEntity);
        // 保存关联关系
        if (attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            relationDao.insert(relationEntity);
        }
    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type) {
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>().eq("attr_type", "base".equalsIgnoreCase(type) ? ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() : ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());

        /**
         * 如果前端传了分类 ID（比如只查「手机」分类下的属性），
         * 就加条件：WHERE catelog_id = 传入的id；不传就查全部分类属性。
         */
        if (catelogId != 0){
            queryWrapper.eq("catelog_id", catelogId);
        }

        /**
         * 如果前端输入了搜索关键字：
         * 拼接条件 AND (attr_id=关键字 OR attr_name LIKE %关键字%)
         * 括号是 lambda 包出来的，保证 OR 逻辑不会打乱前面分类筛选条件。
         */
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)){
            queryWrapper.and((wrapper)->{
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        /**
         * new Query().getPage(params)：从 url?page=1&limit=10取出页码、每页条数
         * this.page(分页对象,查询条件)：执行 SQL 分页查询pms_attr，拿到一页的属性实体列表
         * 结果存在IPage分页对象里，包含总条数、当前页数据
         */
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );

        // 包装分页返回工具类
        PageUtils pageUtils = new PageUtils(page);
        // page.getRecords()：取出当前页所有查到的AttrEntity（数据库原始数据）
        List<AttrEntity> records = page.getRecords();
        // 流式循环：把数据库实体 → 前端 VO
        List<AttrRespVo> respVos = records.stream().map((attrEntity) -> {
            // 对每一条查到的属性实体，加工成前端能直接渲染的AttrRespVo对象，循环处理每一行属性
            AttrRespVo attrRespVo = new AttrRespVo();
            try {
                // 把AttrEntity里所有基础字段（attrName、icon、valueSelect 等）一键复制给 VO
                BeanUtils.copyProperties(attrRespVo, attrEntity);

                if ("base".equalsIgnoreCase(type)){
                    // 设置分组名称
                    // 拿当前属性的 attrId，去中间绑定表查它绑定的分组 ID
                    AttrAttrgroupRelationEntity attrId = relationDao.selectOne(
                            new QueryWrapper<AttrAttrgroupRelationEntity>()
                                    .eq("attr_id", attrEntity.getAttrId()));
                    if (attrId != null) {
                        // 拿着分组 ID 去分组表查分组名字
                        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrId.getAttrGroupId());
                        attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                    }
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            // 属性表里只存了数字catelogId，拿这个 ID 去分类表查分类全名，塞进 VO 给页面展示
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }

            // 每一行实体加工完变成 VO，全部收集成 VO 集合
            return attrRespVo;
        }).collect(Collectors.toList());

        // 替换分页里的原始数据，返回前端
        pageUtils.setList(respVos);
        return pageUtils;
    }

    @Override
    public AttrRespVo getAttrInfo(Long attrId) throws InvocationTargetException, IllegalAccessException {
        AttrRespVo respVo = new AttrRespVo();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(respVo, attrEntity);

        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            // 设置分组信息
            AttrAttrgroupRelationEntity attrAttrgroupRelation = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if (attrAttrgroupRelation != null){
                respVo.setAttrGroupId(attrAttrgroupRelation.getAttrGroupId());
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrAttrgroupRelation.getAttrGroupId());
                if (attrGroupEntity != null){
                    respVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }

        // 设置分类信息
        Long catelogId = attrEntity.getCatelogId();
        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        respVo.setCatelogPath(catelogPath);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if (categoryEntity != null){
            respVo.setCatelogName(categoryEntity.getName());
        }

        return respVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) throws InvocationTargetException, IllegalAccessException {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attrEntity, attr);
        this.updateById(attrEntity);

        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()){
            // 修改分组关联
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attr.getAttrId());

            Integer count = Math.toIntExact(relationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId())));
            if (count > 0){
                relationDao.update(relationEntity, new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            }else{
                relationDao.insert(relationEntity);
            }
        }
    }
}