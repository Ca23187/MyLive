package com.mylive.infra.mapstruct;

import com.mylive.infra.jpa.entity.po.VideoInfoFile;
import com.mylive.infra.jpa.entity.po.VideoInfoFilePost;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VideoInfoFilePostMapper {
    List<VideoInfoFile> toVideoInfoFileList(List<VideoInfoFilePost> videoInfoFilePostList);
}