package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.VideoInfo;
import com.mylive.infra.jpa.entity.vo.BasicVideoInfoVo;
import com.mylive.infra.jpa.entity.vo.PortalVideoInfoVo;
import com.mylive.infra.jpa.entity.vo.UHomeVideoInfoVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface VideoInfoRepository extends JpaRepository<VideoInfo, String> {
    VideoInfo findByVideoId(String videoId);

    @Query("""
    select new com.mylive.infra.jpa.entity.vo.BasicVideoInfoVo(
        v.videoId,
        v.videoTitle,
        v.videoCover
    ) from VideoInfo v
    where v.recommendType = :recommendType
    order by v.createdAt desc
    """)
    List<BasicVideoInfoVo> getRecommendVideoList(Integer recommendType);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.PortalVideoInfoVo(
        v.videoId,
        v.videoTitle,
        v.videoCover,
        v.duration,
        v.createdAt,
        v.playCount,
        v.danmakuCount,
        u.nickname
    ) from VideoInfo v
    join UserInfo u on v.userId = u.userId
    where (:parentCategoryId is null or v.parentCategoryId = :parentCategoryId)
        and (:categoryId is null or v.categoryId = :categoryId)
        and ((:parentCategoryId is not null or :categoryId is not null) or v.recommendType = :recommendType)
    """, countQuery = """
    select count(v) from VideoInfo v
    where (:parentCategoryId is null or v.parentCategoryId = :parentCategoryId)
        and (:categoryId is null or v.categoryId = :categoryId)
        and ((:parentCategoryId is not null or :categoryId is not null) or v.recommendType = :recommendType)
    """)
    Page<PortalVideoInfoVo> getPortalVideoPage(
            Integer parentCategoryId,
            Integer categoryId,
            Integer recommendType,
            Pageable pageable
    );

    @Query("""
    update VideoInfo v
    set v.saveCount = v.saveCount + :saveCount, v.lastUpdatedAt = CURRENT_TIMESTAMP
    where v.videoId = :videoId and v.saveCount + :saveCount >= 0
    """)
    @Modifying
    int updateSaveCountByVideoId(int saveCount, String videoId);

    @Query("""
    update VideoInfo v
    set v.likeCount = v.likeCount + :likeCount, v.lastUpdatedAt = CURRENT_TIMESTAMP
    where v.videoId = :videoId and v.likeCount + :likeCount >= 0
    """)
    @Modifying
    int updateLikeCountByVideoId(int likeCount, String videoId);

    @Query("update VideoInfo v set v.coinCount = v.coinCount + :increment, v.lastUpdatedAt = CURRENT_TIMESTAMP where v.videoId = :videoId")
    @Modifying
    int incrCoinCount(String videoId, Integer increment);

    @Query("update VideoInfo v set v.commentCount = v.commentCount + 1, v.lastUpdatedAt = CURRENT_TIMESTAMP where v.videoId = :videoId")
    @Modifying
    void incrCommentCount(String videoId);

    @Query("""
    update VideoInfo v
    set v.commentCount = v.commentCount - :delta,
        v.lastUpdatedAt = CURRENT_TIMESTAMP
    where v.videoId = :videoId and v.commentCount - :delta >= 0
    """)
    @Modifying
    int decrCommentCount(String videoId, int delta);

    boolean existsByCategoryIdOrParentCategoryId(Integer categoryId, Integer parentCategoryId);

    @Query("update VideoInfo v set v.recommendType = 1 - v.recommendType, v.lastUpdatedAt = CURRENT_TIMESTAMP where v.videoId = :videoId")
    @Modifying
    void updateRecommendTypeByVideoId(String videoId);

    @Query("""
    update VideoInfo v
    set v.danmakuCount = v.danmakuCount + 1, v.lastUpdatedAt = CURRENT_TIMESTAMP
    where v.videoId = :videoId
    and v.allowDanmaku = 1
    """)
    @Modifying
    int incrDanmakuCountIfAllowed(String videoId);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.UHomeVideoInfoVo(
        v.videoId,
        v.videoTitle,
        v.videoCover,
        v.playCount,
        v.createdAt,
        null
    ) from VideoInfo v where v.userId = :userId
    and (:videoTitleFuzzy is null
        or lower(v.videoTitle) like concat('%', :videoTitleFuzzy, '%'))
    """, countQuery = """
    select count(v) from VideoInfo v where v.userId = :userId
    and (:videoTitleFuzzy is null
        or lower(v.videoTitle) like concat('%', :videoTitleFuzzy, '%'))
    """)
    Page<UHomeVideoInfoVo> findUHomeVideoPage(Long userId, String videoTitleFuzzy, Pageable pageable);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.UHomeVideoInfoVo(
        v.videoId,
        v.videoTitle,
        v.videoCover,
        null,
        null,
        u.actionTime
    ) from UserAction u left join VideoInfo v on v.videoId = u.videoId
    where u.actionType = :actionType and u.userId = :userId
    """, countQuery = """
    select count(u) from UserAction u where u.actionType = :actionType and u.userId = :userId
    """)
    Page<UHomeVideoInfoVo> findUHomeSavedVideoPage(Long userId, Integer actionType, Pageable pageable);

    @Query("""
    select new com.mylive.infra.jpa.entity.vo.UHomeVideoInfoVo(
        v.videoId,
        v.videoTitle,
        v.videoCover,
        v.playCount,
        v.createdAt,
        null
    )
    from VideoInfo v
    where v.userId = :userId
        and (
            :seriesId is null or
            not exists (
                select 1
                from UserVideoSeriesVideo u
                where u.seriesId = :seriesId
                and u.userId = :userId
                and u.videoId = v.videoId
            )
        )
    """)
    List<UHomeVideoInfoVo> findUHomeVideoList(Long userId, Long seriesId);

    List<VideoInfo> findByUserIdAndVideoIdIn(Long userId, Collection<String> videoIds);

    @Query("""
    update VideoInfo v set v.allowDanmaku = :allowDanmaku, v.allowComment = :allowComment, v.lastUpdatedAt = current_timestamp
         where v.videoId = :videoId and v.userId = :userId
    """)
    @Modifying
    void updateInteraction(String videoId, Long userId, Integer allowDanmaku, Integer allowComment);

    @Query("""
    select new com.mylive.infra.jpa.entity.vo.BasicVideoInfoVo(
        v.videoId,
        v.videoTitle,
        v.videoCover
    ) from VideoInfo v where v.userId = :userId order by v.createdAt desc
    """)
    List<BasicVideoInfoVo> getBasicVoList(Long userId);

    @Query("update VideoInfo v set v.danmakuCount = v.danmakuCount - 1, v.lastUpdatedAt = CURRENT_TIMESTAMP where v.videoId = :videoId and v.danmakuCount > 0")
    @Modifying
    int decrDanmakuCount(String videoId);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.PortalVideoInfoVo(
        v.videoId,
        v.videoTitle,
        v.videoCover,
        v.duration,
        v.createdAt,
        v.playCount,
        v.danmakuCount,
        u.nickname
    ) from VideoInfo v
    join UserInfo u on v.userId = u.userId
    where v.lastPlayedAt > :timeThreshold
    """, countQuery = """
    select count(v) from VideoInfo v where v.lastPlayedAt > :timeThreshold
    """)
    Page<PortalVideoInfoVo> getHotVideoList(LocalDateTime timeThreshold, Pageable pageable);

    @Query("""
    update VideoInfo v
    set v.playCount = v.playCount + :count,
        v.lastUpdatedAt = current_timestamp,
        v.lastPlayedAt = current_timestamp
    where v.videoId = :videoId""")
    @Modifying
    void incrReadCountAndUpdateTime(String videoId, Integer count);
}
