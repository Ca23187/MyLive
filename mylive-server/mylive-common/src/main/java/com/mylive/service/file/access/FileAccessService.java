package com.mylive.service.file.access;

import com.mylive.infra.jpa.entity.dto.FileReadResourceDto;

public interface FileAccessService {

    FileReadResourceDto openImageForRead(String relativePath);

    FileReadResourceDto openM3U8ForRead(String relativePath);

    FileReadResourceDto openTsForRead(String relativePath, String ts);
}
