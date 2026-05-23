package com.mylive.service.statistic.admin;

import com.mylive.enums.StatisticTypeEnum;
import com.mylive.infra.jpa.entity.dto.statistic.AdminStatisticDto;
import com.mylive.infra.jpa.entity.dto.statistic.StatisticTotalDto;
import com.mylive.infra.jpa.repository.StatisticInfoRepository;
import com.mylive.infra.jpa.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatisticServiceImpl implements AdminStatisticService {

    private final StatisticInfoRepository statisticInfoRepository;
    private final UserInfoRepository userInfoRepository;

    @Override
    public Map<String, Object> getRealTimeStatisticInfo() {
        String preDate = LocalDate.now().minusDays(1).toString();
        List<AdminStatisticDto> preDayData = statisticInfoRepository.getAggStatInfo4Admin(preDate);
        // 把粉丝数替换为用户总数
        preDayData.forEach(item -> {
            if (StatisticTypeEnum.FAN.getType().equals(item.getDataType())) {
                item.setStatisticCount(userInfoRepository.count());
            }
        });
        Map<Integer, Long> preDayDataMap = preDayData.stream()
                .collect(Collectors.toMap(
                        AdminStatisticDto::getDataType,
                        AdminStatisticDto::getStatisticCount,
                        (item1, item2) -> item2
                ));
        StatisticTotalDto dto = statisticInfoRepository.getTotalCountInfo(null);
        dto.setUserCount(userInfoRepository.count());
        Map<String, Object> result = new HashMap<>();
        result.put("preDayData", preDayDataMap);
        result.put("totalCountInfo", dto);
        return result;
    }

    @Override
    public List<AdminStatisticDto> getWeekStatisticInfo(Integer dataType) {
        List<String> dateList = new ArrayList<>();
        for (int i = 7; i > 0; i--) {
            dateList.add(LocalDate.now().minusDays(i).toString());
        }
        List<AdminStatisticDto> statisticInfoList;
        if (StatisticTypeEnum.FAN.getType().equals(dataType)) {
            statisticInfoList = statisticInfoRepository.getUserCountStat4Admin(dataType, dateList.get(0), dateList.get(dateList.size() - 1));
        } else {
            statisticInfoList = statisticInfoRepository.getAggStatInfo4Admin(dataType, dateList.get(0), dateList.get(dateList.size() - 1));
        }

        Map<String, AdminStatisticDto> dataMap = statisticInfoList.stream().collect(
                Collectors.toMap(
                        AdminStatisticDto::getStatisticDate,
                        Function.identity(),
                        (data1, data2) -> data2
                )

        );
        List<AdminStatisticDto> resultDataList = new ArrayList<>();
        for (String date : dateList) {
            AdminStatisticDto dataItem = dataMap.get(date);
            if (dataItem == null) {
                dataItem = new AdminStatisticDto();
                dataItem.setStatisticCount(0L);
                dataItem.setStatisticDate(date);
            }
            resultDataList.add(dataItem);
        }
        return resultDataList;
    }
}
