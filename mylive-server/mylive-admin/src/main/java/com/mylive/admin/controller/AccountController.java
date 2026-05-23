package com.mylive.admin.controller;

import com.mylive.config.AppProperties;
import com.mylive.constants.Constants;
import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.wf.captcha.SpecCaptcha;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final RedisComponent redisComponent;
    private final AppProperties appProperties;
    private final PasswordEncoder passwordEncoder;

    /**
     * 验证码
     */
    @PostMapping("/checkCode")
    public ResponseVo<Map<String, String>> checkCode() {
        SpecCaptcha captcha = new SpecCaptcha(100, 42);
        String code = captcha.text();
        String checkCodeKey = redisComponent.saveCheckCode(code);
        String checkCodeBase64 = captcha.toBase64();
        Map<String, String> result = new HashMap<>();
        result.put("checkCode", checkCodeBase64);
        result.put("checkCodeKey", checkCodeKey);
        return ResponseVo.ok(result);
    }

    @PostMapping(value = "/login")
    public ResponseVo<TokenInfo> login(
            HttpServletResponse response,
            @NotBlank String account,
            @NotBlank String password,
            @NotBlank String checkCodeKey,
            @NotBlank String checkCode,
            @CookieValue(name = Constants.TOKEN_ADMIN, required = false) String oldToken) {
        try {
            if (!checkCode.trim().equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
                throw new BusinessException("Captcha is incorrect");
            }
            if (!account.trim().equals(appProperties.getAdminAccount())
                    || !passwordEncoder.matches(password.trim(), appProperties.getAdminPassword())) {
                throw new BusinessException("Incorrect username or password.");
            }
            String newToken = redisComponent.saveAdminToken(account);
            Cookie cookie = new Cookie(Constants.TOKEN_ADMIN, newToken);
            cookie.setMaxAge(-1);
            cookie.setPath("/");
            cookie.setSecure(true);
            response.addCookie(cookie);
            redisComponent.cleanAdminToken(oldToken);
            return ResponseVo.ok(null);
        } finally {
            redisComponent.cleanCheckCode(checkCodeKey);
        }
    }

    @PostMapping(value = "/logout")
    public ResponseVo<Void> logout(HttpServletResponse response,
                                   @CookieValue(name = Constants.TOKEN_ADMIN, required = false) String oldToken) {
        redisComponent.cleanTokenInfo(oldToken);
        Cookie cookie = new Cookie(Constants.TOKEN_ADMIN, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setSecure(true);
        response.addCookie(cookie);
        return ResponseVo.ok();
    }
}