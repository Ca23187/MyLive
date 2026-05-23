package com.mylive.service.video;

import com.mylive.infra.jpa.entity.po.VideoComment;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.VideoCommentVo;
import com.mylive.infra.jpa.entity.vo.VideoCommentVoItem;

public interface VideoCommentService {
    VideoCommentVoItem postComment(VideoComment comment, Long replyCommentId, String mentionUserIds, Long videoUserId);

    VideoCommentVo getComments(String videoId, Integer pageNo, Integer orderType, String token);

    VideoCommentVo getReplyList(Long parentId, Integer pageNo, String token);

    void topComment(Long commentId, Long userId);

    void cancelTopComment(Long commentId, Long userId);

    void deleteComment(Long commentId, Long userId);

    PaginationResultVo<VideoCommentVoItem> getUCenterCommentPage(Integer pageNo, String videoId, Long userId);

    PaginationResultVo<VideoCommentVoItem> getAdminCommentPage(Integer pageNo, String videoTitleFuzzy);
}
