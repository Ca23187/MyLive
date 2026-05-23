package com.mylive.admin.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.infra.jpa.entity.dto.FileReadResourceDto;
import com.mylive.infra.jpa.entity.po.VideoInfoFilePost;
import com.mylive.response.ResponseVo;
import com.mylive.service.file.access.FileAccessService;
import com.mylive.service.file.upload.FileUploadService;
import com.mylive.service.video.post.AdminVideoInfoPostService;
import com.mylive.utils.ServletNetUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@RequiresLogin
public class FileController {

    private final FileUploadService fileUploadService;
    private final FileAccessService fileAccessService;
    private final AdminVideoInfoPostService adminVideoInfoPostService;

    @PostMapping("/uploadImage")
    public ResponseVo<String> uploadCover(@NotNull MultipartFile file, @NotNull Boolean createThumbnail) {
        return ResponseVo.ok(fileUploadService.uploadImage(file, createThumbnail));
    }

    @GetMapping("/getResource")
    public void getResource(HttpServletResponse response, @NotBlank String sourceName) {
        FileReadResourceDto resource = fileAccessService.openImageForRead(sourceName);
        ServletNetUtils.writeResource(response, resource);
        response.setHeader("Cache-Control", "max-age=2592000");
    }

    @GetMapping("/videoResource/{fileId}/")
    public void getVideoResource(HttpServletResponse response, @PathVariable @NotBlank String fileId) {
        VideoInfoFilePost videoInfoFilePost = adminVideoInfoPostService.getFilePostByFileId(fileId);
        FileReadResourceDto resource = fileAccessService.openM3U8ForRead(videoInfoFilePost.getFilePath());
        ServletNetUtils.writeResource(response, resource);
    }

    @GetMapping("/videoResource/{fileId}/{ts}")
    public void getVideoResourceTs(HttpServletResponse response, @PathVariable @NotBlank String fileId, @PathVariable @NotNull String ts) {
        VideoInfoFilePost videoInfoFilePost = adminVideoInfoPostService.getFilePostByFileId(fileId);
        FileReadResourceDto resource = fileAccessService.openTsForRead(videoInfoFilePost.getFilePath(), ts);
        ServletNetUtils.writeResource(response, resource);
    }
}
