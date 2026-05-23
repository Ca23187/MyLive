package com.mylive.infra.jpa.entity.dto.request;

import com.mylive.infra.jpa.entity.po.VideoInfoFilePost;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VideoInfoPostDto {

    private String videoId;

    @NotBlank
    private String videoCover;

    @NotBlank
    @Size(max = 100)
    private String videoTitle;

    @NotNull
    private Integer parentCategoryId;

    private Integer categoryId;

    @NotNull
    private Integer postType;

    @NotBlank
    @Size(max = 300)
    private String tags;

    @Size(max = 2000)
    private String introduction;

    @NotNull
    private Integer allowDanmaku;

    @NotNull
    private Integer allowComment;

    @NotEmpty
    private List<VideoInfoFilePost> fileInfoList;

    private String originInfo;
}