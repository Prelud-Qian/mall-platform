package com.mall.product.product.vo;

import com.mall.product.product.entity.SkuImagesEntity;
import com.mall.product.product.entity.SkuInfoEntity;
import com.mall.product.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
public class SkuItemVo {
    // sku基本信息获取    pms_sku_info
    SkuInfoEntity info;

    boolean hasStock = true;

    // sku的图片信息     pms_sku_images
    List<SkuImagesEntity> images;

    // 获取spu的销售属性组合
    List<SkuItemSaleAttrVo> saleAttr;

    // 获取spu的介绍
    SpuInfoDescEntity desc;

    // 获取spu的规格参数信息
    List<SpuItemAttrGroupVo> groupAttrs;

}
