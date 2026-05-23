package com.mylive.infra.jpa.entity.vo;

import com.mylive.enums.VideoStatusEnum;
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
public class VideoInfoPostVo implements Serializable {

    // VideoInfoPost 表字段
    private String videoId;
    private String videoCover;
    private String videoTitle;
    private Long userId;
    private Integer status;
    private LocalDateTime createdAt;
    private Integer duration;
    private LocalDateTime lastUpdatedAt;

    // VideoInfo 表字段
    private Integer playCount;
    private Integer likeCount;
    private Integer danmakuCount;
    private Integer commentCount;
    private Integer coinCount;
    private Integer saveCount;
    private Integer recommendType;

    // UserInfo 表字段
    private String nickname;
    private String avatar;

    // 转 JSON 时自动调用赋值
    public String getStatusName() {
        VideoStatusEnum statusEnum = VideoStatusEnum.getByStatus(this.status);
        return statusEnum == null ? null : statusEnum.getDesc();
    }
}