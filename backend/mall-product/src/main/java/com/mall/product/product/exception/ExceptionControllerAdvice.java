package com.mall.product.product.exception;

import com.exception.BizCodeEnum;
import com.mall.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 集中处理所有异常（统一异常处理）
 */
@Slf4j
//@ResponseBody
//@ControllerAdvice(basePackages = "com.mall.product.product.controller")
@RestControllerAdvice(basePackages = "com.mall.product.product.controller")
public class ExceptionControllerAdvice {

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handleValidException(MethodArgumentNotValidException e){
        /**
         * 异常对象 e 里面有一个东西：
         * BindingResult
         * 它里面装着：
         * 哪个字段错了 → field
         * 错误提示是什么 → defaultMessage
         */
        log.error("数据校验出现问题{}，异常类型：{}", e.getMessage(), e.getClass());
        BindingResult bindingResult = e.getBindingResult();

        /**
         * errorMap 里的 字段名 + 错误信息 到底从哪来的？
         * 字段名、错误信息，全部来自你实体类（BrandEntity）上写的校验注解！
         *
         * 字段名 = 实体类的字段名（name/logo/sort...）
         * 错误信息 = 你写在注解里的 message 值
         */
        Map<String, String> errorMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach((fieldError) -> {
            // fieldError.getField()拿到实体属性字段名   fieldError.getDefaultMessage()拿到注解里message提示文本
            errorMap.put(fieldError.getField(), fieldError.getDefaultMessage());
        });
        // 从枚举VALID_EXCEPTION取出：code=10001、msg = 参数格式校验失败
        return R.error(BizCodeEnum.VALID_EXCEPTION.getCode(), BizCodeEnum.VALID_EXCEPTION.getMsg()).put("data", errorMap);
    }

    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable throwable){

        log.error("错误：", throwable);
        return R.error(BizCodeEnum.UNKNOW_EXCEPTION.getCode(), BizCodeEnum.UNKNOW_EXCEPTION.getMsg());
    }
}
