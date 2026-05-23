package com.mylive.web.controller;

import com.mylive.annotation.RequiresLogin;
import com.mylive.config.AppProperties;
import com.mylive.constants.Constants;
import com.mylive.enums.DateTimePatternEnum;
import com.mylive.enums.StatisticTypeEnum;
import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.dto.*;
import com.mylive.infra.jpa.entity.po.VideoInfoFile;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.response.ResponseVo;
import com.mylive.service.file.access.FileAccessService;
import com.mylive.service.file.upload.FileUploadService;
import com.mylive.service.video.VideoInfoService;
import com.mylive.utils.DateUtils;
import com.mylive.utils.ServletNetUtils;
import com.mylive.utils.StringTools;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileController {

    private final FileAccessService fileAccessService;
    private final RedisComponent redisComponent;
    private final AppProperties appProperties;
    private final FileUploadService fileUploadService;
    private final VideoInfoService videoInfoService;

    @GetMapping("/getResource")
    public void getResource(HttpServletResponse response, @NotBlank String sourceName) {
        FileReadResourceDto resource = fileAccessService.openImageForRead(sourceName);
        response.setHeader("Cache-Control", "max-age=2592000");
        ServletNetUtils.writeResource(response, resource);
    }

    @PostMapping("/preUploadVideo")
    @RequiresLogin
    public ResponseVo<String> preUploadVideo(@NotNull @Min(1) Integer chunks,
                                             @NotNull @Min(1) Long fileSize,
                                             @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
        if (fileSize > sysSettingDto.getVideoSize() * Constants.MB) {
            throw new BusinessException("File size is too large");
        }
        String uploadId = StringTools.getRandomString(Constants.VIDEO_UPLOAD_ID_LENGTH);
        UploadingFileDto dto = new UploadingFileDto();
        dto.setChunks(chunks);
        dto.setUploadId(uploadId);
        dto.setFileSize(fileSize);

        String day = DateUtils.format(LocalDateTime.now(), DateTimePatternEnum.YYYYMMDD.getPattern());
        String filePath = day + "/" + tokenInfo.getUserId() + uploadId;
        Path tmpFolder = Paths.get(
                appProperties.getProjectFolder(),
                Constants.FILE_FOLDER,
                Constants.FILE_FOLDER_TEMP,
                filePath);
        try {
            if (Files.exists(tmpFolder)) {
                FileUtils.deleteDirectory(tmpFolder.toFile());
            }
            Files.createDirectories(tmpFolder);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create directories: " + tmpFolder.toAbsolutePath(), e);
        }
        dto.setFilePath(filePath);
        redisComponent.savePreUploadFileInfo(tokenInfo.getUserId(), uploadId, dto);
        return ResponseVo.ok(uploadId);
    }

    @PostMapping("/uploadVideo")
    @RequiresLogin
    public ResponseVo<Void> uploadVideo(@NotNull MultipartFile chunkFile,
                                        @NotNull Integer chunkIndex,
                                        @NotBlank String uploadId,
                                        @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        UploadingFileDto dto = redisComponent.getUploadingFileInfo(tokenInfo.getUserId(), uploadId);
        if (dto == null) {
            throw new BusinessException("File does not exist, please upload again");
        }
        // 判断分片
        if (chunkIndex < 0 || chunkIndex >= dto.getChunks()) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        Path folder = Paths.get(appProperties.getProjectFolder(),
                Constants.FILE_FOLDER,
                Constants.FILE_FOLDER_TEMP,
                dto.getFilePath()
        );
        try {
            Path targetFile = folder.resolve(String.valueOf(chunkIndex));
            chunkFile.transferTo(targetFile);
        } catch (IOException e) {
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }
        return ResponseVo.ok();
    }

    @PostMapping("/delUploadVideo")
    @RequiresLogin
    public ResponseVo<String> delUploadVideo(@NotBlank String uploadId,
                                             @CookieValue(name = Constants.TOKEN_WEB) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        UploadingFileDto dto = redisComponent.getUploadingFileInfo(tokenInfo.getUserId(), uploadId);
        if (dto == null) {
            throw new BusinessException("File does not exist");
        }
        redisComponent.delUploadedFileInfo(tokenInfo.getUserId(), uploadId);
        try {
            FileUtils.deleteDirectory(
                    Paths.get(
                            appProperties.getProjectFolder(),
                            Constants.FILE_FOLDER,
                            Constants.FILE_FOLDER_TEMP,
                            dto.getFilePath()
                    ).toFile());
        } catch (IOException e) {
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }
        return ResponseVo.ok(uploadId);
    }

    @PostMapping("/uploadImage")
    @RequiresLogin
    public ResponseVo<String> uploadCover(@NotNull MultipartFile file, @NotNull Boolean createThumbnail) {
        return ResponseVo.ok(fileUploadService.uploadImage(file, createThumbnail));
    }

    @GetMapping("/videoResource/{fileId}/")
    public void getVideoResource(HttpServletResponse response,
                                 @PathVariable @NotBlank String fileId) {
        VideoFileCacheDto dto = getVideoFileByFileId(fileId);
        redisComponent.incrVideoPlayCount(dto.getVideoId());
        redisComponent.updateStatisticCount(dto.getVideoUserId(), dto.getVideoId(), 1, StatisticTypeEnum.PLAY);
        FileReadResourceDto resource = fileAccessService.openM3U8ForRead(dto.getFilePath());
        ServletNetUtils.writeResource(response, resource);
    }

    @GetMapping("/videoResource/{fileId}/{ts}")
    public void getVideoResourceTs(HttpServletResponse response, @PathVariable @NotBlank String fileId, @PathVariable @NotNull String ts) {
        VideoFileCacheDto dto = getVideoFileByFileId(fileId);
        FileReadResourceDto resource = fileAccessService.openTsForRead(dto.getFilePath(), ts);
        ServletNetUtils.writeResource(response, resource);
    }

    private VideoFileCacheDto getVideoFileByFileId(String fileId) {
        VideoFileCacheDto cache = redisComponent.getVideoFileCache(fileId);
        if (cache != null) {
            return cache;
        }

        if (redisComponent.isVideoFileNegCached(fileId)) {
            throw new BusinessException(ResponseCodeEnum.NOT_FOUND);
        }

        VideoInfoFile db = videoInfoService.getVideoInfoFileByFileId(fileId);
        if (db == null) {
            redisComponent.saveVideoFileNegCache(fileId);
            throw new BusinessException(ResponseCodeEnum.NOT_FOUND);
        }

        VideoFileCacheDto dto = new VideoFileCacheDto();
        dto.setFileId(db.getFileId());
        dto.setVideoId(db.getVideoId());
        dto.setFileIndex(db.getFileIndex());
        dto.setFilePath(db.getFilePath());
        dto.setVideoUserId(db.getUserId());

        redisComponent.saveVideoFileCache(dto);
        return dto;
    }
}
