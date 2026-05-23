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
public class UCenterDanmakuVo implements Serializable {
    private Long danmakuId;
    private String text;
    private Integer time;
    private LocalDateTime postedAt;
    private String videoId;
    private Long videoUserId;
    private String videoTitle;
    private String videoCover;
}
