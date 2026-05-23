package com.mylive.infra.jpa.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AdminCommentVo implements Serializable {
    private Long commentId;
    private String videoId;
    private String videoCover;
    private String videoTitle;
    private Long videoUserId;
    private Long userId;
    private String nickname;
    private String avatar;
    private String content;
    private LocalDateTime createdAt;
    private Long replyUserId;
    private String replyUserNickname;
}
