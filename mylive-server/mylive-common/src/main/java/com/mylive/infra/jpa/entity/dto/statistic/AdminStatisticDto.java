package com.mylive.infra.jpa.entity.dto.statistic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AdminStatisticDto implements Serializable {
    private Integer dataType;
    private Long statisticCount;
    private String statisticDate;

    public AdminStatisticDto(Long statisticCount, String statisticDate) {
        this.statisticCount = statisticCount;
        this.statisticDate = statisticDate;
    }

    public AdminStatisticDto(Integer dataType, int statisticCount, String statisticDate) {
        this.dataType = dataType;
        this.statisticCount = (long) statisticCount;
        this.statisticDate = statisticDate;
    }
}