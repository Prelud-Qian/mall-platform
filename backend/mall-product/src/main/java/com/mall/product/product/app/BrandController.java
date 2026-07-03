package com.mall.product.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.valid.AddGroup;
import com.valid.UpdateGroup;
import com.valid.UpdateStatusGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.mall.product.product.entity.BrandEntity;
import com.mall.product.product.service.BrandService;
import com.mall.common.utils.PageUtils;
import com.mall.common.utils.R;


/**
 * 品牌
 *
 * @author preludqian
 * @email 2551932043@qq.com
 * @date 2026-05-19 21:42:34
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:brand:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    //@RequiresPermissions("product:brand:info")
    public R info(@PathVariable("brandId") Long brandId){
		BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }

    @GetMapping("/infos")
    public R info(@RequestParam("brandIds") List<Long> brandIds){
        List<BrandEntity> brand = brandService.getBrandsByIds(brandIds);

        return R.ok().put("brand", brand);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    // @Valid 告诉 SpringMVC：帮我校验这个 brand 对象
    public R save(@Validated({AddGroup.class}) @RequestBody BrandEntity brand/*, BindingResult result*/){

        brandService.save(brand);

        /**
         *   SpringMVC 自动校验
         *       自动检查 BrandEntity 里每个字段的注解：
         *       name 是不是空？
         *       logo 是不是合法 URL？
         *       firstLetter 符不符合正则？
         *       sort 是不是 ≥0？
         *
         *   校验不通过 → 自动抛出异常
         *      抛出：MethodArgumentNotValidException
         */

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:brand:update")
    public R update(@Validated(UpdateGroup.class) @RequestBody BrandEntity brand){
		brandService.updateDetail(brand);

        return R.ok();
    }

    /**
     * 修改状态
     */
    @RequestMapping("/update/status")
    //@RequiresPermissions("product:brand:update")
    public R updateStatus(@Validated(UpdateStatusGroup.class) @RequestBody BrandEntity brand){
        brandService.updateById(brand);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:brand:delete")
    public R delete(@RequestBody Long[] brandIds){
		brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
