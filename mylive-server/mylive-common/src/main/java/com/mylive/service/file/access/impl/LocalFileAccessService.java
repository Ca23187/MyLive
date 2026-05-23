package com.mylive.service.file.access.impl;

import com.mylive.config.AppProperties;
import com.mylive.constants.Constants;
import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.dto.FileReadResourceDto;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.file.access.FileAccessService;
import com.mylive.utils.ContentTypeGuesser;
import com.mylive.utils.StringTools;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "storage",
        name = "type",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalFileAccessService implements FileAccessService {

    private final AppProperties appProperties;

    @Override
    public FileReadResourceDto openImageForRead(String relativePath) {
        if (!StringTools.isRelPathOk(relativePath)) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        return openLocalPath(resolveSafePath(relativePath), null, null);
    }

    @Override
    public FileReadResourceDto openM3U8ForRead(String relativePath) {
        Path path = resolveSafePath(relativePath).resolve(Constants.M3U8_NAME);
        return openLocalPath(path, null, "application/vnd.apple.mpegurl");
    }

    @Override
    public FileReadResourceDto openTsForRead(String relativePath, String ts) {
        Path path = resolveSafePath(relativePath).resolve(ts);
        return openLocalPath(path, null, "video/mp2t");
    }

    private Path resolveSafePath(String relativePath) {
        if (!StringTools.isRelPathOk(relativePath)) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        Path root = Paths.get(appProperties.getProjectFolder(), Constants.FILE_FOLDER)
                .toAbsolutePath()
                .normalize();

        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        return path;
    }

    private FileReadResourceDto openLocalPath(Path path, Long size, String contentType) {
        try {
            if (!Files.exists(path) || Files.isDirectory(path)) throw new BusinessException(ResponseCodeEnum.NOT_FOUND);
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);

            FileReadResourceDto res = new FileReadResourceDto();
            res.setContentLength(size == null ? Files.size(path) : size);
            res.setContentType(StringUtils.hasText(contentType)
                    ? contentType
                    : ContentTypeGuesser.guess(path.toString()));
            res.setOpenStream(() -> {
                try {
                    return Files.newInputStream(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            return res;
        } catch (IOException e) {
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }
}
