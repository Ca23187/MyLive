package com.mylive.service.file.upload;

import com.mylive.config.AppProperties;
import com.mylive.constants.Constants;
import com.mylive.enums.DateTimePatternEnum;
import com.mylive.exception.BusinessException;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.file.storage.ObjectStorageService;
import com.mylive.utils.DateUtils;
import com.mylive.utils.StringTools;
import com.mylive.utils.ThumbnailTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final ObjectProvider<ObjectStorageService> objectStorageProvider;
    private final AppProperties appProperties;

    private ObjectStorageService oss() {  // 可选注入，切 minio 就启用
        return objectStorageProvider.getIfAvailable();
    }

    private boolean isMinioEnabled() {
        return oss() != null;
    }

    @Override
    public String uploadImage(MultipartFile file, Boolean createThumbnail) {
        if (file.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        String day = DateUtils.format(LocalDateTime.now(), DateTimePatternEnum.YYYYMMDD.getPattern());
        String fileName = file.getOriginalFilename();
        String fileSuffix = StringTools.getSuffix(fileName);
        String realFileName = StringTools.getRandomString(Constants.FILE_NAME_LENGTH) + fileSuffix;

        Path folder = Paths.get(
                appProperties.getProjectFolder(),
                Constants.FILE_FOLDER,
                Constants.FILE_COVER,
                day
        );

        try {
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }
        } catch (IOException e) {
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }

        Path sourceFilePath = folder.resolve(realFileName);
        Path thumbnailFilePath = folder.resolve(realFileName + Constants.THUMBNAIL_SUFFIX);

        try {
            // 1. 原图先落本地
            file.transferTo(sourceFilePath.toFile());

            // 2. 按需生成缩略图
            if (createThumbnail) {
                boolean created = ThumbnailTools.createThumbnail(
                        sourceFilePath,
                        Constants.THUMBNAIL_WIDTH,
                        thumbnailFilePath,
                        false
                );
                if (!created) {
                    Files.copy(sourceFilePath, thumbnailFilePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // 3. 如果是 MinIO，则上传并清理本地文件
            String objectKey = Constants.FILE_COVER + day + "/" + realFileName;
            String thumbnailKey = objectKey + Constants.THUMBNAIL_SUFFIX;

            if (isMinioEnabled()) {
                uploadCoverToMinioAndCleanup(
                        objectKey,
                        createThumbnail ? thumbnailKey : null,
                        sourceFilePath,
                        createThumbnail ? thumbnailFilePath : null
                );
            }

            return objectKey;
        } catch (Exception e) {
            throw (e instanceof BusinessException be) ? be : new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }

    private void uploadCoverToMinioAndCleanup(String objectKey,
                                              String thumbnailKey,
                                              Path sourceFile,
                                              Path thumbnailFile) {
        try {
            try (InputStream in = Files.newInputStream(sourceFile)) {
                oss().save(objectKey, in, Files.size(sourceFile), null);
            }

            if (thumbnailFile != null && Files.exists(thumbnailFile)) {
                try (InputStream in = Files.newInputStream(thumbnailFile)) {
                    oss().save(thumbnailKey, in, Files.size(thumbnailFile), Constants.THUMBNAIL_TYPE);
                }
            }
        } catch (Exception e) {
            try { oss().delete(objectKey); } catch (Exception ignored) {}
            if (thumbnailKey != null) {
                try { oss().delete(thumbnailKey); } catch (Exception ignored) {}
            }
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        } finally {
            try { Files.deleteIfExists(sourceFile); } catch (Exception ignored) {}
            if (thumbnailFile != null) {
                try { Files.deleteIfExists(thumbnailFile); } catch (Exception ignored) {}
            }
        }
    }
}
