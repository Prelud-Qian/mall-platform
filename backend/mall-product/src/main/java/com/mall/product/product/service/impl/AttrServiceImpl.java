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
import com.mall.product.product.vo.AttrGroupRelationVo;
import com.mall.product.product.vo.AttrRespVo;
import com.mall.product.product.vo.AttrVo;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
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
        if (attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId() != null){
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
                    if (attrId != null && attrId.getAttrGroupId() != null) {
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

    /**
     * 根据分组id查找关联的所有基本属性
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> entities = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));

        List<Long> attrIds = entities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        // 增加空判断：没有关联属性直接返回空集合，不查数据库
        if (attrIds.isEmpty()) {
            return new ArrayList<>();
        }

        Collection<AttrEntity> attrEntities = this.listByIds(attrIds);
        return (List<AttrEntity>) attrEntities;
    }

    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {
        // 把前端传过来的 VO 对象 → 转换成数据库实体类对象
        /**
         * 把数组转成 List 方便处理
         * 遍历每一条要删除的关联
         * 创建数据库对应的实体对象
         * 把 Vo 的 attrId、attrGroupId 复制到实体类
         * 把所有实体收集到一个 list
         */
        List<AttrAttrgroupRelationEntity> entities = Arrays.asList(vos).stream().map((item) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            try {
                BeanUtils.copyProperties(relationEntity, item);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            return relationEntity;
        }).collect(Collectors.toList());
        // 调用 DAO，批量删除这些关联
        relationDao.deleteBatchRelation(entities);
    }

    /**
     * 获取当前分组没有关联的所有属性
     * @param params
     * @param attrgroupId
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {

        /**
         * 多个属性分组 同属于 一个分类
         * 一个分类下有 多个基础属性
         * 属性分组 只能关联自己所属分类内的基础属性
         * 如果某一个属性分组关联了一个属性，那么同一分类下的其他属性分组就不能查询出且关联上这个属性了
         */

        /**
         * 根据传入的分组 ID，去pms_attr_group查询整条分组数据
         * 取出分组绑定的分类catelogId
         */
        // 当前分组只能绑定同分类下的属性，不能跨分类拿属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();
        /**
         * 当前分组只能关联别的分组没有引用的属性
         * 查出当前分类下全部其他相同分类的属性分组
         */
        // 查询该分类下所有属性分组（包含传入的 attrgroupId 本身）
        List<AttrGroupEntity> group = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        // Stream 流式遍历分组列表，只提取所有分组的主键 attr_group_id，打包成 Long 集合
        List<Long> collect = group.stream().map(item -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());

        // 查询这些分组已经绑定了哪些属性（中间表）
        /**
         * 操作中间表pms_attr_attrgroup_relation
         * 条件：attr_group_id IN (所有同分类分组id集合)
         * 拿到所有同分类下 属性分组已经绑定过的全部关联记录
         */
        List<AttrAttrgroupRelationEntity> groupId = relationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", collect));
        // 用 stream 提取所有被绑定属性的 attr_id，存入 attrIds 集合
        List<Long> attrIds = groupId.stream().map(item -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        // 从当前分类的所有属性中移除这些属性
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>().eq("catelog_id", catelogId) // 条件1：只查当前分类
                .eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()); // 条件2：只查基础属性（1）
        // 排除已经被同分类任意分组绑定过的属性
        if (attrIds != null && attrIds.size() > 0){
            // notIn = attr_id 不在已绑定属性 id 集合里，剩下的就是「未关联属性」
            wrapper.notIn("attr_id", attrIds);
        }

        // 搜索关键字 key 逻辑
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)){
            wrapper.and((w) -> {
                w.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        // 分页查询 MyBatis-Plus
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);

        // 封装分页工具返回
        PageUtils pageUtils = new PageUtils(page);

        return pageUtils;
    }

    /**
     * 在指定的所有属性集合里面挑出检索属性
     * @param attrIds
     * @return
     */
    @Override
    public List<Long> selectSearchAttrIds(List<Long> attrIds) {

        /**
         * SELECT attr_id FROM `pms_attr` WHERE attr_id IN(?) AND search_type=1
         */

        return baseMapper.selectSearchAttrIds(attrIds);
    }
}