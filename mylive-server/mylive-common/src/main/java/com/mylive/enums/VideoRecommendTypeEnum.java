package com.mylive.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VideoRecommendTypeEnum {
    NO_RECOMMEND(0, "未推荐"),
    RECOMMEND(1, "已推荐");

    private final Integer type;
    private final String desc;
}