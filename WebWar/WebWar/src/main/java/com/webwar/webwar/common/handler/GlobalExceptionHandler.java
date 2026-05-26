package com.webwar.webwar.common.handler;

import com.webwar.webwar.common.exception.BusinessException;
import com.webwar.webwar.common.result.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    // 处理所有未捕获的异常
    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e) {
        e.printStackTrace();
        return R.fail(500, "服务器内部错误：" + e.getMessage());
    }
}