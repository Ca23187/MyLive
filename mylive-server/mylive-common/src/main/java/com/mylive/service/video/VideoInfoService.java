package com.mylive.service.video;

import com.mylive.infra.jpa.entity.po.VideoInfoFile;
import com.mylive.infra.jpa.entity.vo.*;

import java.util.List;

public interface VideoInfoService {
    List<BasicVideoInfoVo> getRecommendVideoList();

    PaginationResultVo<PortalVideoInfoVo> getPortalVideoPage(Integer parentCategoryId, Integer categoryId, Integer pageNo);

    VideoDetailVo getVideoDetail(String videoId, String token);

    List<VideoPartVo> getVideoPartList(String videoId);

    VideoInfoFile getVideoInfoFileByFileId(String fileId);

    PaginationResultVo<UHomeVideoInfoVo> getUHomeVideoPage(Long userId, Integer type, Integer pageNo, String videoTitle, Integer orderType);

    PaginationResultVo<UHomeVideoInfoVo> getUHomeSavedVideoPage(Long userId, Integer pageNo);

    List<UHomeVideoInfoVo> getUHomeVideoList(Long seriesId, Long userId);

    void changeInteraction(String videoId, Long userId, Integer allowDanmaku, Integer allowComment);

    void deleteVideo(String videoId, Long userId, String reason);

    List<BasicVideoInfoVo> getBasicVideoInfoVo(Long userId);

    PaginationResultVo<PortalVideoInfoVo> getHotVideoList(Integer pageNo);
}
