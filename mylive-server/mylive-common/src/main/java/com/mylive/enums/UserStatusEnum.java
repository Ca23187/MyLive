package com.mylive.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    DISABLED(0, "禁用"),
    ACTIVE(1, "启用");

    private final Integer status;
    private final String desc;
}
