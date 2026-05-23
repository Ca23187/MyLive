package com.mylive.service.statistic.admin;

import com.mylive.infra.jpa.entity.dto.statistic.AdminStatisticDto;

import java.util.List;
import java.util.Map;

public interface AdminStatisticService {
    Map<String, Object> getRealTimeStatisticInfo();

    List<AdminStatisticDto> getWeekStatisticInfo(Integer dataType);
}
