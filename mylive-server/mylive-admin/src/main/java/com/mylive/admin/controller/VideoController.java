package com.mylive.admin.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.infra.jpa.entity.dto.request.AdminVideoInfoPostQuery;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.VideoInfoPostVo;
import com.mylive.infra.jpa.entity.vo.VideoPartVo;
import com.mylive.response.ResponseVo;
import com.mylive.service.video.VideoInfoService;
import com.mylive.service.video.post.AdminVideoInfoPostService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/videoInfo")
@RequiresLogin
@AllArgsConstructor
public class VideoController {

    private final AdminVideoInfoPostService adminVideoInfoPostService;
    private final VideoInfoService videoInfoService;

    @PostMapping("/loadVideoPostList")
    public ResponseVo<PaginationResultVo<VideoInfoPostVo>> loadVideoPostList(AdminVideoInfoPostQuery query) {
        return ResponseVo.ok(adminVideoInfoPostService.findPostPage4Admin(query));
    }

    @RequestMapping("/recommendVideo")
    public ResponseVo<Void> recommendVideo(@NotBlank String videoId) {
        adminVideoInfoPostService.recommendVideo(videoId);
        return ResponseVo.ok();
    }

    @PostMapping("/reviewVideo")
    public ResponseVo<Void> reviewVideo(@NotBlank String videoId, @NotNull Long userId, @NotNull Integer status, String reason) {
        adminVideoInfoPostService.reviewVideo(videoId, userId, status, reason);
        return ResponseVo.ok();
    }

    @PostMapping("/deleteVideo")
    public ResponseVo<Void> deleteVideo(@NotBlank String videoId, @NotBlank String reason) {
        videoInfoService.deleteVideo(videoId, null, reason.trim());
        return ResponseVo.ok();
    }

    @PostMapping("/loadVideoPartList")
    public ResponseVo<List<VideoPartVo>> loadVideoPartList(@NotBlank String videoId) {
        return ResponseVo.ok(adminVideoInfoPostService.getVideoPartList(videoId));
    }
}
