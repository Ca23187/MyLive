package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.VideoComment;
import com.mylive.infra.jpa.entity.vo.VideoCommentVoItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface VideoCommentRepository extends JpaRepository<VideoComment, Long> {

    VideoComment findByCommentId(Long commentId);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.VideoCommentVoItem(
        c.commentId,
        c.parentCommentId,
        c.userId,
        c.avatar,
        c.nickname,
        c.content,
        c.imgPath,
        c.replyUserId,
        c.replyNickname,
        c.postedAt,
        c.likeCount,
        c.dislikeCount,
        c.topType,
        c.replyCount,
        c.mentionJson,
        null,
        null,
        null,
        null,
        null
    )
    from VideoComment c
    where c.videoId = :videoId and c.parentCommentId = 0
    """, countQuery = """
    select count(c)
    from VideoComment c
    where c.videoId = :videoId and c.parentCommentId = 0
    """)
    Page<VideoCommentVoItem> getCommentsWithUser(String videoId, Pageable pageable);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.VideoCommentVoItem(
        c.commentId,
        c.parentCommentId,
        c.userId,
        c.avatar,
        c.nickname,
        c.content,
        c.imgPath,
        c.replyUserId,
        c.replyNickname,
        c.postedAt,
        c.likeCount,
        c.dislikeCount,
        c.topType,
        c.replyCount,
        c.mentionJson,
        null,
        null,
        null,
        null,
        null
    )
    from VideoComment c
    where c.parentCommentId = :parentId
    """, countQuery = """
    select count(c) from VideoComment c where c.parentCommentId = :parentId
    """)
    Page<VideoCommentVoItem> getReplyWithUser(Long parentId, Pageable pageable);

    @Modifying
    @Query("""
    update VideoComment c
    set c.replyCount = c.replyCount + 1
    where c.commentId = :commentId
    """)
    void increaseReplyCount(Long commentId);


    @Modifying
    @Query("""
    update VideoComment c
    set c.replyCount = c.replyCount - 1
    where c.commentId = :commentId and c.replyCount > 0
    """)
    int decrReplyCount(Long commentId);

    @Modifying
    @Query("""
    update VideoComment vc
    set vc.likeCount = vc.likeCount + :likeDelta,
        vc.dislikeCount = vc.dislikeCount + :dislikeDelta
    where vc.commentId = :commentId
        and vc.likeCount + :likeDelta >= 0
        and vc.dislikeCount + :dislikeDelta >= 0
    """)
    int updateCount(Long commentId, int likeDelta, int dislikeDelta);

    @Query("select c.videoId from VideoComment c where c.commentId = :commentId")
    String findVideoIdByCommentId(Long commentId);

    @Modifying
    @Query("""
    update VideoComment c
    set c.topType = :topType
    where c.commentId = :commentId and c.videoUserId = :videoUserId
    """)
    int updateTopTypeByCommentIdAndVideoUserIdAndVideoId(Integer topType, Long commentId, Long videoUserId, String videoId);

    @Modifying
    @Query("""
    update VideoComment c
    set c.topType = :noTop
    where c.videoId = :videoId
    and c.topType = :top
    and c.commentId != :commentId
    """)
    void clearTopByVideoId(String videoId, Integer noTop, Integer top, Long commentId);

    @Modifying
    @Query("""
    update VideoComment c
    set c.topType = :topType
    where c.commentId = :commentId
      and c.videoId in (select v.videoId from VideoInfo v where v.userId = :userId)
    """)
    int updateTopTypeWithAuth(Integer topType, Long commentId, Long userId);

    int deleteByParentCommentId(Long parentCommentId);

    void deleteAllByVideoId(String videoId);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.VideoCommentVoItem(
        c.commentId,
        c.parentCommentId,
        c.userId,
        c.avatar,
        c.nickname,
        c.content,
        c.imgPath,
        c.replyUserId,
        c.replyNickname,
        c.postedAt,
        null,
        null,
        null,
        null,
        c.mentionJson,
        v.videoId,
        v.videoTitle,
        v.videoCover,
        c.videoUserId,
        null
    ) from VideoComment c join VideoInfo v on c.videoId = v.videoId
    where (:userId is null or (c.userId = :userId or c.videoUserId = :userId))
        and (:videoId is null or c.videoId = :videoId)
    """, countQuery = """
    select count(c) from VideoComment c
    where (:userId is null or (c.userId = :userId or c.videoUserId = :userId))
        and (:videoId is null or c.videoId = :videoId)
    """)
    Page<VideoCommentVoItem> findUCenterCommentPage(Long userId, String videoId, Pageable pageable);

    int deleteByCommentId(Long commentId);

    @Modifying
    @Query("delete from VideoComment c where c.commentId = :commentId and (c.userId = :userId or c.videoUserId = :userId)")
    int deleteByCommentIdAndAuth(Long commentId, Long userId);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.VideoCommentVoItem(
        c.commentId,
        c.parentCommentId,
        c.userId,
        c.avatar,
        c.nickname,
        c.content,
        c.imgPath,
        c.replyUserId,
        c.replyNickname,
        c.postedAt,
        c.likeCount,
        c.dislikeCount,
        c.topType,
        c.replyCount,
        c.mentionJson,
        v.videoId,
        v.videoTitle,
        v.videoCover,
        c.videoUserId,
        null
    ) from VideoComment c join VideoInfo v on c.videoId = v.videoId
    where (:videoTitleFuzzy is null
    or lower(v.videoTitle) like concat('%', :videoTitleFuzzy, '%'))
    """, countQuery = """
    select count(c) from VideoComment c join VideoInfo v on c.videoId = v.videoId
    where (:videoTitleFuzzy is null
    or lower(v.videoTitle) like concat('%', :videoTitleFuzzy, '%'))
    """)
    Page<VideoCommentVoItem> findAdminCommentPage(String videoTitleFuzzy, Pageable pageable);
}
