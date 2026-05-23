package com.mylive.exception;

import com.mylive.response.ResponseCodeEnum;
import com.mylive.response.ResponseVo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** 参数校验异常（@RequestBody + @Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseVo<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                      HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数校验失败");

        log.warn("请求体参数校验失败，URL={}, msg={}", request.getRequestURL(), msg);
        return ResponseVo.error(ResponseCodeEnum.BAD_REQUEST.getCode(), msg);
    }

    /** 参数校验异常（controller方法参数校验）*/
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseVo<?> handleHandlerMethodValidation(
            HandlerMethodValidationException e,
            HttpServletRequest request
    ) {
        String msg = e.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("参数校验失败");

        log.warn("方法参数校验错误，URL={}, 错误={}", request.getRequestURL(), msg);
        return ResponseVo.error(ResponseCodeEnum.BAD_REQUEST.getCode(), msg);
    }

    /** 参数绑定异常 */
    @ExceptionHandler({
            BindException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class
    })
    public ResponseVo<?> handleBindException(Exception e, HttpServletRequest request) {
        log.warn("参数绑定/缺失异常，URL={}, error={}", request.getRequestURL(), e.getMessage());
        return ResponseVo.error(ResponseCodeEnum.BAD_REQUEST.getCode(), "请求参数错误");
    }

    /** JSON 格式错、body 为空、字段类型错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseVo<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                      HttpServletRequest request) {
        log.warn("请求体解析失败，URL={}, error={}", request.getRequestURL(), rootMsg(e));
        return ResponseVo.error(ResponseCodeEnum.BAD_REQUEST.getCode(), "请求体格式错误");
    }

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseVo<?> handleBusinessException(BusinessException e,
                                                 HttpServletRequest request) {
        log.warn("业务异常，URL={}, 错误={}", request.getRequestURL(), e.getMessage());
        int code = (e.getCode() == null ? ResponseCodeEnum.BAD_REQUEST.getCode() : e.getCode());
        return ResponseVo.error(code, e.getMessage());
    }

    /** 主键冲突 */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseVo<?> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        log.error("数据库主键冲突，URL={}, 错误={}", request.getRequestURL(), e.getMessage());
        return ResponseVo.error(ResponseCodeEnum.ALREADY_EXISTS);
    }

    /** 404 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseVo<?> handleNotFound(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("地址不存在，URL={}, 错误={}", request.getRequestURL(), e.getMessage());
        return ResponseVo.error(ResponseCodeEnum.NOT_FOUND);
    }

    /** 其他所有异常 */
    @ExceptionHandler(Exception.class)
    public ResponseVo<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常，URL={}", request.getRequestURL(), e);
        return ResponseVo.error(ResponseCodeEnum.INTERNAL_ERROR);
    }

    private String rootMsg(Throwable e) {
        Throwable t = e;
        Throwable last = e;
        while (t != null) {
            last = t;
            t = t.getCause();
        }
        return String.valueOf(last.getMessage());
    }
}
