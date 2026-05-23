package com.mylive.web.controller;


import com.mylive.annotation.RequiresLogin;
import com.mylive.constants.Constants;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.po.UserVideoSeries;
import com.mylive.infra.jpa.entity.vo.UHomeVideoInfoVo;
import com.mylive.infra.jpa.entity.vo.UserVideoSeriesDetailVo;
import com.mylive.infra.jpa.entity.vo.VideoSeriesWithVideoVo;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.mylive.service.video.UserVideoSeriesService;
import com.mylive.service.video.VideoInfoService;
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
@RequestMapping("/uhome/series")
@RequiredArgsConstructor
public class UHomeVideoSeriesController {

    private final UserVideoSeriesService userVideoSeriesService;
    private final RedisComponent redisComponent;
    private final VideoInfoService videoInfoService;

    @PostMapping("/loadVideoSeries")
    public ResponseVo<List<UserVideoSeries>> loadVideoSeries(@NotNull Long userId) {
        return ResponseVo.ok(userVideoSeriesService.getAllSeries(userId));
    }

    @PostMapping("/saveVideoSeries")
    @RequiresLogin
    public ResponseVo<Void> saveVideoSeries(Long seriesId,
                                            @NotBlank @Size(max = 100) String seriesName,
                                            @Size(max = 200) String seriesDescription,
                                            String videoIds,
                                            @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        UserVideoSeries videoSeries = new UserVideoSeries();
        videoSeries.setUserId(tokenInfo.getUserId());
        videoSeries.setSeriesId(seriesId);
        videoSeries.setSeriesName(seriesName.trim());
        videoSeries.setSeriesDescription(seriesDescription);
        userVideoSeriesService.saveUserVideoSeries(videoSeries, videoIds);
        return ResponseVo.ok();
    }

    @PostMapping("/loadAllVideo")
    @RequiresLogin
    public ResponseVo<List<UHomeVideoInfoVo>> loadAllVideo(Long seriesId,
                                                           @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(videoInfoService.getUHomeVideoList(seriesId, tokenInfo.getUserId()));
    }

    @PostMapping("/getVideoSeriesDetail")
    public ResponseVo<UserVideoSeriesDetailVo> getVideoSeriesDetail(@NotNull Long seriesId) {
        return ResponseVo.ok(userVideoSeriesService.getVideoSeriesDetail(seriesId));
    }

    @PostMapping("/addSeriesVideo")
    @RequiresLogin
    public ResponseVo<Void> addSeriesVideo(@NotNull Long seriesId,
                                            @NotBlank String videoIds,
                                            @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        userVideoSeriesService.addSeriesVideo(tokenInfo.getUserId(), seriesId, videoIds);
        return ResponseVo.ok();
    }

    @PostMapping("/reorderSeriesVideo")
    @RequiresLogin
    public ResponseVo<Void> reorderSeriesVideo(@NotNull Long seriesId,
                                               @NotBlank String videoIds,
                                               @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        userVideoSeriesService.reorderSeriesVideo(tokenInfo.getUserId(), seriesId, videoIds);
        return ResponseVo.ok();
    }

    @PostMapping("/delSeriesVideo")
    @RequiresLogin
    public ResponseVo<Void> delSeriesVideo(@NotNull Long seriesId,
                                           @NotBlank String videoId,
                                           @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        userVideoSeriesService.delSeriesVideo(tokenInfo.getUserId(), seriesId, videoId);
        return ResponseVo.ok();
    }

    @PostMapping("/delVideoSeries")
    @RequiresLogin
    public ResponseVo<Void> delVideoSeries(@NotNull Long seriesId,
                                           @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        userVideoSeriesService.delVideoSeries(tokenInfo.getUserId(), seriesId);
        return ResponseVo.ok();
    }

    @PostMapping("/reorderVideoSeries")
    @RequiresLogin
    public ResponseVo<Void> reorderVideoSeries(@NotBlank String seriesIds,
                                               @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        userVideoSeriesService.reorderVideoSeries(tokenInfo.getUserId(), seriesIds);
        return ResponseVo.ok();
    }

    @PostMapping("/loadVideoSeriesWithVideo")
    public ResponseVo<List<VideoSeriesWithVideoVo>> loadVideoSeriesWithVideo(@NotNull Long userId) {
        return ResponseVo.ok(userVideoSeriesService.getVideoSeriesWithVideo(userId));
    }
}
