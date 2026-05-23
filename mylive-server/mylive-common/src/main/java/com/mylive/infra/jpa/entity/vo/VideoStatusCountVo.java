package com.mylive.infra.jpa.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@AllArgsConstructor
@Getter
@Setter
public class VideoStatusCountVo implements Serializable {
    private Long reviewPassedCount;
    private Long reviewRejectedCount;
    private Long inProgress;
}