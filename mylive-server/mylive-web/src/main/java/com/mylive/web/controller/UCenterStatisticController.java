package com.mylive.web.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.constants.Constants;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.po.StatisticInfo;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.mylive.service.statistic.user.UserStatisticInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ucenter")
@RequiredArgsConstructor
public class UCenterStatisticController {

    private final UserStatisticInfoService service;
    private final RedisComponent redisComponent;

    @PostMapping("/getRealTimeStatisticInfo")
    @RequiresLogin
    public ResponseVo<Map<String, Object>> getRealTimeStatisticInfo(@CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(service.getRealTimeStatisticInfo(tokenInfo.getUserId()));
    }

    @PostMapping("/getWeekStatisticInfo")
    @RequiresLogin
    public ResponseVo<List<StatisticInfo>> getWeekStatisticInfo(Integer dataType, @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(service.getWeekStatisticInfo(dataType, tokenInfo.getUserId()));
    }
}
