package com.mylive.infra.jpa.entity.vo;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PortalVideoInfoVo implements Serializable {
    private String videoId;
    private String videoTitle;
    private String videoCover;
    private Integer duration;
    private LocalDateTime createdAt;
    private Integer playCount;
    private Integer danmakuCount;
    private String nickname;
}