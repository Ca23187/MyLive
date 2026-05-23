package com.mylive.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum VideoStatusEnum {
    TRANSCODING(0, "Transcoding"),
    TRANSCODE_FAILED(1, "Transcode Failed"),
    PENDING(2, "Pending"),
    PASSED(3, "Review Passed"),
    REJECTED(4, "Review Rejected"),;

    private final Integer status;
    private final String desc;

    private static final Map<Integer, VideoStatusEnum> STATUS_MAP;

    static {
        Map<Integer, VideoStatusEnum> map = new HashMap<>();
        for (VideoStatusEnum e : VideoStatusEnum.values()) {
            map.put(e.status, e);
        }
        STATUS_MAP = Collections.unmodifiableMap(map);
    }

    public static VideoStatusEnum getByStatus(Integer status) {
        return status == null ? null : STATUS_MAP.get(status);
    }
}