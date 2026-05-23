package com.mylive.infra.jpa.entity.vo;

import com.mylive.infra.jpa.entity.po.UserAction;
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
@AllArgsConstructor
public class VideoDetailVo implements Serializable {
    private VideoInfoVo videoInfo;
    private List<UserAction> userActionList;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VideoInfoVo implements Serializable {
        private String videoId;
        private String videoTitle;
        private Long userId;
        private LocalDateTime createdAt;
        private Integer postType;
        private String originInfo;
        private String tags;
        private String introduction;
        private Integer allowDanmaku;
        private Integer allowComment;
        private Integer playCount;
        private Integer likeCount;
        private Integer danmakuCount;
        private Integer coinCount;
        private Integer saveCount;
    }
}
