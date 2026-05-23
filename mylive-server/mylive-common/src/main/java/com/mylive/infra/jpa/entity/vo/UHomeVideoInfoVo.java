package com.mylive.infra.jpa.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UHomeVideoInfoVo implements Serializable {
    private String videoId;
    private String videoTitle;
    private String videoCover;
    private Integer playCount;
    private LocalDateTime createdAt;
    private LocalDateTime actionTime;
}
