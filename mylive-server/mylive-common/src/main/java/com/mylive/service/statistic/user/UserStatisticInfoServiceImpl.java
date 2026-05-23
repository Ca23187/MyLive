package com.mylive.service.statistic.user;

import com.mylive.constants.Constants;
import com.mylive.enums.StatisticTypeEnum;
import com.mylive.infra.jpa.entity.dto.statistic.StatisticTotalDto;
import com.mylive.infra.jpa.entity.po.StatisticInfo;
import com.mylive.infra.jpa.repository.StatisticInfoRepository;
import com.mylive.infra.jpa.repository.UserFollowRepository;
import com.mylive.infra.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserStatisticInfoServiceImpl implements UserStatisticInfoService {

    private final RedisUtils redisUtils;
    private final StatisticInfoRepository statisticInfoRepository;
    private final UserFollowRepository userFollowRepository;

    @Override
    public Map<String, Object> getRealTimeStatisticInfo(Long userId) {
        String preDate = LocalDate.now().minusDays(1).toString();
        List<StatisticInfo> preDayData =
                statisticInfoRepository.findByStatisticDateAndUserId(preDate, userId);
        Map<Integer, Integer> preDayDataMap = preDayData.stream()
                .collect(Collectors.toMap(
                        StatisticInfo::getDataType,
                        StatisticInfo::getStatisticCount,
                        (item1, item2) -> item2)
                );

        StatisticTotalDto dto = statisticInfoRepository.getTotalCountInfo(userId);
        dto.setUserCount(userFollowRepository.countByFollowUserId(userId));

        Map<String, Object> result = new HashMap<>();
        result.put("preDayData", preDayDataMap);
        result.put("totalCountInfo", dto);
        return result;
    }


    @Override
    public List<StatisticInfo> getWeekStatisticInfo(Integer dataType, Long userId) {
        List<String> dateList = new ArrayList<>();
        for (int i = 7; i > 0; i--) {
            dateList.add(LocalDate.now().minusDays(i).toString());
        }
        List<StatisticInfo> statisticInfoList =
                statisticInfoRepository
                        .findByDataTypeAndUserIdAndStatisticDateBetweenOrderByStatisticDateAsc(
                                dataType, userId, dateList.get(0), dateList.get(dateList.size() - 1));

        Map<String, StatisticInfo> dataMap =
                statisticInfoList.stream().collect(
                        Collectors.toMap(
                                StatisticInfo::getStatisticDate,
                                Function.identity(),
                                (data1, data2) -> data2
                        )
                );
        List<StatisticInfo> resultDataList = new ArrayList<>();
        for (String date : dateList) {
            StatisticInfo dataItem = dataMap.get(date);
            if (dataItem == null) {
                dataItem = new StatisticInfo();
                dataItem.setStatisticCount(0);
                dataItem.setStatisticDate(date);
            }
            resultDataList.add(dataItem);
        }
        return resultDataList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void flushStatisticByType(String date, StatisticTypeEnum type) {
        Set<String> userIds = redisUtils.sMembers(Constants.REDIS_KEY_STAT_USER_SETS + date);

        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        List<StatisticInfo> list = new ArrayList<>();

        for (String userIdStr : userIds) {
            long userId = Long.parseLong(userIdStr);
            String key = Constants.REDIS_KEY_STAT_PREFIX + type.getRedisPrefix() + date + ":" + userId;

            int count = getStatisticCount(key, type);

            StatisticInfo info = new StatisticInfo();
            info.setStatisticDate(date);
            info.setUserId(userId);
            info.setDataType(type.getType());
            info.setStatisticCount(count);

            list.add(info);
        }

        statisticInfoRepository.saveAll(list);
    }

    private int getStatisticCount(String key, StatisticTypeEnum type) {
        return switch (type) {

            // hash 类型：按视频维度累计
            case PLAY, LIKE, SAVE, COIN, COMMENT, DANMAKU -> {
                List<Object> values = redisUtils.hValues(key);

                int sum = 0;

                for (Object value : values) {
                    if (value == null) {
                        continue;
                    }

                    try {
                        sum += Integer.parseInt((String) value);
                    } catch (Exception e) {
                        log.warn("parse hash value failed, key={}, value={}", key, value);
                    }
                }

                yield sum;
            }

            // string 类型：单值统计
            case FAN -> {
                String value = redisUtils.get(key);

                if (!StringUtils.hasText(value)) {
                    yield 0;
                }

                try {
                    yield Integer.parseInt(value);
                } catch (Exception e) {
                    log.warn("parse statistic count failed, key={}, value={}", key, value);
                    yield 0;
                }
            }
        };
    }
}
