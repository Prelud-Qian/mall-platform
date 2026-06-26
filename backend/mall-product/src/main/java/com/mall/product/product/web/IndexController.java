package com.mall.product.product.web;

import com.mall.product.product.entity.CategoryEntity;
import com.mall.product.product.service.CategoryService;
import com.mall.product.product.vo.Catelog2Vo;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @GetMapping({"/", "/index.html"})
    // org.springframework.ui.Model 是 SpringMVC 提供的数据容器，专门用于后端向 Thymeleaf 页面传数据。
    public String indexPage(Model model){

        // TODO 查出所有的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();


        // 视图解析器进行拼串：
        // classpath:/templates/ + 返回值 + .html
        model.addAttribute("categorys", categoryEntities);
        return "index";
    }

    // index/catalog.json
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson(){
        Map<String, List<Catelog2Vo>> catalogJson = categoryService.getCatalogJson();

        return catalogJson;
    }
}
