package com.mylive.admin.interceptor;

import com.mylive.annotation.RequiresLogin;
import com.mylive.constants.Constants;
import com.mylive.exception.BusinessException;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseCodeEnum;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

@Component
@RequiredArgsConstructor
public class AppInterceptor implements HandlerInterceptor {

    private final RedisComponent redisComponent;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean requiresLogin =
                handlerMethod.hasMethodAnnotation(RequiresLogin.class) ||
                        handlerMethod.getBeanType().isAnnotationPresent(RequiresLogin.class);

        if (!requiresLogin) {
            return true;
        }

        Cookie cookie = WebUtils.getCookie(request, Constants.TOKEN_ADMIN);
        if (cookie == null
                || redisComponent.getAdminToken(cookie.getValue()) == null) {
            throw new BusinessException(ResponseCodeEnum.NOT_LOGGED_IN);
        }
        return true;
    }
}
