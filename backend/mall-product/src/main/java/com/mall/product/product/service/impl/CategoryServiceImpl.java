package com.mall.product.product.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.mall.product.product.service.CategoryBrandRelationService;
import com.mall.product.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.product.product.dao.CategoryDao;
import com.mall.product.product.entity.CategoryEntity;
import com.mall.product.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.cache.annotation.CacheRemoveAll;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

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
    private List<CategoryEntity> getChildren(CategoryEntity parentCategory, List<CategoryEntity> allCategory) {
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
     * @CacheEvict:失效模式
     * @param category
     */
    @CacheEvict(value = "category", key = "'getLevel1Categorys'")
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

//        // 删缓存模式
//        redisTemplate.delete("catalogJSON");

        // TODO 延迟双删
    }

    // 每一个需要缓存的数据 都要指定放到哪个名字的缓存中 【缓存的分区（按照业务类型分）】
    @Cacheable(value = {"category"}, key = "#root.method.name") // 代表当前方法的结果需要缓存，如果缓存中有，方法不用调用；如果缓存中没有，会调用方法，最后将方法的结果放入缓存
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));

        return categoryEntities;
    }

    // TODO 产生堆外内存溢出：OutOfDirectMemoryError

    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        // 给缓存中放json字符串，拿出的json字符串，还要逆转为能用的对象类型 【序列化与反序列化】

        // 缓存中存的数据是json字符串
        // JSON 跨语言，跨平台兼容
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            // 缓存中没有，查询数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedissonLock();
            // 查到的数据再放入缓存，将对象转为json放在缓存中
            String s = JSON.toJSONString(catalogJsonFromDb);
            redisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
            return catalogJsonFromDb;
        }

        // 转为指定的对象
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
        return result;
    }

    /**
     * 缓存里面的数据如何和数据库内的数据保持一致
     * 缓存数据一致性
     * 1.双写模式   最终一致性
     * 2.失效模式
     * @return
     */

    // TODO 多个线程   只要有一个线程修改了新的缓存数据  那其他线程就应该不继续等待着去 执行修改缓存数据的操作了
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {

        RLock lock = redissonClient.getLock("CatalogJson-lock");
        lock.lock();

        Map<String, List<Catelog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        } finally {
            lock.unlock();
        }

        return dataFromDb;

    }


    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {

        // 占分布式锁 去redis占坑
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功");
            // 加锁成功     执行业务
            // 设置过期时间，必须和加锁是同步的，原子的
//            redisTemplate.expire("lock", 30, TimeUnit.SECONDS);

            Map<String, List<Catelog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally {
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                // 删除锁
                Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                        Arrays.asList("lock"), uuid);
            }

            // 获取值对比+对比成功删除=原子操作    lua脚本解锁
//            String lockValue = redisTemplate.opsForValue().get("lock");
//            if (uuid.equals(lockValue)){
//                redisTemplate.delete("lock"); // 删除锁
//            }

            return dataFromDb;
        } else {
            System.out.println("获取分布式锁失败。。。等待重试");
            // 加锁失败     自旋重试
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return getCatalogJsonFromDbWithRedisLock();
        }

    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        // 得到锁以后，应该先去缓存中确定一次，如果缓存中没有数据才需要继续查询
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJSON)) {
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }

        /**
         *  将数据库的多次查询变为一次
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        //  查出所有一级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        // 封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 每一个的一级分类，查到这个一级分类的所有二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            // 封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(level2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, level2.getCatId().toString(), level2.getName());
                    // 找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catalog = getParent_cid(selectList, level2.getCatId());
                    if (level3Catalog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catalog.stream().map(level3 -> {
                            // 封装成指定格式
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(level2.getCatId().toString(), level3.getCatId().toString(), level3.getName());

                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }

                    return catelog2Vo;
                }).collect(Collectors.toList());
            }

            return catelog2Vos;
        }));

        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
        return parent_cid;
    }

    // 从数据库查询并封装分类数据
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock() {

        synchronized (this) {
            return getDataFromDb();
        }

    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        return collect;
//        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        // 1.收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }
}