package com.mylive.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommentTopTypeEnum {
    NO_TOP(0, "未置顶"),
    TOP(1, "已置顶");
    private final Integer type;
    private final String desc;
}
