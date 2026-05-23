package com.mylive.web.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.constants.Constants;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.po.UserMessage;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.UserMessageCountVo;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.mylive.service.user.UserMessageService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class UserMessageController {

    private final UserMessageService userMessageService;
    private final RedisComponent redisComponent;

    @PostMapping("/getNoReadCount")
    public ResponseVo<Integer> getNoReadCount(@CookieValue(name = Constants.TOKEN_WEB, required = false) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        if (tokenInfo == null) {
            return ResponseVo.ok(0);
        }
        return ResponseVo.ok(userMessageService.getNoReadCount(tokenInfo.getUserId()));
    }

    @PostMapping("/getNoReadCountGroup")
    @RequiresLogin
    public ResponseVo<List<UserMessageCountVo>> getNoReadCountGroup(@CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(userMessageService.getMessageTypeWithNoReadCount(tokenInfo.getUserId()));
    }

    @PostMapping("/readAll")
    @RequiresLogin
    public ResponseVo<Void> readAll(String messageTypes,
                                    @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        userMessageService.readAllMessage(messageTypes, tokenInfo.getUserId());
        return ResponseVo.ok();
    }

    @PostMapping("/loadMessage")
    @RequiresLogin
    public ResponseVo<PaginationResultVo<UserMessage>> loadMessage(@NotBlank String messageTypes,
                                                                   Integer pageNo,
                                                                   @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(userMessageService.getMessagePage(messageTypes, pageNo, tokenInfo.getUserId()));
    }

    @PostMapping("/delMessage")
    @RequiresLogin
    public ResponseVo<Void> delMessage(@NotNull Integer messageId,
                                       @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        userMessageService.deleteMessage(messageId, tokenInfo.getUserId());
        return ResponseVo.ok();
    }
}
