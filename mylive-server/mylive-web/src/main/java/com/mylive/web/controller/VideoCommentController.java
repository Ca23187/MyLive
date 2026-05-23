package com.mylive.web.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.constants.Constants;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.po.VideoComment;
import com.mylive.infra.jpa.entity.vo.FollowOrFanListVo;
import com.mylive.infra.jpa.entity.vo.VideoCommentVo;
import com.mylive.infra.jpa.entity.vo.VideoCommentVoItem;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.mylive.service.user.UserFollowService;
import com.mylive.service.video.VideoCommentService;
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
@RequestMapping("comment")
@RequiredArgsConstructor
public class VideoCommentController {
    private final RedisComponent redisComponent;
    private final VideoCommentService videoCommentService;
    private final UserFollowService userFollowService;

    @PostMapping("/loadComment")
    public ResponseVo<VideoCommentVo> loadComment(@NotBlank String videoId,
                                                  Integer pageNo,
                                                  Integer orderType,
                                                  @CookieValue(name = Constants.TOKEN_WEB, required = false) String token) {
        return ResponseVo.ok(videoCommentService.getComments(videoId, pageNo, orderType, token));
    }

    @PostMapping("/loadReply")
    public ResponseVo<VideoCommentVo> loadReply(
            @NotNull Long parentId,
            Integer pageNo,
            @CookieValue(name = Constants.TOKEN_WEB, required = false) String token
    ) {
        return ResponseVo.ok(videoCommentService.getReplyList(parentId, pageNo, token));
    }

    @PostMapping("/postComment")
    @RequiresLogin
    public ResponseVo<VideoCommentVoItem> postComment(@NotBlank String videoId,
                                                      @NotNull Long videoUserId,
                                                      Long replyCommentId,
                                                      @NotBlank @Size(max = 500) String content,
                                                      @Size(max = 50) String imgPath,
                                                      String mentionUserIds,
                                                      @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        VideoComment comment = new VideoComment();
        comment.setUserId(tokenInfo.getUserId());
        comment.setNickname(tokenInfo.getNickname());
        comment.setAvatar(tokenInfo.getAvatar());
        comment.setVideoId(videoId);
        comment.setContent(content);
        comment.setImgPath(imgPath);
        VideoCommentVoItem vo = videoCommentService.postComment(comment, replyCommentId, mentionUserIds, videoUserId);
        vo.setAvatar(tokenInfo.getAvatar());
        vo.setNickname(tokenInfo.getNickname());
        return ResponseVo.ok(vo);
    }

    @PostMapping("/userDelComment")
    @RequiresLogin
    public ResponseVo<Void> userDelComment(@NotNull Long commentId,
                                           @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        videoCommentService.deleteComment(commentId, tokenInfo.getUserId());
        return ResponseVo.ok();
    }

    @PostMapping("/topComment")
    @RequiresLogin
    public ResponseVo<Void> topComment(@NotNull Long commentId,
                                       @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        videoCommentService.topComment(commentId, tokenInfo.getUserId());
        return ResponseVo.ok();
    }

    @PostMapping("/cancelTopComment")
    @RequiresLogin
    public ResponseVo<Void> cancelTopComment(@NotNull Long commentId,
                                             @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        videoCommentService.cancelTopComment(commentId, tokenInfo.getUserId());
        return ResponseVo.ok();
    }

    @PostMapping("/searchMentionUser")
    @RequiresLogin
    public ResponseVo<List<FollowOrFanListVo>> searchMentionUser(String keyword,
                                                                 @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        return ResponseVo.ok(userFollowService.searchMentionUserList(keyword, tokenInfo.getUserId()));
    }
}
