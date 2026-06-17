package com.mall.ware.ware.service.impl;

import com.constant.WareConstant;
import com.mall.ware.ware.entity.PurchaseDetailEntity;
import com.mall.ware.ware.service.PurchaseDetailService;
import com.mall.ware.ware.service.WareSkuService;
import com.mall.ware.ware.vo.MergeVo;
import com.mall.ware.ware.vo.PurchaseDoneVo;
import com.mall.ware.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.Query;

import com.mall.ware.ware.dao.PurchaseDao;
import com.mall.ware.ware.entity.PurchaseEntity;
import com.mall.ware.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;

import com.exception.BizCodeEnum;
import com.exception.BizException;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService detailService;

    @Autowired
    WareSkuService wareSkuService;

    private Long id;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceivePurchase(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status", 0).or().eq("status", 1)
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        // 没有传入采购单 ID → 新建采购单，拿到采购单主键 id
        if (purchaseId == null){
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }

        // 确认采购单状态是新建 每一项采购需求的状态是新建 才可以合并
        // 查出采购单的状态
        PurchaseEntity purchaseEntity = this.getById(purchaseId);
        int purchaseStatus = purchaseEntity.getStatus();
        // 查出每一项采购需求的状态
        List<Long> items = mergeVo.getItems();
        List<Integer> purchaseDetailStatus = items.stream().map(item -> {
            Integer status = detailService.queryStatus(item);
            return status;
        }).collect(Collectors.toList());

        // 判断所有采购需求状态是不是都为新建
        Boolean detailStatusNew = true;
        for (Integer detailStatus : purchaseDetailStatus) {
            if (detailStatus != 0){
                detailStatusNew = false;
                break;
            }
        }

        if (purchaseStatus == 0 && detailStatusNew){
            // 根据选中的需求 id 列表，批量更新采购需求表，把它们关联到该采购单、状态改为「已分配」
            Long finalPurchaseId = purchaseId;
            List<PurchaseDetailEntity> collect = items.stream().map(i -> {
                PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
                detailEntity.setId(i);
                detailEntity.setPurchaseId(finalPurchaseId);
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
                return detailEntity;
            }).collect(Collectors.toList());

            detailService.updateBatchById(collect);

            // 修改采购单内容
            PurchaseEntity updatePurchase  = new PurchaseEntity();
            updatePurchase.setId(purchaseId);
            updatePurchase.setStatus(WareConstant.PurchaseStatusEnum.ASSIGNED.getCode());
            updatePurchase.setUpdateTime(new Date());
            this.updateById(updatePurchase );
        }else{
            /**
             * 新建采购单 this.save(newPurchase) 只是写入事务缓存，还没真正提交数据库；
             * 只要抛出异常，整个事务全部回滚，自动删掉刚新建的采购单，不用手动写 delete 删除；
             */
//            throw new RuntimeException("校验失败");
            // 抛出自定义业务异常，携带统一错误码+完整可读提示
            throw new BizException(BizCodeEnum.PURCHASE_MERGE_FAIL);
        }
    }

    @Override
    public void received(List<Long> ids) {
        // 确认当前采购单是新建或者已分配状态
        List<PurchaseEntity> collect = ids.stream().map(id -> {
            PurchaseEntity byId = this.getById(id);
            return byId;
        }).filter(item -> {
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() || item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).map(item -> {
            // 改变采购单的状态
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());

        this.updateBatchById(collect);

        // 改变采购项的状态
        collect.forEach(item -> {
            List<PurchaseDetailEntity> entities = detailService.listDetailByPurchaseId(item.getId());
            List<PurchaseDetailEntity> detailEntities = entities.stream().map(entity -> {
                entity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return entity;
            }).collect(Collectors.toList());
            detailService.updateBatchById(detailEntities);
        });
    }

    @Override
    public void done(PurchaseDoneVo doneVo) {
        Long id = doneVo.getId();

        // 改变采购项的状态
        Boolean flag = true;
        List<PurchaseItemDoneVo> items = doneVo.getItems();

        List<PurchaseDetailEntity> updates = new ArrayList<>();
        for (PurchaseItemDoneVo item : items) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.HASERROR.getCode()){
                flag = false;
                detailEntity.setStatus(item.getStatus());
            }else{
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                // 将成功采购的进行入库
                PurchaseDetailEntity entity = detailService.getById(item.getItemId());
                wareSkuService.addStock(entity.getSkuId(), entity.getWareId(), entity.getSkuNum());
            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }

        detailService.updateBatchById(updates);

        // 改变采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        purchaseEntity.setStatus(flag ? WareConstant.PurchaseStatusEnum.FINISH.getCode() : WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);


    }

}