package com.mylive.infra.jpa.entity.dto.statistic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StatisticTotalDto implements Serializable {

    private long playCount;

    private long likeCount;

    private long danmakuCount;

    private long commentCount;

    private long coinCount;

    private long saveCount;

    private long userCount;
}