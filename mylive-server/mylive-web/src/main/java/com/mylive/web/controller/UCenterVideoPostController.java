package com.mylive.web.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.constants.Constants;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.dto.request.VideoInfoPostDto;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.PostedVideoEditVo;
import com.mylive.infra.jpa.entity.vo.VideoInfoPostVo;
import com.mylive.infra.jpa.entity.vo.VideoStatusCountVo;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.mylive.service.video.VideoInfoService;
import com.mylive.service.video.post.UserVideoInfoPostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ucenter")
@RequiredArgsConstructor
@RequiresLogin
public class UCenterVideoPostController {
    private final RedisComponent redisComponent;

    private final UserVideoInfoPostService userVideoInfoPostService;
    private final VideoInfoService videoInfoService;

    @PostMapping("/postVideo")
    public ResponseVo<Void> postVideo(@RequestBody @Valid VideoInfoPostDto dto,
                                      @CookieValue(name = Constants.TOKEN_WEB) String token) {

        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        userVideoInfoPostService.saveVideoInfoPost(tokenInfo.getUserId(), dto);
        return ResponseVo.ok();
    }

    @PostMapping("/loadVideoPostList")
    public ResponseVo<PaginationResultVo<VideoInfoPostVo>> loadVideoPostList(
            Integer status,
            Integer pageNo,
            String videoTitleFuzzy,
            @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(userVideoInfoPostService.findPostPage4User(
                status, pageNo, videoTitleFuzzy, tokenInfo.getUserId()));
    }

    @PostMapping("/getVideoCountInfo")
    public ResponseVo<VideoStatusCountVo> getVideoCountInfo(@CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(userVideoInfoPostService.getStatusCount(tokenInfo.getUserId()));
    }

    @PostMapping("/getVideoByVideoId")
    public ResponseVo<PostedVideoEditVo> getVideoByVideoId(@NotEmpty String videoId,
                                                           @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(userVideoInfoPostService.getEditVideoPost(tokenInfo.getUserId(), videoId));
    }

    @RequestMapping("/saveVideoInteraction")
    public ResponseVo<Void> saveVideoInteraction(@NotBlank String videoId,
                                                 @NotNull Integer allowDanmaku,
                                                 @NotNull Integer allowComment,
                                                 @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        videoInfoService.changeInteraction(videoId, tokenInfo.getUserId(), allowDanmaku, allowComment);
        return ResponseVo.ok();
    }

    @PostMapping("/deleteVideo")
    public ResponseVo<Void> deleteVideo(@NotBlank String videoId,
                                        @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        videoInfoService.deleteVideo(videoId, tokenInfo.getUserId(), null);
        return ResponseVo.ok();
    }
}
