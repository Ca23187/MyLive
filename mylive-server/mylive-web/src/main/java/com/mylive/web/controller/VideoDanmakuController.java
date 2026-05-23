package com.mylive.web.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.constants.Constants;
import com.mylive.infra.jpa.entity.po.VideoDanmaku;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.mylive.service.video.VideoDanmakuService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/danmaku")
@RequiredArgsConstructor
public class VideoDanmakuController {

    private final VideoDanmakuService videoDanmakuService;
    private final RedisComponent redisComponent;

    @PostMapping("/loadDanmaku")
    public ResponseVo<List<VideoDanmaku>> loadDanmaku(@NotBlank String fileId, @NotBlank String videoId) {
        return ResponseVo.ok(videoDanmakuService.getDanmakuList(fileId, videoId));
    }

    @PostMapping("/postDanmaku")
    @RequiresLogin
    public ResponseVo<Void> postDanmaku(@NotBlank String videoId,
                                        @NotBlank String fileId,
                                        @NotBlank @Size(max = 200) String text,
                                        @NotNull Integer mode,
                                        @NotBlank String color,
                                        @NotNull Integer time,
                                        @NotNull Long videoUserId,
                                        @CookieValue(name = Constants.TOKEN_WEB) String token) {
        VideoDanmaku danmaku = new VideoDanmaku();
        danmaku.setVideoId(videoId);
        danmaku.setFileId(fileId);
        danmaku.setText(text);
        danmaku.setMode(mode);
        danmaku.setColor(color);
        danmaku.setTime(time);
        danmaku.setUserId(redisComponent.getTokenInfo(token).getUserId());
        danmaku.setVideoUserId(videoUserId);
        videoDanmakuService.saveVideoDanmaku(danmaku);
        return ResponseVo.ok();
    }
}
