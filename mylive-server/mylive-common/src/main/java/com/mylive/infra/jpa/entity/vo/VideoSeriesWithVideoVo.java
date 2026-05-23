package com.mylive.infra.jpa.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class VideoSeriesWithVideoVo implements Serializable {
    private Long seriesId;
    private String seriesName;
    private List<UHomeVideoInfoVo> videoInfoList;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class FlatDto {

        private Long seriesId;
        private String seriesName;

        private String videoId;
        private String videoCover;
        private String videoTitle;
        private Integer playCount;
        private LocalDateTime createdAt;
    }
}
