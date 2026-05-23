package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.UserVideoSeriesVideo;
import com.mylive.infra.jpa.entity.po.id.UserVideoSeriesVideoId;
import com.mylive.infra.jpa.entity.vo.UserVideoSeriesDetailVo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserVideoSeriesVideoRepository extends JpaRepository<UserVideoSeriesVideo, UserVideoSeriesVideoId> {

    @Query("""
    select coalesce(max(u.orderNum), 0) from UserVideoSeriesVideo u
    where u.seriesId = :seriesId
    """)
    Integer findMaxOrder(Long seriesId);

    // JPA 内部类写法：用 $ 连接
    @Query("""
    select new com.mylive.infra.jpa.entity.vo.UserVideoSeriesDetailVo$UserVideoSeriesVideoVo(
        u.seriesId,
        u.videoId,
        v.videoTitle,
        v.videoCover,
        v.playCount,
        v.createdAt
    ) from UserVideoSeriesVideo u left join VideoInfo v on u.videoId = v.videoId
    where u.seriesId = :seriesId order by u.orderNum asc
    """)
    List<UserVideoSeriesDetailVo.UserVideoSeriesVideoVo> findVideoSeriesVideoVoList(Long seriesId);

    @Query(value = """
    SELECT v.video_cover
    FROM user_video_series_video u
    JOIN video_info v ON u.video_id = v.video_id
    WHERE u.series_id = :seriesId
      AND u.user_id = :userId
    ORDER BY u.order_num DESC
    LIMIT 1
    """, nativeQuery = true)
    String findLatestCover(Long seriesId, Long userId);

    void deleteByUserIdAndSeriesId(Long userId, Long seriesId);

    List<UserVideoSeriesVideo> findByUserIdAndSeriesId(Long userId, Long seriesId);

    @Modifying
    @Query("""
    update UserVideoSeriesVideo u
    set u.orderNum = :orderNum
    where u.userId = :userId
      and u.seriesId = :seriesId
      and u.videoId = :videoId
    """)
    int updateOrderNum(Long userId, Long seriesId, String videoId, Integer orderNum);

    int deleteByUserIdAndSeriesIdAndVideoId(Long userId, Long seriesId, String videoId);
}
