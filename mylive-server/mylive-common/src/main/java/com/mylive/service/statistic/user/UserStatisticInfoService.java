package com.mylive.service.statistic.user;

import com.mylive.enums.StatisticTypeEnum;
import com.mylive.infra.jpa.entity.po.StatisticInfo;

import java.util.List;
import java.util.Map;

public interface UserStatisticInfoService {
    Map<String, Object> getRealTimeStatisticInfo(Long userId);

    List<StatisticInfo> getWeekStatisticInfo(Integer dataType, Long userId);

    void flushStatisticByType(String yesterday, StatisticTypeEnum type);
}
