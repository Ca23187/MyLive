package com.mylive.infra.jpa.entity.dto.es;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class VideoInfoEsDto implements Serializable {
    private String videoId;
    private String videoCover;
    private String videoTitle;
    private Date createdAt;
    private List<String> tags;

    private String userId;

    private Integer playCount;
    private Integer danmakuCount;
    private Integer saveCount;
    private Integer duration;
}
