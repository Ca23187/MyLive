package com.mylive.web.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.constants.Constants;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.vo.BasicVideoInfoVo;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.UCenterDanmakuVo;
import com.mylive.infra.jpa.entity.vo.VideoCommentVoItem;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.mylive.service.video.VideoCommentService;
import com.mylive.service.video.VideoDanmakuService;
import com.mylive.service.video.VideoInfoService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ucenter")
@RequiredArgsConstructor
@RequiresLogin
public class UCenterInteractionController {

    private final RedisComponent redisComponent;
    private final VideoInfoService videoInfoService;
    private final VideoCommentService videoCommentService;
    private final VideoDanmakuService videoDanmakuService;

    @PostMapping("/loadAllVideo")
    public ResponseVo<List<BasicVideoInfoVo>> loadAllVideo(
            @CookieValue(name = Constants.TOKEN_WEB) String token
    ) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(videoInfoService.getBasicVideoInfoVo(tokenInfo.getUserId()));
    }

    @PostMapping("/loadComment")
    public ResponseVo<PaginationResultVo<VideoCommentVoItem>> loadComment(
            Integer pageNo, String videoId, @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(videoCommentService.getUCenterCommentPage(pageNo, videoId, tokenInfo.getUserId()));
    }

    @PostMapping("/delComment")
    public ResponseVo<Void> delComment(@NotNull Long commentId,
                                       @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        videoCommentService.deleteComment(commentId, tokenInfo.getUserId());
        return ResponseVo.ok();
    }

    @PostMapping("/loadDanmaku")
    public ResponseVo<PaginationResultVo<UCenterDanmakuVo>> loadDanmaku(
            Integer pageNo, String videoId, @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(videoDanmakuService.getUCenterDanmakuPage(pageNo, videoId, tokenInfo.getUserId()));
    }

    @PostMapping("/delDanmaku")
    public ResponseVo<Void> delDanmaku(@NotNull Long danmakuId,
                                       @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        videoDanmakuService.deleteDanmaku(tokenInfo.getUserId(), danmakuId);
        return ResponseVo.ok();
    }
}
