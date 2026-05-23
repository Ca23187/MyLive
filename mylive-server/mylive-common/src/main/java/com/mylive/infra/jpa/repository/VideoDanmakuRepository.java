package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.VideoDanmaku;
import com.mylive.infra.jpa.entity.vo.AdminDanmakuVo;
import com.mylive.infra.jpa.entity.vo.UCenterDanmakuVo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VideoDanmakuRepository extends JpaRepository<VideoDanmaku, Long> {
    List<VideoDanmaku> findByFileIdOrderByDanmakuIdAsc(String fileId);

    void deleteAllByVideoId(String videoId);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.UCenterDanmakuVo(
        d.danmakuId,
        d.text,
        d.time,
        d.postedAt,
        v.videoId,
        v.userId,
        v.videoTitle,
        v.videoCover
    ) from VideoDanmaku d join VideoInfo v on v.videoId = d.videoId
    where (d.userId = :userId or d.videoUserId = :userId)
        and (:videoId is null or d.videoId = :videoId)
    """, countQuery = """
    select count(d) from VideoDanmaku d
    where (d.userId = :userId or d.videoUserId = :userId)
        and (:videoId is null or d.videoId = :videoId)
    """)
    Page<UCenterDanmakuVo> findUCenterVoPage(String videoId, Long userId, Pageable pageable);


    int deleteByDanmakuId(Long danmakuId);

    @Modifying
    @Query("delete from VideoDanmaku d where d.danmakuId = :danmakuId and (d.userId = :userId or d.videoUserId = :userId)")
    int deleteByDanmakuIdAndAuth(Long danmakuId, Long userId);

    @Query(value = """
    select new com.mylive.infra.jpa.entity.vo.AdminDanmakuVo(
        d.danmakuId,
        d.text,
        d.time,
        d.postedAt,
        v.videoId,
        v.userId,
        v.videoTitle,
        v.videoCover,
        v.userId,
        u.nickname,
        u.avatar
    ) from VideoDanmaku d
    join VideoInfo v on v.videoId = d.videoId
    join UserInfo u on u.userId = d.userId
    where (:videoTitleFuzzy is null
    or lower(v.videoTitle) like concat('%', :videoTitleFuzzy, '%'))
    """, countQuery = """
    select count(d) from VideoDanmaku d
    join VideoInfo v on v.videoId = d.videoId
    where (:videoTitleFuzzy is null
    or lower(v.videoTitle) like concat('%', :videoTitleFuzzy, '%'))
    """)
    Page<AdminDanmakuVo> findAdminVoPage(String videoTitleFuzzy, Pageable pageable);
}
