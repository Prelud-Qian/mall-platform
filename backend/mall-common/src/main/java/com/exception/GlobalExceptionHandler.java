package com.exception;

import com.mall.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice // 拦截所有@RestController接口
public class GlobalExceptionHandler {
    // 捕获我们自定义的业务异常（合并失败、参数错误等）
    @ExceptionHandler(BizException.class)
    public R handleBizException(BizException e) {
        log.error("业务异常：{}", e.getMessage(), e);
        return R.error(e.getCode(), e.getMessage());
    }

    // 兜底捕获所有未知系统异常
    @ExceptionHandler(Exception.class)
    public R handleOtherException(Exception e) {
        log.error("系统未知异常", e);
        return R.error(BizCodeEnum.UNKNOW_EXCEPTION.getCode(), BizCodeEnum.UNKNOW_EXCEPTION.getMsg());
    }
}
