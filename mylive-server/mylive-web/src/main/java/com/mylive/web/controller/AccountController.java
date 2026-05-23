package com.mylive.web.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.constants.Constants;
import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.vo.UserCountInfoVo;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.mylive.service.user.UserInfoService;
import com.wf.captcha.SpecCaptcha;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
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

    private final UserInfoService userInfoService;
    private final RedisComponent redisComponent;

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

    @PostMapping("/sendEmailCode")
    public ResponseVo<Void> sendEmailCode(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank String checkCodeKey,
            @NotBlank String checkCode) {
        try {
            if (!checkCode.trim().equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
                throw new BusinessException("Captcha is incorrect");
            }
            userInfoService.sendEmailCode(email);
            return ResponseVo.ok();
        } finally {
            redisComponent.cleanCheckCode(checkCodeKey);
        }
    }

    @PostMapping("/register")
    public ResponseVo<Void> register(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 20) String nickname,
            @NotBlank @Pattern(regexp = Constants.REGEX_PASSWORD) String registerPassword,
            @NotBlank String emailCode) {
        userInfoService.register(email, nickname.trim(), registerPassword, emailCode.trim());
        return ResponseVo.ok();
    }

    @PostMapping("/login")
    public ResponseVo<TokenInfo> login(
            HttpServletRequest request,
            HttpServletResponse response,
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String checkCodeKey,
            @NotBlank String checkCode,
            @CookieValue(name = Constants.TOKEN_WEB, required = false) String oldToken) {
        try {
            if (!checkCode.trim().equalsIgnoreCase(redisComponent.getCheckCode(checkCodeKey))) {
                throw new BusinessException("Captcha is incorrect");
            }
            String ip = request.getRemoteAddr();
            TokenInfo tokenInfo = userInfoService.login(email, password.trim(), ip);
            String newToken = redisComponent.saveTokenInfo(tokenInfo);
            Cookie cookie = new Cookie(Constants.TOKEN_WEB, newToken);
            cookie.setMaxAge(Constants.COOKIE_TTL_SECONDS);
            cookie.setPath("/");
            cookie.setSecure(true);
            response.addCookie(cookie);
            redisComponent.cleanTokenInfo(oldToken);
            return ResponseVo.ok(tokenInfo);
        } finally {
            redisComponent.cleanCheckCode(checkCodeKey);
        }
    }

    @PostMapping("/autoLogin")
    public ResponseVo<TokenInfo> autoLogin(
            HttpServletResponse response,
            @CookieValue(name = Constants.TOKEN_WEB, required = false) String oldToken) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(oldToken);
        if (tokenInfo == null) {
            return ResponseVo.ok(null);
        }
        String tokenForCookie = oldToken;
        // 快过期时才轮换新token
        if (redisComponent.needUpdateToken(oldToken)) {
            String newToken = redisComponent.saveTokenInfo(tokenInfo);
            // 旧token不立刻删，保留一个很短的过渡期，减少并发抖动
            redisComponent.keepOldTokenForGracePeriod(oldToken);
            tokenForCookie = newToken;
        }
        // 否则继续续token
        Cookie cookie = new Cookie(Constants.TOKEN_WEB, tokenForCookie);
        cookie.setMaxAge(Constants.COOKIE_TTL_SECONDS);
        cookie.setPath("/");
        cookie.setSecure(true);
        response.addCookie(cookie);
        return ResponseVo.ok(tokenInfo);
    }

    @PostMapping("/logout")
    public ResponseVo<Void> logout(HttpServletResponse response,
                                   @CookieValue(name = Constants.TOKEN_WEB, required = false) String oldToken) {
        redisComponent.cleanTokenInfo(oldToken);
        Cookie cookie = new Cookie(Constants.TOKEN_WEB, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setSecure(true);
        response.addCookie(cookie);
        return ResponseVo.ok();
    }

    @PostMapping("/getUserCountInfo")
    @RequiresLogin
    public ResponseVo<UserCountInfoVo> getUserCountInfo(@CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(userInfoService.getUserCountInfo(tokenInfo.getUserId()));
    }
}