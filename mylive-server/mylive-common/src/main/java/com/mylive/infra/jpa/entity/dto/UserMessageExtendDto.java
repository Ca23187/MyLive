package com.mylive.infra.jpa.entity.dto;

import lombok.Data;

@Data
public class UserMessageExtendDto {
    private String messageContent;      // 评论内容 / 系统消息内容
    private String messageContentReply; // 被回复的评论内容
    private Integer reviewStatus;       // 审核状态
    private Integer coinCount;          // 投币数量
    private String sendUserNickname;
    private String sendUserAvatar;
    private String videoCover;
    private String videoTitle;
    private String imgPath;
}