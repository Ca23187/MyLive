package com.mylive.web.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.constants.Constants;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.po.UserInfo;
import com.mylive.infra.jpa.entity.vo.FollowOrFanListVo;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.UHomeVideoInfoVo;
import com.mylive.infra.jpa.entity.vo.UserInfoVo;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.mylive.service.user.UserFollowService;
import com.mylive.service.user.UserInfoService;
import com.mylive.service.video.VideoInfoService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/uhome")
@RequiredArgsConstructor
public class UHomeController {

    private final RedisComponent redisComponent;
    private final UserInfoService userInfoService;
    private final UserFollowService userFollowService;
    private final VideoInfoService videoInfoService;

    @PostMapping("/getUserInfo")
    public ResponseVo<UserInfoVo> getUserInfo(@NotNull Long userId,
                                              @CookieValue(name = Constants.TOKEN_WEB, required = false) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(userInfoService.getUserDetail(userId, null == tokenInfo ? null : tokenInfo.getUserId()));
    }

    @PostMapping("/updateUserInfo")
    @RequiresLogin
    public ResponseVo<Void> updateUserInfo(@NotBlank @Size(max = 20) String nickname,
                                           @NotBlank @Size(max = 100) String avatar,
                                           @NotNull Integer gender,
                                           String birthday,
                                           @Size(max = 150) String school,
                                           @Size(max = 80) String profile,
                                           @Size(max = 300) String noticeInfo,
                                           @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(tokenInfo.getUserId());
        userInfo.setNickname(nickname.trim());
        userInfo.setAvatar(avatar);
        userInfo.setGender(gender);
        userInfo.setBirthday(birthday == null ? "" : birthday.trim());
        userInfo.setSchool(school == null ? "" : school.trim());
        userInfo.setProfile(profile);
        userInfo.setNoticeInfo(noticeInfo);
        userInfoService.updateUserInfo(userInfo, tokenInfo, token);
        return ResponseVo.ok();
    }

    @PostMapping("/saveTheme")
    @RequiresLogin
    public ResponseVo<Void> saveTheme(@NotNull @Min(1) @Max(10) Integer theme,
                                      @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        userInfoService.updateTheme(theme, tokenInfo.getUserId());
        return ResponseVo.ok();
    }

    @PostMapping("/follow")
    @RequiresLogin
    public ResponseVo<Void> follow(@NotNull Long followUserId,
                                   @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        userFollowService.followUser(tokenInfo, followUserId);
        return ResponseVo.ok();
    }

    @PostMapping("/unFollow")
    @RequiresLogin
    public ResponseVo<Void> unFollow(@NotNull Long followUserId,
                                     @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        userFollowService.unFollow(tokenInfo.getUserId(), followUserId);
        return ResponseVo.ok();
    }

    @PostMapping("/loadFollowList")
    @RequiresLogin
    public ResponseVo<PaginationResultVo<FollowOrFanListVo>> loadFollowList(Integer pageNo,
                                                                            @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(userFollowService.findFollowPage(pageNo, tokenInfo.getUserId()));
    }

    @PostMapping("/loadFanList")
    @RequiresLogin
    public ResponseVo<PaginationResultVo<FollowOrFanListVo>> loadFanList(Integer pageNo,
                                                                         @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(userFollowService.findFanPage(pageNo, tokenInfo.getUserId()));
    }

    @PostMapping("/loadVideoList")
    public ResponseVo<PaginationResultVo<UHomeVideoInfoVo>> loadVideoList(@NotNull Long userId,
                                                                          Integer type,
                                                                          Integer pageNo,
                                                                          String videoTitle,
                                                                          Integer orderType) {
        return ResponseVo.ok(videoInfoService.getUHomeVideoPage(
                userId, type, pageNo, videoTitle, orderType));
    }

    @PostMapping("/loadUserSave")
    public ResponseVo<PaginationResultVo<UHomeVideoInfoVo>> loadUserSave(@NotNull Long userId, Integer pageNo) {
        return ResponseVo.ok(videoInfoService.getUHomeSavedVideoPage(userId, pageNo));
    }
}
