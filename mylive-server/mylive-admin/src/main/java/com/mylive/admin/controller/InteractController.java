package com.mylive.admin.controller;

import com.mylive.infra.jpa.entity.vo.AdminDanmakuVo;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.VideoCommentVoItem;
import com.mylive.response.ResponseVo;
import com.mylive.service.video.VideoCommentService;
import com.mylive.service.video.VideoDanmakuService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/interact")
@RequiredArgsConstructor
public class InteractController {
    private final VideoCommentService videoCommentService;
    private final VideoDanmakuService videoDanmakuService;


    @PostMapping("/loadDanmaku")
    public ResponseVo<PaginationResultVo<AdminDanmakuVo>> loadDanmaku(Integer pageNo, String videoTitleFuzzy) {
        return ResponseVo.ok(videoDanmakuService.getAdminDanmakuPage(pageNo, videoTitleFuzzy));
    }


    @PostMapping("/delDanmaku")
    public ResponseVo<Void> delDanmaku(@NotNull Long danmakuId) {
        videoDanmakuService.deleteDanmaku(null, danmakuId);
        return ResponseVo.ok();
    }

    @PostMapping("/loadComment")
    public ResponseVo<PaginationResultVo<VideoCommentVoItem>> loadComment(Integer pageNo, String videoTitleFuzzy) {
        return ResponseVo.ok(videoCommentService.getAdminCommentPage(pageNo, videoTitleFuzzy));
    }

    @PostMapping("/delComment")
    public ResponseVo<Void> delComment(@NotNull Long commentId) {
        videoCommentService.deleteComment(commentId, null);
        return ResponseVo.ok();
    }
}
