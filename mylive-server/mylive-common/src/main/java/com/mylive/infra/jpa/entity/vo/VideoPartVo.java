package com.mylive.infra.jpa.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VideoPartVo {
    private String fileId;
    private String title;
    private Integer transcodeResult;
    private String uploadId;
    private Integer duration;
}
