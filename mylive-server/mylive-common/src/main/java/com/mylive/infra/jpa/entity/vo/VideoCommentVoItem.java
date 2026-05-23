package com.mylive.infra.jpa.entity.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VideoCommentVoItem implements Serializable {
    private Long commentId;
    private Long parentCommentId;
    private Long userId;
    private String avatar;
    private String nickname;
    private String content;
    private String imgPath;
    private Long replyUserId;
    private String replyNickname;
    private LocalDateTime postedAt;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer topType;
    private Integer replyCount;

    @JsonIgnore
    private String mentionJson;

    private String videoId;
    private String videoTitle;
    private String videoCover;
    private Long videoUserId;

    private List<MentionUserVo> mentionUsers;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MentionUserVo implements Serializable {
        private Long userId;
        private String nickname;
    }
}
