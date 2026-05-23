package com.mylive.web.task;

import com.mylive.constants.Constants;
import com.mylive.enums.SearchOrderTypeEnum;
import com.mylive.infra.elasticsearch.ElasticSearchComponent;
import com.mylive.infra.jpa.repository.VideoInfoRepository;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.infra.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class VideoPlayCountFlushTask {

    private final RedisComponent redisComponent;
    private final VideoInfoRepository repository;
    private final ElasticSearchComponent elasticSearchComponent;
    private final RedisUtils redisUtils;

    @Scheduled(fixedDelay = Constants.VIDEO_PLAY_COUNT_FLUSH_MILLI)
    @Transactional(rollbackFor = Exception.class)
    public void flushPlayCount() {
        redisComponent.flushVideoPlayCount(
                (videoId, count) -> {
                    repository.incrReadCountAndUpdateTime(videoId, count);

                    // DB 成功后立刻扣减 Redis
                    redisUtils.hIncrement(
                            Constants.REDIS_KEY_VIDEO_PLAY_COUNT,
                            videoId,
                            -count
                    );

                    // ES 失败不影响 flush
                    try {
                        elasticSearchComponent.updateDocCount(
                                videoId,
                                SearchOrderTypeEnum.VIDEO_PLAY.getField(),
                                count
                        );
                    } catch (Exception ignored) {}
                }
        );
    }
}