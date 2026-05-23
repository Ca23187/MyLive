package com.mylive.web.task;

import com.mylive.enums.StatisticTypeEnum;
import com.mylive.service.statistic.user.UserStatisticInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Slf4j
@RequiredArgsConstructor
public class StatisticTask {
    private final UserStatisticInfoService service;

    @Scheduled(cron = "0 0 5 * * ?")
    public void flushStatisticInfo() {
        String yesterday = LocalDate.now().minusDays(1).toString();

        for (StatisticTypeEnum type : StatisticTypeEnum.values()) {
            try {
                service.flushStatisticByType(yesterday, type);
            } catch (Exception e) {
                log.error("Failed to flush statistic, type = {}", type.name(), e);
            }
        }
    }
}
