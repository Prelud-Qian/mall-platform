package com.example.mall_search.controller;

import com.example.mall_search.service.MallSearchService;
import com.example.mall_search.vo.SearchParam;
import com.example.mall_search.vo.SearchResult;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    /**
     * 自动将页面提交过来的所有请求查询参数封装成指定的对象
     * @param param
     * @return
     */
    @GetMapping("/list.html")
    // Model 是 SpringMVC 提供的数据载体对象，专门用于后端向 Thymeleaf/JSP 页面传递数据
    public String listPage(SearchParam param, Model model, HttpServletRequest request){
        String queryString = request.getQueryString();
        param.set_queryString(queryString);
        // 根据传递来的页面查询参数，去es中检索商品
        SearchResult result = mallSearchService.search(param);
        // addAttribute(String 页面取值key, Object 后端数据)
        model.addAttribute("result", result);

        return "list";
    }
}
