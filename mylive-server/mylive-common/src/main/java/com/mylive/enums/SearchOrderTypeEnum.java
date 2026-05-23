package com.mylive.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SearchOrderTypeEnum {
    VIDEO_PLAY(0, "playCount", "视频播放数"),
    VIDEO_TIME(1, "createdAt", "视频时间"),
    VIDEO_DANMAKU(2, "danmakuCount", "弹幕数"),
    VIDEO_SAVE(3, "saveCount", "视频收藏");

    private final Integer type;
    private final String field;
    private final String desc;

    public static SearchOrderTypeEnum getByType(Integer type) {
        for (SearchOrderTypeEnum item : SearchOrderTypeEnum.values()) {
            if (item.getType().equals(type)) {
                return item;
            }
        }
        return null;
    }
}
