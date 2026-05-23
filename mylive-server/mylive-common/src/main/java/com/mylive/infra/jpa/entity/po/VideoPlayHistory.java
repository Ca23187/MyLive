package com.mylive.infra.jpa.entity.po;

import com.mylive.infra.jpa.entity.po.id.VideoPlayHistoryId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 视频播放历史
 */
@Entity
@Getter
@Setter
@IdClass(VideoPlayHistoryId.class)
public class VideoPlayHistory implements Serializable {

    @Id
    private Long userId;

    @Id
    private String videoId;

    private String fileId;

    private Integer progress;

    private Integer duration;

    private Integer finished;

    private LocalDateTime lastPlayedAt;

    private String videoCover;

    private String videoTitle;

    private String fileTitle;
}
