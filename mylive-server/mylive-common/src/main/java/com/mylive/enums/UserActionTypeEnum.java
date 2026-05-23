package com.mylive.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum UserActionTypeEnum {

    COMMENT_LIKE(0, "like_count", "评论喜欢点赞"),
    COMMENT_DISLIKE(1, "dislike_count", "评论讨厌"),
    VIDEO_LIKE(2, "like_count", "视频点赞"),
    VIDEO_SAVE(3, "save_count", "视频收藏"),
    VIDEO_COIN(4, "coin_count", "视频投币"),
    VIDEO_COMMENT(5, "comment_count", "视频评论数"),
    VIDEO_DANMAKU(6, "danmaku_count", "弹幕评论数"),
    VIDEO_PLAY(7, "play_count", "视频播放数");


    private final Integer type;
    private final String field;
    private final String desc;

    private static final Map<Integer, UserActionTypeEnum> TYPE_MAP;

    static {
        Map<Integer, UserActionTypeEnum> map = new HashMap<>();
        for (UserActionTypeEnum e : UserActionTypeEnum.values()) {
            map.put(e.type, e);
        }
        TYPE_MAP = Collections.unmodifiableMap(map);
    }

    public static UserActionTypeEnum getByType(Integer type) {
        return type == null ? null : TYPE_MAP.get(type);
    }
}
