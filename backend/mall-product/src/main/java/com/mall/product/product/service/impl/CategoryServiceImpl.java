package com.mall.product.product.service.impl;

import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import com.mall.product.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.product.dao.CategoryDao;
import com.mall.product.product.entity.CategoryEntity;
import com.mall.product.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 1. 查询所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        // 组装成父子的树形结构
        // 找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == 0;
        }).map((categoryEntity) -> {
            categoryEntity.setChildren(getChildren(categoryEntity, entities));
            return categoryEntity;
        }).sorted((category1, category2) -> {
            return category1.getSort() - category2.getSort();
        }).collect(Collectors.toList());

        return level1Menus;
    }

    // parentCategory：当前要找子分类的「父节点」     allCategory：所有分类的原始列表（数据库查出来的所有数据
    private List<CategoryEntity> getChildren(CategoryEntity parentCategory, List<CategoryEntity> allCategory){
        List<CategoryEntity> children = allCategory.stream().filter(categoryEntity -> { // 遍历 allCategory
            // return true：放行当前分类，进入后续流程；return false：过滤丢弃
            return categoryEntity.getParentCid() == parentCategory.getCatId();
        }).map(categoryEntity -> { // categoryEntity = 上一步 filter 过滤后剩下的每一个对象   map 里的变量 = filter 过滤后的每一个元素
            categoryEntity.setChildren(getChildren(categoryEntity, allCategory));
            return categoryEntity; // 给下一步传递加工好的对象
        }).sorted((category1, category2) -> {
            Integer sort1 = category1.getSort() == null ? 0 : category1.getSort();
            Integer sort2 = category2.getSort() == null ? 0 : category2.getSort();
            return Integer.compare(sort1, sort2);
        }).collect(Collectors.toList());

        return children;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        // TODO 检查当前删除的菜单，是否被别的地方引用

        // 逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);

        return parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths){
        // 1.收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0){
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }
}