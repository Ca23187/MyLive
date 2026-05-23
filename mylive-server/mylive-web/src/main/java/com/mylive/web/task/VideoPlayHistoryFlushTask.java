package com.mylive.web.task;

import com.mylive.constants.Constants;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.service.video.VideoPlayHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoPlayHistoryFlushTask {

    private final RedisComponent redisComponent;
    private final VideoPlayHistoryService videoPlayHistoryService;

    @Scheduled(fixedDelay = Constants.VIDEO_HISTORY_FLUSH_MILLI)
    public void flushPlayHistory() {
        redisComponent.flushVideoPlayHistory(
                videoPlayHistoryService::saveHistory
        );
    }
}