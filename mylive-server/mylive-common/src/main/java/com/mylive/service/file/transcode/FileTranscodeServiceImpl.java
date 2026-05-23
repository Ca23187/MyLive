package com.mylive.service.file.transcode;

import com.mylive.config.AppProperties;
import com.mylive.constants.Constants;
import com.mylive.enums.VideoFileTranscodeResultEnum;
import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.dto.SysSettingDto;
import com.mylive.infra.jpa.entity.dto.TranscodeResult;
import com.mylive.infra.jpa.entity.dto.UploadingFileDto;
import com.mylive.infra.jpa.entity.po.VideoInfoFilePost;
import com.mylive.infra.jpa.repository.VideoInfoFilePostRepository;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.file.storage.ObjectStorageService;
import com.mylive.utils.FileTools;
import com.mylive.utils.VideoTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileTranscodeServiceImpl implements FileTranscodeService {

    private final ObjectProvider<ObjectStorageService> objectStorageProvider;
    private final AppProperties appProperties;
    private final RedisComponent redisComponent;
    private final VideoInfoFilePostRepository videoInfoFilePostRepository;
    private final FileTranscodeTxService fileTranscodeTxService;

    private ObjectStorageService oss() {  // 可选注入，切 minio 就启用
        return objectStorageProvider.getIfAvailable();
    }

    private boolean isMinioEnabled() {
        return oss() != null;
    }

    // NOTE: 拆事务：拆出来个 TX（Transaction）服务来专门支持事务
    // NOTE: 目的：避免耗时巨长的 doTranscode 被一个大事务包住导致数据库连接池被长期占用
    @Override
    public void transcodeVideoFile(String fileId) {
        VideoInfoFilePost post = videoInfoFilePostRepository.findByFileId(fileId);
        if (post == null) {
            throw new BusinessException(ResponseCodeEnum.NOT_FOUND);
        }

        // 防止消息队列重复消费
        if (Objects.equals(post.getTranscodeResult(), VideoFileTranscodeResultEnum.SUCCESS.getStatus())) {
            return;
        }

        try {
            TranscodeResult result = doTranscode(post); // 无事务（核心耗时）
            fileTranscodeTxService.updateSuccess(post.getFileId(), result);

        } catch (Exception e) {
            log.error("文件转码失败", e);
            fileTranscodeTxService.updateFail(post.getFileId());
        }
    }

    private TranscodeResult doTranscode(VideoInfoFilePost post) throws Exception {

        UploadingFileDto dto = redisComponent.getUploadingFileInfo(
                post.getUserId(), post.getUploadId());

        // 存储切片文件的临时目录
        Path tmpFolder = Paths.get(
                appProperties.getProjectFolder(),
                Constants.FILE_FOLDER,
                Constants.FILE_FOLDER_TEMP,
                dto.getFilePath()
        );

        Path targetFolder = Paths.get(
                appProperties.getProjectFolder(),
                Constants.FILE_FOLDER,
                Constants.FILE_VIDEO,
                dto.getFilePath()
        );
        if (!Files.exists(targetFolder)) {
            Files.createDirectories(targetFolder);
        }

        // 校验并发分片上传完整性和最终大小
        long totalSize = 0;
        for (int i = 0; i < dto.getChunks(); i++) {
            Path chunkFile = tmpFolder.resolve(String.valueOf(i));
            if (!Files.exists(chunkFile)) {
                throw new BusinessException("Video segments were not uploaded completely. Please upload it again.");
            }
            try {
                totalSize += Files.size(chunkFile);
            } catch (IOException e) {
                throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
            }
        }
        SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
        if (totalSize > sysSettingDto.getVideoSize() * Constants.MB) {
            throw new BusinessException("File size is too large");
        }
        if (totalSize != dto.getFileSize()) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        // 合并文件并拿长度
        Path videoPath = targetFolder.resolve(Constants.TEMP_VIDEO_NAME);
        FileTools.union(tmpFolder, videoPath, true);
        int duration = VideoTools.getVideoDuration(videoPath);
        long fileSize = Files.size(videoPath);

        // 转码
        if (!"h264".equals(VideoTools.getVideoCodec(videoPath))) {
            VideoTools.transcodeToMp4Replace(videoPath);
        }

        // 切片
        VideoTools.cutVideo(videoPath, true);

        // 上传 MinIO
        if (isMinioEnabled()) {
            String objectKey = Constants.FILE_VIDEO + dto.getFilePath();
            uploadVideoProductsToMinioAndCleanUp(objectKey);
        }

        return new TranscodeResult(
                duration,
                fileSize,
                Constants.FILE_VIDEO + dto.getFilePath()
        );
    }

    private void uploadVideoProductsToMinioAndCleanUp(String objectKey) {
        Path productFolder = Paths.get(
                appProperties.getProjectFolder(),
                Constants.FILE_FOLDER,
                objectKey
        );
        List<String> uploadedKeys = new ArrayList<>();
        try {
            // 上传当前视频目录下的所有产物：m3u8 / ts 等
            try (Stream<Path> stream = Files.walk(productFolder)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .toList();

                for (Path file : files) {
                    Path relativePath = productFolder.relativize(file);

                    String fileObjectKey = Paths.get(objectKey)
                            .resolve(relativePath)
                            .toString()
                            .replace("\\", "/");

                    try (InputStream in = Files.newInputStream(file)) {
                        oss().save(fileObjectKey, in, Files.size(file), null);
                    }

                    uploadedKeys.add(fileObjectKey);
                }
            }

            // 上传全部成功后，再删除本地目录
            FileUtils.deleteDirectory(productFolder.toFile());

        } catch (Exception e) {
            // MinIO 上传失败，回滚已经上传的文件
            for (String key : uploadedKeys) {
                try {
                    oss().delete(key);
                } catch (Exception ignored) {}
            }
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }
}
