package com.mylive.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VideoFileUpdateTypeEnum {
    NO_UPDATE(0, "无更新"),
    UPDATED(1, "有更新");
    private final Integer status;
    private final String desc;
}