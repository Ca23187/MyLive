package com.mylive.web.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.constants.Constants;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.po.UserAction;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.mylive.service.video.UserActionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/userAction")
@RequiredArgsConstructor
@RequiresLogin
public class UserActionController {

    private final RedisComponent redisComponent;
    private final UserActionService userActionService;

    @PostMapping("doAction")
    public ResponseVo<Void> doAction(@NotBlank String videoId,
                                     @NotNull Integer actionType,
                                     @Max(2) @Min(1) Integer actionCount,
                                     Long commentId,
                                     @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        UserAction userAction = new UserAction();
        userAction.setUserId(tokenInfo.getUserId());
        userAction.setVideoId(videoId);
        userAction.setActionType(actionType);
        userAction.setActionCount(actionCount == null ? 1 : actionCount);
        userAction.setCommentId(commentId == null ? 0 : commentId);
        userActionService.saveAction(userAction, tokenInfo);
        return ResponseVo.ok();
    }
}
