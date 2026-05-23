package com.mylive.service.video.post;

import com.mylive.infra.jpa.entity.dto.request.AdminVideoInfoPostQuery;
import com.mylive.infra.jpa.entity.po.VideoInfoFilePost;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.VideoInfoPostVo;
import com.mylive.infra.jpa.entity.vo.VideoPartVo;

import java.util.List;

public interface AdminVideoInfoPostService {
    PaginationResultVo<VideoInfoPostVo> findPostPage4Admin(AdminVideoInfoPostQuery query);

    void reviewVideo(String videoId, Long userId, Integer status, String reason);

    void recommendVideo(String videoId);

    VideoInfoFilePost getFilePostByFileId(String fileId);

    List<VideoPartVo> getVideoPartList(String videoId);
}
