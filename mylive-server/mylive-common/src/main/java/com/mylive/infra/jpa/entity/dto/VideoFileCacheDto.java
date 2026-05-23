package com.mylive.infra.jpa.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VideoFileCacheDto implements Serializable {

    private String fileId;
    private String videoId;
    private Integer fileIndex;
    /**
     * 视频文件目录路径，不包含 index.m3u8 / ts 文件名
     */
    private String filePath;

    /**
     * 统计用
     */
    private Long videoUserId;
}