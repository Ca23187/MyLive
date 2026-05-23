package com.mylive.infra.jpa.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostedVideoEditVo implements Serializable {
    private String videoId;
    private String videoTitle;
    private String videoCover;
    private String introduction;
    private Integer postType;
    private String originInfo;
    private String tags;
    private Integer parentCategoryId;
    private Integer categoryId;
    private Integer allowDanmaku;
    private Integer allowComment;

    private List<VideoPartVo> videoInfoFileList;
}
