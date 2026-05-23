package com.mylive.infra.jpa.entity.vo;

import com.mylive.infra.jpa.entity.po.UserVideoSeries;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserVideoSeriesDetailVo implements Serializable {
    private UserVideoSeries videoSeries;
    private List<UserVideoSeriesVideoVo> seriesVideoList;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserVideoSeriesVideoVo implements Serializable {
        private Long seriesId;
        private String videoId;
        private String videoTitle;
        private String videoCover;
        private Integer playCount;
        private LocalDateTime createdAt;
    }
}
