package com.java3y.austin.web.exception;

import com.java3y.austin.common.enums.RespStatusEnum;
import com.java3y.austin.common.vo.BasicResultVO;
import com.java3y.austin.logistics.engine.FreightNoRateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 拦截异常统一返回
 * <p>
 * 修复：原实现把完整异常堆栈拼进 msg 返回给前端（信息泄露 + 不专业）。
 * 现改为：堆栈只记录到服务端日志，客户端仅返回简洁错误信息。
 *
 * @author kl
 * @version 1.0.0
 */
@ControllerAdvice(basePackages = "com.java3y.austin.web.controller")
@ResponseBody
public class ExceptionHandlerAdvice {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandlerAdvice.class);

    @ExceptionHandler({CommonException.class})
    @ResponseStatus(HttpStatus.OK)
    public BasicResultVO<Object> commonResponse(CommonException ce) {
        log.error("业务异常", ce);
        return BasicResultVO.fail(ce.getRespStatusEnum() == null ? RespStatusEnum.ERROR_500 : ce.getRespStatusEnum(),
                ce.getMessage() == null ? "业务处理失败" : ce.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.OK)
    public BasicResultVO<String> typeMismatch(MethodArgumentTypeMismatchException e) {
        log.error("参数类型错误", e);
        return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "参数类型错误：" + e.getName());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.OK)
    public BasicResultVO<String> notReadable(HttpMessageNotReadableException e) {
        log.error("请求体解析失败", e);
        return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "请求参数格式错误");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.OK)
    public BasicResultVO<String> missingParam(MissingServletRequestParameterException e) {
        log.error("缺少必填参数", e);
        return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, "缺少必填参数：" + e.getParameterName());
    }

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.OK)
    public BasicResultVO<String> exceptionResponse(Exception e) {
        // 堆栈只保留在服务端日志，不返回给客户端
        log.error("系统异常", e);
        return BasicResultVO.fail(RespStatusEnum.ERROR_500, "系统繁忙，请稍后重试");
    }

    @ExceptionHandler({FreightNoRateException.class})
    @ResponseStatus(HttpStatus.OK)
    public BasicResultVO<String> noRate(FreightNoRateException e) {
        log.warn("运费价卡未命中: {}", e.getMessage());
        return BasicResultVO.fail(RespStatusEnum.CLIENT_BAD_PARAMETERS, e.getMessage());
    }
}