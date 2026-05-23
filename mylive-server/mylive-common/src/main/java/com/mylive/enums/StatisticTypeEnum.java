package com.mylive.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StatisticTypeEnum {

    PLAY(0, "播放量", "play:"),
    FAN(1, "粉丝", "fan:"),
    LIKE(2, "点赞", "like:"),
    SAVE(3, "收藏", "save:"),
    COIN(4, "投币", "coin:"),
    COMMENT(5, "评论", "comment:"),
    DANMAKU(6, "弹幕", "danmaku:");

    private final Integer type;
    private final String desc;
    private final String redisPrefix;
}
