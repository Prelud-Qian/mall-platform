package com.mall.product;

import com.mall.product.product.dao.AttrGroupDao;
import com.mall.product.product.dao.SkuSaleAttrValueDao;
import com.mall.product.product.service.BrandService;
import com.mall.product.product.service.CategoryService;
import com.mall.product.product.vo.SkuItemSaleAttrVo;
import com.mall.product.product.vo.SkuItemVo;
import com.mall.product.product.vo.SpuItemAttrGroupVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;


@Slf4j
@SpringBootTest
class MallProductApplicationTests {

	@Autowired
	BrandService brandService;

	@Autowired
	CategoryService categoryService;

	@Autowired
	AttrGroupDao attrGroupDao;

	@Autowired
	SkuSaleAttrValueDao skuSaleAttrValueDao;

	@Test
	public void testFindPath(){
		Long[] catelogPath = categoryService.findCatelogPath(225L);
		log.info("完整路径：{}", Arrays.asList(catelogPath));
	}

	@Test
	public void test(){
		List<SpuItemAttrGroupVo> attrGroupWithAttrsBySpuId = attrGroupDao.getAttrGroupWithAttrsBySpuId(5L, 225L);
		System.out.println(attrGroupWithAttrsBySpuId);

		List<SkuItemSaleAttrVo> saleAttrsBySpuId = skuSaleAttrValueDao.getSaleAttrsBySpuId(5L);
		System.out.println(saleAttrsBySpuId);
	}

}
