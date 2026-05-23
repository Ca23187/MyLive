package com.mylive.infra.redis;

import co.elastic.clients.util.TriConsumer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mylive.constants.Constants;
import com.mylive.enums.StatisticTypeEnum;
import com.mylive.infra.jpa.entity.dto.SysSettingDto;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.dto.UploadingFileDto;
import com.mylive.infra.jpa.entity.dto.VideoFileCacheDto;
import com.mylive.infra.jpa.entity.po.CategoryInfo;
import com.mylive.infra.jpa.entity.po.VideoPlayHistory;
import com.mylive.infra.jpa.entity.vo.UserCountInfoVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisComponent {

    private final RedisUtils redisUtils;
    private final StringRedisTemplate strRedis;
    private final ObjectMapper mapper;

    public String saveCheckCode(String code) {
        String checkCodeKey = UUID.randomUUID().toString();
        redisUtils.setEx(
                Constants.REDIS_KEY_CHECK_CODE + checkCodeKey,
                code,
                Constants.REDIS_TTL_CHECK_CODE
        );
        return checkCodeKey;
    }

    public String getCheckCode(String checkCodeKey) {
        return redisUtils.get(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }

    public void cleanCheckCode(String checkCodeKey) {
        redisUtils.delete(Constants.REDIS_KEY_CHECK_CODE + checkCodeKey);
    }

    public String saveAdminToken(String account) {
        String token = UUID.randomUUID().toString();
        redisUtils.setEx(
                Constants.REDIS_KEY_TOKEN_ADMIN + token,
                account,
                Constants.REDIS_TTL_ADMIN_TOKEN
        );
        return token;
    }

    public void cleanAdminToken(String token) {
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_ADMIN + token);
    }

    public String getAdminToken(String token) {
        return redisUtils.get(Constants.REDIS_KEY_TOKEN_ADMIN + token);
    }

    public String saveTokenInfo(TokenInfo tokenInfo) {
        String token = UUID.randomUUID().toString();
        redisUtils.setEx(
                Constants.REDIS_KEY_TOKEN_WEB + token,
                tokenInfo,
                Constants.REDIS_TTL_TOKEN_INFO
        );
        return token;
    }

    public boolean needUpdateToken(String token) {
        return redisUtils.ttl(Constants.REDIS_KEY_TOKEN_WEB + token) < Constants.TOKEN_UPDATE_THRESHOLD.toSeconds();
    }

    public void cleanTokenInfo(String token) {
        if (token == null) return;
        redisUtils.delete(Constants.REDIS_KEY_TOKEN_WEB + token);
    }

    public TokenInfo getTokenInfo(String token) {
        if (token == null) return null;
        return redisUtils.get(Constants.REDIS_KEY_TOKEN_WEB + token, TokenInfo.class);
    }

    public void updateTokenInfo(String token, TokenInfo tokenInfo) {
        if (token == null || tokenInfo == null) {
            return;
        }
        String key = Constants.REDIS_KEY_TOKEN_WEB + token;
        long ttl = redisUtils.ttl(key);
        if (ttl <= 0) {
            return;
        }
        redisUtils.setEx(key, tokenInfo, Duration.ofSeconds(ttl));
    }

    public void keepOldTokenForGracePeriod(String token) {
        String key = Constants.REDIS_KEY_TOKEN_WEB + token;
        // 如果旧token本来剩余时间比过渡期还短，就不动
        if (redisUtils.ttl(key) > Constants.TOKEN_GRACE_PERIOD.toSeconds()) {
            redisUtils.expire(key, Constants.TOKEN_GRACE_PERIOD);
        }
    }

    public void saveCategoryList(List<CategoryInfo> list) {
        redisUtils.set(Constants.REDIS_KEY_CATEGORY_LIST, list);
    }

    public List<CategoryInfo> getCategoryList() {
        return redisUtils.get(Constants.REDIS_KEY_CATEGORY_LIST, new TypeReference<>() {});
    }

    public void savePreUploadFileInfo(Long userId, String uploadId, UploadingFileDto dto) {
        redisUtils.setEx(
                Constants.REDIS_KEY_UPLOADING_FILE + userId + ":" + uploadId,
                dto,
                Constants.REDIS_TTL_UPLOADING_FILE
        );
    }

    public UploadingFileDto getUploadingFileInfo(Long userId, String uploadId) {
        return redisUtils.get(Constants.REDIS_KEY_UPLOADING_FILE + userId + ":" + uploadId, UploadingFileDto.class);
    }

    public void delUploadingFileInfo(Long userId, String uploadId) {
        redisUtils.delete(Constants.REDIS_KEY_UPLOADING_FILE + userId+ ":" + uploadId);
    }

    public SysSettingDto getSysSettingDto() {
        SysSettingDto sysSettingDto = redisUtils.get(Constants.REDIS_KEY_SYS_SETTING, SysSettingDto.class);
        if (sysSettingDto == null) {
            sysSettingDto = new SysSettingDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingDto);
        }
        return sysSettingDto;
    }

    public void saveSysSettingDto(SysSettingDto sysSettingDto) {
        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingDto);
    }

    public void delUploadedFileInfo(Long userId, String uploadId) {
        redisUtils.delete(Constants.REDIS_KEY_UPLOADING_FILE + userId + ":" + uploadId);
    }

    public void saveDelFilePathList(String videoId, List<String> filePathList) {
        redisUtils.setEx(
                Constants.REDIS_KEY_FILE_DEL_PATH_LIST + videoId,
                filePathList,
                Constants.REDIS_TTL_FILE_DEL_PATH_LIST
        );
    }

    public List<String> getDelFilePathList(String videoId) {
        return redisUtils.get(Constants.REDIS_KEY_FILE_DEL_PATH_LIST + videoId, new TypeReference<>() {});
    }

    public void cleanDelFilePathList(String videoId) {
        redisUtils.delete(Constants.REDIS_KEY_FILE_DEL_PATH_LIST + videoId);
    }

    public void addFileIds2TranscodeQueue(List<String> fileIdList) {
        redisUtils.lpushStringAll(
                Constants.REDIS_KEY_FILE_TRANSCODE_QUEUE,
                fileIdList,
                null
        );
    }

    public String getFileIdFromTranscodeQueue() {
        return redisUtils.brpop(
                Constants.REDIS_KEY_FILE_TRANSCODE_QUEUE,
                Constants.TRANSCODE_CONSUME_BLOCK
        );
    }

    /**
     * NOTE: 用 ZSET 实现在线人数估计
     * ZSET 原理：维护一个记录 （key，score） 的集合，把用户 heart beat 时间戳当作 score
     * 每次塞入新用户，让 ZSET 清除 25 秒内未活跃用户，然后用 ZCARD 统计集合数量，从而估计在线人数
     * 记得给 ZSET 设置 exp 并续命
     */
    public Long reportVideoPlayOnline(String fileId, String deviceId) {
        String key = Constants.REDIS_KEY_VIDEO_ONLINE_COUNT_ONLINE + fileId;
        long now = System.currentTimeMillis();
        // 更新在线时间
        redisUtils.zadd(key, deviceId, now);

        // 清理25秒未活跃用户
        redisUtils.zremRangeByScore(key, 0, now - Constants.VIDEO_ONLINE_ZSET_CLEAN_MILLI);

        // 给整个 key 续命（50秒）
        redisUtils.expire(key, Constants.VIDEO_ONLINE_ZSET_TTL);

        // 返回在线人数
        return redisUtils.zcard(key);
    }

    // NOTE: 按天分桶的滑动窗口法：每一天当作一个桶，有效期 7 天
    public void addKeywordCount(String keyword) {
        String dayBucketKey = Constants.REDIS_KEY_HOT_KEYWORDS_BUCKET + LocalDate.now();
        redisUtils.zaddCount(dayBucketKey, keyword);
        redisUtils.expire(dayBucketKey, Constants.REDIS_KEY_TTL_HOT_KEY);
    }

    public List<String> getHotKeywords(int days, int topN) {
        String resultKey = Constants.REDIS_KEY_HOT_KEYWORDS_AGG + days;  // ...:agg:7 表示7天热榜

        // 先查已经聚合好的结果，查不到就算聚合
        List<String> cached = redisUtils.getZSetList(resultKey, topN);
        if (!cached.isEmpty()) {
            return cached;
        }

        // NOTE: 注意：这里有缓存击穿问题：cached 过期后，如果有大量线程同时涌入，可能会导致同时进行聚合计算，Redis 压力增大
        // NOTE: 单机用 synchronized，分布式用 Redisson 加写锁，但对这小破项目没必要

        // 准备聚合最近 7 天的bucket
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            keys.add(Constants.REDIS_KEY_HOT_KEYWORDS_BUCKET + LocalDate.now().minusDays(i));
        }

        // 聚合完存 cache，有效期 5 min
        strRedis.opsForZSet().unionAndStore(keys.get(0), keys.subList(1, keys.size()), resultKey);
        redisUtils.expire(resultKey, Constants.REDIS_TTL_HOT_KEYWORDS_AGG);
        return redisUtils.getZSetList(resultKey, topN);
    }

    public VideoFileCacheDto getVideoFileCache(String fileId) {
        return redisUtils.get(Constants.REDIS_KEY_VIDEO_FILE + fileId, VideoFileCacheDto.class);
    }

    public void saveVideoFileCache(VideoFileCacheDto dto) {
        redisUtils.setEx(Constants.REDIS_KEY_VIDEO_FILE + dto.getFileId(),
                dto, Constants.REDIS_TTL_VIDEO_FILE);
    }

    public boolean isVideoFileNegCached(String fileId) {
        return redisUtils.get(Constants.REDIS_KEY_VIDEO_FILE_NEG + fileId) != null;
    }

    public void saveVideoFileNegCache(String fileId) {
        redisUtils.setEx(Constants.REDIS_KEY_VIDEO_FILE_NEG + fileId,
                "1", Constants.REDIS_TTL_VIDEO_FILE_NEG);
    }

    // NOTE: {key: videoId, value: count}，每次播放后 count 自增，并且给 hash 内的该条内容续期
    // NOTE: ttl 的期限（当前为 10 分钟）必须远大于 flush 时间 （当前为60秒）
    public void incrVideoPlayCount(String videoId) {
        redisUtils.hIncrementAndExpireField(
                Constants.REDIS_KEY_VIDEO_PLAY_COUNT,
                videoId,
                1,
                Duration.ofMinutes(10)
        );
    }

    public void flushVideoPlayCount(BiConsumer<String, Integer> consumer) {
        Map<String, String> all = redisUtils.hGetAll(Constants.REDIS_KEY_VIDEO_PLAY_COUNT);
        if (all.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : all.entrySet()) {
            String videoId = entry.getKey();
            int count;
            try {
                count = Integer.parseInt(entry.getValue());
            } catch (Exception e) {
                continue;
            }
            if (count <= 0) {
                continue;
            }
            try {
                consumer.accept(videoId, count);
            } catch (Exception e) {
                log.error("flush video play count failed, videoId={}, count={}", videoId, count, e);
            }
        }
    }

    public VideoPlayHistory getVideoPlayHistory(Long userId, String videoId) {
        return redisUtils.hGet(
                Constants.REDIS_KEY_VIDEO_PLAY_HISTORY,
                userId + ":" + videoId,
                VideoPlayHistory.class
        );
    }

    public void saveVideoPlayHistory(VideoPlayHistory history) {
        redisUtils.hSet(
                Constants.REDIS_KEY_VIDEO_PLAY_HISTORY,
                history.getUserId() + ":" + history.getVideoId(),
                history
        );
    }

    public void flushVideoPlayHistory(TriConsumer<String, String, VideoPlayHistory> consumer) {
        Map<String, String> all = redisUtils.hGetAll(Constants.REDIS_KEY_VIDEO_PLAY_HISTORY);
        if (all == null || all.isEmpty()) {
            return;
        }

        for (Map.Entry<String, String> entry : all.entrySet()) {
            VideoPlayHistory history;
            try {
                history = mapper.readValue(entry.getValue(), VideoPlayHistory.class);
            } catch (Exception e) {
                log.error("parse video play history failed, field={}, value={}", entry.getKey(), entry.getValue(), e);
                continue;
            }

            try {
                consumer.accept(entry.getKey(), entry.getValue(), history);
            } catch (Exception e) {
                log.error("flush video play history failed, field={}", entry.getKey(), e);
            }
        }
    }

    public UserCountInfoVo getUserCountInfo(Long userId) {
        return redisUtils.get(Constants.REDIS_KEY_USER_COUNT_INFO + userId, UserCountInfoVo.class);
    }

    public void saveUserCountInfo(UserCountInfoVo userCountInfoVo, Long userId) {
        redisUtils.setEx(
                Constants.REDIS_KEY_USER_COUNT_INFO + userId, 
                userCountInfoVo, 
                Constants.REDIS_TTL_USER_COUNT_INFO
        );
    }

    public void cleanUserCountInfo(Long userId) {
        redisUtils.delete(Constants.REDIS_KEY_USER_COUNT_INFO + userId);
    }

    public void updateStatisticCount(Long videoUserId, String videoId, int delta, StatisticTypeEnum statisticType) {
        String date = LocalDate.now().toString();
        String key = Constants.REDIS_KEY_STAT_PREFIX + statisticType.getRedisPrefix() + date + ":" + videoUserId;
        redisUtils.hIncrement(key, videoId, delta);

        // 第一次建 hash 的时候设置TTL
        Long expire = redisUtils.ttl(key);
        if (expire == null || expire < 0) {
            redisUtils.expire(key, Constants.REDIS_TTL_STAT);
        }

        String userSetKey = Constants.REDIS_KEY_STAT_USER_SETS + date;
        redisUtils.sAdd(userSetKey, String.valueOf(videoUserId));
        redisUtils.expire(userSetKey, Constants.REDIS_TTL_STAT);
    }

    public void updateStatisticFanCount(Long followUserId, int delta, StatisticTypeEnum statisticType) {
        String date = LocalDate.now().toString();
        String key = Constants.REDIS_KEY_STAT_PREFIX + statisticType.getRedisPrefix() + date + ":" + followUserId;
        redisUtils.incrementAndExpireIfAbsent(key, Constants.REDIS_TTL_STAT, delta);

        String userSetKey = Constants.REDIS_KEY_STAT_USER_SETS + date;
        redisUtils.sAdd(userSetKey, String.valueOf(followUserId));
        redisUtils.expire(userSetKey, Constants.REDIS_TTL_STAT);
    }

    public void saveEmailCode(String email, String code) {
        redisUtils.setEx(Constants.REDIS_KEY_EMAIL_CODE + email, code, Constants.REDIS_TTL_EMAIL_CODE);
    }

    public String getEmailCode(String email) {
        return redisUtils.get(Constants.REDIS_KEY_EMAIL_CODE + email);
    }

    public void cleanEmailCode(String email) {
        redisUtils.delete(Constants.REDIS_KEY_EMAIL_CODE + email);
    }
}
