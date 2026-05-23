package com.mylive.service.file.upload;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {
    String uploadImage(MultipartFile file, Boolean createThumbnail);
}
