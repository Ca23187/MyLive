package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.VideoInfoPost;
import com.mylive.infra.jpa.entity.vo.VideoInfoPostVo;
import com.mylive.infra.jpa.entity.vo.VideoStatusCountVo;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VideoInfoPostRepository extends JpaRepository<VideoInfoPost, String> {

    VideoInfoPost findByVideoId(String videoId);

    @Query("update VideoInfoPost v set v.status = :status, v.lastUpdatedAt = CURRENT_TIMESTAMP where v.videoId = :videoId")
    @Modifying
    void updateStatusByVideoId(Integer status, String videoId);

    @Query("update VideoInfoPost v set v.status = :status, v.duration = :duration, v.lastUpdatedAt = CURRENT_TIMESTAMP where v.videoId = :videoId")
    @Modifying
    void updateStatusAndDurationByVideoId(Integer status, Integer duration, String videoId);

    @Query(value = """
        select new com.mylive.infra.jpa.entity.vo.VideoInfoPostVo(
            v.videoId,
            v.videoCover,
            v.videoTitle,
            v.userId,
            v.status,
            v.createdAt,
            v.duration,
            v.lastUpdatedAt,
            c.playCount,
            c.likeCount,
            c.danmakuCount,
            c.commentCount,
            c.coinCount,
            c.saveCount,
            c.recommendType,
            null,
            null
        )
        from VideoInfoPost v
        left join VideoInfo c on c.videoId = v.videoId
        where v.userId = :userId
          and (:status is null or v.status = :status)
          and (:videoTitleFuzzy is null
              or lower(v.videoTitle) like concat('%', :videoTitleFuzzy, '%'))
          and (:excludeStatusList is null or v.status not in :excludeStatusList)
    """, countQuery = """
        select count(v)
        from VideoInfoPost v
        where v.userId = :userId
          and (:status is null or v.status = :status)
          and (:videoTitleFuzzy is null
              or lower(v.videoTitle) like concat('%', :videoTitleFuzzy, '%'))
          and (:excludeStatusList is null or v.status not in :excludeStatusList)
    """
    )
    Page<VideoInfoPostVo> findPostPage4User(
            Long userId,
            Integer status,
            String videoTitleFuzzy,
            List<Integer> excludeStatusList,
            Pageable pageable
    );

    @Query(value = """
        select new com.mylive.infra.jpa.entity.vo.VideoInfoPostVo(
            v.videoId,
            v.videoCover,
            v.videoTitle,
            v.userId,
            v.status,
            v.createdAt,
            v.duration,
            v.lastUpdatedAt,
            c.playCount,
            c.likeCount,
            c.danmakuCount,
            c.commentCount,
            c.coinCount,
            c.saveCount,
            c.recommendType,
            u.nickname,
            u.avatar
        )
        from VideoInfoPost v
        left join VideoInfo c on c.videoId = v.videoId
        left join UserInfo u on u.userId = v.userId
        where (:categoryId is null or v.categoryId = :categoryId)
          and (:parentCategoryId is null or v.parentCategoryId = :parentCategoryId)
          and (:videoTitleFuzzy is null
              or lower(v.videoTitle) like concat('%', :videoTitleFuzzy, '%'))
          and (
                :recommendType is null
                or (:recommendType = 0 and (c.recommendType = 0 or c.recommendType is null))
                or (:recommendType = 1 and c.recommendType = 1)
              )
    """, countQuery = """
        select count(v)
        from VideoInfoPost v
        left join VideoInfo c on c.videoId = v.videoId
        left join UserInfo u on u.userId = v.userId
        where (:categoryId is null or v.categoryId = :categoryId)
          and (:parentCategoryId is null or v.parentCategoryId = :parentCategoryId)
          and (:videoTitleFuzzy is null
              or lower(v.videoTitle) like concat('%', :videoTitleFuzzy, '%'))
          and (
                :recommendType is null
                or (:recommendType = 0 and (c.recommendType = 0 or c.recommendType is null))
                or (:recommendType = 1 and c.recommendType = 1)
              )
    """
    )
    Page<VideoInfoPostVo> findPostPage4Admin(
            String videoTitleFuzzy,
            Integer categoryId,
            Integer parentCategoryId,
            Integer recommendType,
            Pageable pageable
    );

    @Query("""
    select new com.mylive.infra.jpa.entity.vo.VideoStatusCountVo(
        sum(case when v.status = :passedStatus then 1 else 0 end),
        sum(case when v.status = :rejectedStatus then 1 else 0 end),
        sum(case when v.status not in (:passedStatus, :rejectedStatus) then 1 else 0 end)
    )
    from VideoInfoPost v
    where v.userId = :userId
""")
    VideoStatusCountVo countVideoStatus(
            Long userId,
            Integer passedStatus,
            Integer rejectedStatus
    );

    @Query("update VideoInfoPost v set v.status = :newStatus, v.lastUpdatedAt = CURRENT_TIMESTAMP where v.videoId = :videoId and v.status = :oldStatus")
    @Modifying
    int updateStatusByVideoIdAndStatus(Integer newStatus, String videoId, Integer oldStatus);

    VideoInfoPost findByUserIdAndVideoId(Long userId, String videoId);

    @Query("""
    update VideoInfoPost v set v.allowDanmaku = :allowDanmaku, v.allowComment = :allowComment, v.lastUpdatedAt = current_timestamp
         where v.videoId = :videoId and v.userId = :userId
    """)
    @Modifying
    void updateInteraction(String videoId, Long userId, Integer allowDanmaku, Integer allowComment);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VideoInfoPost v where v.videoId = :videoId")
    void lockByVideoId(String videoId);
}
