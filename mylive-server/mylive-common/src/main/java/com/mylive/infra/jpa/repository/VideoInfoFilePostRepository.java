package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.VideoInfoFilePost;
import com.mylive.infra.jpa.entity.vo.VideoPartVo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface VideoInfoFilePostRepository extends JpaRepository<VideoInfoFilePost, String> {

    List<VideoInfoFilePost> findByVideoIdAndUserId(String videoId, Long userId);

    void deleteAllByFileIdInAndUserId(Collection<String> fileIds, Long userId);

    long countByVideoIdAndTranscodeResult(String videoId, Integer transcodeResult);

    @Query("""
    select coalesce(sum(v.duration), 0) from VideoInfoFilePost v where v.videoId = :videoId
    """)
    int sumDuration(String videoId);

    VideoInfoFilePost findByFileId(String fileId);

    @Query("update VideoInfoFilePost v set v.updateType = :updateType where v.videoId = :videoId")
    @Modifying
    void updateUpdateTypeByVideoId(Integer updateType, String videoId);

    List<VideoInfoFilePost> findByVideoId(String videoId);

    @Query("""
    select new com.mylive.infra.jpa.entity.vo.VideoPartVo(
        v.fileId,
        v.title,
        v.transcodeResult,
        v.uploadId,
        null
    ) from VideoInfoFilePost v where v.videoId = :videoId order by v.fileIndex asc
    """)
    List<VideoPartVo> findVideoPartList(String videoId);

    void deleteAllByVideoId(String videoId);

    @Query("select v.filePath from VideoInfoFilePost v where v.videoId = :videoId")
    List<String> findFilePathByVideoId(String videoId);
}
