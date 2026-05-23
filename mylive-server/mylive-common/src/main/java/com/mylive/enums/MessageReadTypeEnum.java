package com.mylive.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageReadTypeEnum {
    NO_READ(0, "未读"),
    READ(1, "已读");
    private final Integer type;
    private final String desc;
}