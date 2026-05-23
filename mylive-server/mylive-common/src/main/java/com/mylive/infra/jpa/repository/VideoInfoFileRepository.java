package com.mylive.infra.jpa.repository;

import com.mylive.infra.jpa.entity.po.VideoInfoFile;
import com.mylive.infra.jpa.entity.vo.VideoPartVo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VideoInfoFileRepository extends JpaRepository<VideoInfoFile, String> {

    void deleteByVideoId(String videoId);

    @Query("""
    select new com.mylive.infra.jpa.entity.vo.VideoPartVo(
        v.fileId,
        v.title,
        null,
        null,
        v.duration
    ) from VideoInfoFile v where v.videoId = :videoId order by v.fileIndex asc
    """)
    List<VideoPartVo> findVideoPartList(String videoId);

    void deleteAllByVideoId(String videoId);

    VideoInfoFile findByFileIdAndVideoId(String fileId, String videoId);
}
