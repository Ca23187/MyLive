package com.mylive.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageTypeEnum {
    SYS(1, "系统消息"),
    VIDEO_DELETE(2, "删除视频"),
    VIDEO_LIKE(3, "点赞视频"),
    VIDEO_SAVE(4, "收藏视频"),
    VIDEO_COIN(5, "投币视频"),
    VIDEO_COMMENT(6, "评论视频"),
    COMMENT_REPLY(7, "回复评论"),
    COMMENT_LIKE(8, "点赞评论"),
    USER_FOLLOW(9, "关注"),
    COMMENT_MENTION(10, "@评论");
    private final Integer type;
    private final String desc;
}