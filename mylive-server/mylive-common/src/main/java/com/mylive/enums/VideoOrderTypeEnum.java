package com.mylive.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum VideoOrderTypeEnum {
    CREATED_AT(0, "createdAt", "最新发布"),
    PLAY_COUNT(1, "playCount", "最多播放"),
    SAVE_COUNT(2, "saveCount", "最多收藏");

    private final Integer type;
    private final String field;
    private final String desc;

    private static final Map<Integer, VideoOrderTypeEnum> TYPE_MAP;

    static {
        Map<Integer, VideoOrderTypeEnum> map = new HashMap<>();
        for (VideoOrderTypeEnum e : VideoOrderTypeEnum.values()) {
            map.put(e.type, e);
        }
        TYPE_MAP = Collections.unmodifiableMap(map);
    }

    public static VideoOrderTypeEnum getOrDefaultByType(Integer type) {
        return TYPE_MAP.getOrDefault(type, VideoOrderTypeEnum.CREATED_AT);
    }
}
