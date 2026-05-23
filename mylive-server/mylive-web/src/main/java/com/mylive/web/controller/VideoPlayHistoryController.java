package com.mylive.web.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.constants.Constants;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.po.VideoPlayHistory;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.mylive.service.video.VideoPlayHistoryService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/history")
@Slf4j
@RequiredArgsConstructor
public class VideoPlayHistoryController {

    private final VideoPlayHistoryService service;
    private final RedisComponent redisComponent;

    @PostMapping("/loadHistory")
    @RequiresLogin
    public ResponseVo<PaginationResultVo<VideoPlayHistory>> loadHistory(
            Integer pageNo,
            @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(service.getHistoryPage(tokenInfo.getUserId(), pageNo));
    }

    @PostMapping("/cleanHistory")
    @RequiresLogin
    public ResponseVo<Void> cleanHistory(@CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        service.deleteByUserId(tokenInfo.getUserId());
        return ResponseVo.ok();
    }

    @PostMapping("/delHistory")
    public ResponseVo<Void> delHistory(@NotBlank String videoId, @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        service.deleteSingleHistory(tokenInfo.getUserId(), videoId);
        return ResponseVo.ok();
    }
}
