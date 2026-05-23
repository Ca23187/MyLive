package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.UserVideoSeries;
import com.mylive.infra.jpa.entity.vo.VideoSeriesWithVideoVo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserVideoSeriesRepository extends JpaRepository<UserVideoSeries, Long> {

    List<UserVideoSeries> findAllByUserId(Long userId);

    @Query("""
    select coalesce(max(u.orderNum), 0) from UserVideoSeries u
    where u.userId = :userId
    """)
    Integer findMaxOrder(Long userId);

    @Query("""
    update UserVideoSeries u
    set u.seriesName = :seriesName, u.seriesDescription = :seriesDescription
    where u.userId = :userId and u.seriesId = :seriesId
    """)
    @Modifying
    int updateSeriesNameAndSeriesDescriptionByUserIdAndSeriesId(String seriesName, String seriesDescription, Long userId, Long seriesId);

    @Query("update UserVideoSeries u set u.cover = :cover where u.seriesId = :seriesId")
    @Modifying
    void updateCoverBySeriesId(String cover, Long seriesId);

    UserVideoSeries findByUserIdAndSeriesId(Long userId, Long seriesId);

    int deleteByUserIdAndSeriesId(Long userId, Long seriesId);

    @Modifying
    @Query(value = """
    UPDATE user_video_series s
    JOIN video_info v ON v.video_id = :videoId AND v.user_id = :userId
    SET s.cover = v.video_cover
    WHERE s.series_id = :seriesId
    """, nativeQuery = true)
    int updateCoverByVideoId(Long seriesId, String videoId, Long userId);

    List<UserVideoSeries> findByUserId(Long userId);

    @Modifying
    @Query("""
    update UserVideoSeries s
    set s.orderNum = :orderNum
    where s.seriesId = :seriesId
      and s.userId = :userId
    """)
    int updateOrderNumBySeriesIdAndUserId(Integer orderNum, Long seriesId, Long userId);

    @Query("""
    select new com.mylive.infra.jpa.entity.vo.VideoSeriesWithVideoVo$FlatDto(
        s.seriesId,
        s.seriesName,
        v.videoId,
        v.videoCover,
        v.videoTitle,
        v.playCount,
        v.createdAt
    )
    from UserVideoSeries s
    left join UserVideoSeriesVideo sv on s.seriesId = sv.seriesId
    left join VideoInfo v on sv.videoId = v.videoId
    where s.userId = :userId
    order by s.orderNum asc, sv.orderNum asc
    """)
    List<VideoSeriesWithVideoVo.FlatDto> findSeriesWithVideosFlat(Long userId);
}
