package com.mylive.infra.jpa.entity.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.util.function.Supplier;

@Getter
@Setter
public final class FileReadResourceDto {
    /** 全量长度（必须） */
    private long contentLength;

    /** Content-Type（可空，最终会兜底 application/octet-stream） */
    private String contentType;

    /**
     * 全量读取：需要时再打开一个新的 InputStream（只能读一次，所以必须是 Supplier）
     */
    private Supplier<InputStream> openStream;
}