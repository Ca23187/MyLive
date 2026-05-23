package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.dto.statistic.AdminStatisticDto;
import com.mylive.infra.jpa.entity.dto.statistic.StatisticTotalDto;
import com.mylive.infra.jpa.entity.po.StatisticInfo;
import com.mylive.infra.jpa.entity.po.id.StatisticInfoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StatisticInfoRepository extends JpaRepository<StatisticInfo, StatisticInfoId> {
    List<StatisticInfo> findByStatisticDateAndUserId(String statisticDate, Long userId);

    @Query("""
    select new com.mylive.infra.jpa.entity.dto.statistic.StatisticTotalDto(
        coalesce(sum(v.playCount),0),
        coalesce(sum(v.likeCount),0),
        coalesce(sum(v.danmakuCount),0),
        coalesce(sum(v.commentCount),0),
        coalesce(sum(v.coinCount),0),
        coalesce(sum(v.saveCount),0),
        0
    )
    from VideoInfo v
    where (:userId is null or v.userId = :userId)
""")
    StatisticTotalDto getTotalCountInfo(Long userId);

    List<StatisticInfo> findByDataTypeAndUserIdAndStatisticDateBetweenOrderByStatisticDateAsc(Integer dataType, Long userId, String statisticDateAfter, String statisticDateBefore);

    @Query("""
    select new com.mylive.infra.jpa.entity.dto.statistic.AdminStatisticDto(
        s.dataType,
        coalesce(sum(s.statisticCount), 0),
        s.statisticDate
    ) from StatisticInfo s group by s.dataType, s.statisticDate
    """)
    List<AdminStatisticDto> getAggStatInfo4Admin(String preDate);

    @Query("""
    select new com.mylive.infra.jpa.entity.dto.statistic.AdminStatisticDto(
        count(u),
        cast(function('date_format', u.createdAt, '%Y-%m-%d') as string)
    ) from UserInfo u group by cast(function('date_format', u.createdAt, '%Y-%m-%d') as string)
        order by cast(function('date_format', u.createdAt, '%Y-%m-%d') as string)
    """)
    List<AdminStatisticDto> getUserCountStat4Admin(Integer dataType, String startDate, String endDate);

    @Query("""
    select new com.mylive.infra.jpa.entity.dto.statistic.AdminStatisticDto(
        s.dataType,
        coalesce(sum(s.statisticCount), 0),
        s.statisticDate
    ) from StatisticInfo s
        where s.dataType = :dataType and s.statisticDate >= :startDate and s.statisticDate <= :endDate
            group by s.dataType, s.statisticDate order by s.statisticDate asc
    """)
    List<AdminStatisticDto> getAggStatInfo4Admin(Integer dataType, String startDate, String endDate);
}
