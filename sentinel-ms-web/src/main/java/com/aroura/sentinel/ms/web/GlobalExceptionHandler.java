package com.aroura.sentinel.ms.web;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import com.aroura.sentinel.common.enums.RespStatusEnum;
import com.aroura.sentinel.common.vo.BasicResultVO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 全局异常处理：把异常收敛成统一的 {@link BasicResultVO}，避免 Spring 默认错误页
 * （一份 {timestamp,status,error,path} 的 HTML/JSON）漏给调用方。
 * <p>
 * 在此之前全项目没有一个 @ControllerAdvice：参数校验失败、类型不匹配、SQL 报错
 * 一律冒泡成默认错误页，前端拿不到 msg，也无法区分「参数错」与「服务挂了」。
 *
 * <h3>HTTP 状态码的选择</h3>
 * <b>客户端错误返回 HTTP 200 + 业务码</b>：前端响应拦截器（frontend-vue/src/api/request.js）
 * 只在 HTTP 200 时读取 body 里的 status/msg 并展示具体原因；非 2xx 只会显示 axios 的
 * 通用文案（"Request failed with status code 400"）。所以把 A0001 放在 200 里是**刻意**的，
 * 与本项目既有约定（登录失败也是 200 + A0001）一致，改成正统的 4xx 会让提示变差。
 * <p>
 * <b>服务端异常返回 HTTP 500</b>：这类问题需要被监控与告警看见，不应该伪装成 200。
 * 响应体同样是 BasicResultVO，但 msg 固定为通用文案 —— 堆栈只进日志，不回给调用方。
 *
 * @author sentinel-ms
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * {@code @RequestParam} / {@code @PathVariable} 上的约束校验失败
     * （需要控制器标 {@code @Validated}，否则注解不生效）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public BasicResultVO<Void> handleConstraint(ConstraintViolationException e) {
        String detail = "参数校验失败";
        for (ConstraintViolation<?> v : e.getConstraintViolations()) {
            String path = String.valueOf(v.getPropertyPath());
            int dot = path.lastIndexOf('.');
            detail = (dot >= 0 ? path.substring(dot + 1) : path) + " " + v.getMessage();
            break;
        }
        log.warn("[GlobalException] 参数约束校验失败: {}", detail);
        return new BasicResultVO<>(RespStatusEnum.CLIENT_BAD_PARAMETERS, detail, null);
    }

    /** 请求体/参数校验失败（@Valid @RequestBody、@Validated 方法参数）。 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public BasicResultVO<Void> handleBind(Exception e) {
        String detail = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException) {
            FieldError fe = ((MethodArgumentNotValidException) e).getBindingResult().getFieldError();
            if (fe != null) {
                detail = fe.getField() + " " + fe.getDefaultMessage();
            }
        } else if (e instanceof BindException) {
            FieldError fe = ((BindException) e).getBindingResult().getFieldError();
            if (fe != null) {
                detail = fe.getField() + " " + fe.getDefaultMessage();
            }
        }
        log.warn("[GlobalException] 参数校验失败: {}", detail);
        return new BasicResultVO<>(RespStatusEnum.CLIENT_BAD_PARAMETERS, detail, null);
    }

    /**
     * 缺参数 / 类型不匹配 / 请求体读不了 —— 都是客户端问题，
     * 不该像现在这样冒泡成 500（例如 page=0 会让 SQL 报错）。
     */
    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public BasicResultVO<Void> handleBadRequest(Exception e) {
        // 只取参数名，不回传异常原文 —— 原文里是 "Failed to convert value of type
        // 'java.lang.String' to required type 'int'; nested exception is ..." 这类内部细节，
        // 对调用方没有意义，也不该暴露实现。
        String detail;
        if (e instanceof MissingServletRequestParameterException) {
            detail = "缺少必填参数：" + ((MissingServletRequestParameterException) e).getParameterName();
        } else if (e instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException m = (MethodArgumentTypeMismatchException) e;
            detail = "参数 " + m.getName() + " 类型不正确";
        } else {
            detail = "请求参数不合法";
        }
        log.warn("[GlobalException] 请求参数不合法: {} ({})", detail, e.getMessage());
        return new BasicResultVO<>(RespStatusEnum.CLIENT_BAD_PARAMETERS, detail, null);
    }

    /** 兜底：未预期的异常。堆栈进日志，调用方只拿到通用文案。 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public BasicResultVO<Void> handleUnexpected(Exception e, HttpServletRequest request) {
        log.error("[GlobalException] 未处理异常 path={} method={}",
                request == null ? null : request.getRequestURI(),
                request == null ? null : request.getMethod(), e);
        return new BasicResultVO<>(RespStatusEnum.SERVICE_ERROR, "服务执行异常，请稍后重试", null);
    }
}
