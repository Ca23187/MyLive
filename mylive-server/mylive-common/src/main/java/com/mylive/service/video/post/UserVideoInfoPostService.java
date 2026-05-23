package com.mylive.service.video.post;

import com.mylive.infra.jpa.entity.dto.request.VideoInfoPostDto;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.PostedVideoEditVo;
import com.mylive.infra.jpa.entity.vo.VideoInfoPostVo;
import com.mylive.infra.jpa.entity.vo.VideoStatusCountVo;

public interface UserVideoInfoPostService {
    void saveVideoInfoPost(Long userId, VideoInfoPostDto submittedFileList);

    PaginationResultVo<VideoInfoPostVo> findPostPage4User(Integer status, Integer pageNo, String videoTitleFuzzy, Long userId);

    VideoStatusCountVo getStatusCount(Long userId);

    PostedVideoEditVo getEditVideoPost(Long userId, String videoId);
}
