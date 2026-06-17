package com.exception;

public class BizException extends RuntimeException{
    // 错误码
    private int code;

    // 只传提示信息，默认通用异常码
    public BizException(String msg){
        super(msg);
        this.code = BizCodeEnum.UNKNOW_EXCEPTION.getCode();
    }

    // 传枚举，统一使用规范错误码+提示
    public BizException(BizCodeEnum bizCodeEnum) {
        super(bizCodeEnum.getMsg());
        this.code = bizCodeEnum.getCode();
    }

    // 自定义错误码+自定义提示
    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
