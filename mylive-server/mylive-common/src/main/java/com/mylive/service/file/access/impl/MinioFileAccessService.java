package com.mylive.service.file.access.impl;

import com.mylive.constants.Constants;
import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.dto.FileReadResourceDto;
import com.mylive.infra.jpa.entity.dto.ObjMeta;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.file.access.FileAccessService;
import com.mylive.service.file.storage.ObjectStorageService;
import com.mylive.utils.ContentTypeGuesser;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "storage",
        name = "type",
        havingValue = "minio"
)
public class MinioFileAccessService implements FileAccessService {

    private final ObjectStorageService storageService;

    @Override
    public FileReadResourceDto openImageForRead(String objectKey) {
        return openObject(objectKey);
    }

    @Override
    public FileReadResourceDto openM3U8ForRead(String objectKey) {
        return openObject(objectKey + "/" + Constants.M3U8_NAME);
    }

    @Override
    public FileReadResourceDto openTsForRead(String objectKey, String ts) {
        return openObject(objectKey + "/" + ts);
    }

    private FileReadResourceDto openObject(String objectKey) {
        ObjMeta meta = storageService.statIfExists(objectKey);
        if (meta == null) {
            throw new BusinessException(ResponseCodeEnum.NOT_FOUND);
        }
        FileReadResourceDto res = new FileReadResourceDto();
        res.setContentLength(meta.getSize());
        String ct = meta.getContentType();
        res.setContentType(StringUtils.hasText(ct) ? ct : ContentTypeGuesser.guess(objectKey));
        res.setOpenStream(() -> storageService.get(objectKey));
        return res;
    }
}