package com.mylive.infra.mapstruct;

import com.mylive.infra.jpa.entity.dto.request.VideoInfoPostDto;
import com.mylive.infra.jpa.entity.po.VideoInfo;
import com.mylive.infra.jpa.entity.po.VideoInfoPost;
import com.mylive.infra.jpa.entity.vo.PostedVideoEditVo;
import com.mylive.infra.jpa.entity.vo.VideoPartVo;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VideoInfoPostMapper {
    VideoInfoPost toVideoInfoPost(VideoInfoPostDto videoInfoPostDto);

    VideoInfo toVideoInfo(VideoInfoPost videoInfoPost);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDtoIgnoreNull(VideoInfoPostDto videoInfoPostDto, @MappingTarget VideoInfoPost entity);

    PostedVideoEditVo toEditVo(VideoInfoPost videoInfoPost, List<VideoPartVo> videoInfoFileList);
}