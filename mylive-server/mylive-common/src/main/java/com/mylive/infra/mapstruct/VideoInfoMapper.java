package com.mylive.infra.mapstruct;

import com.mylive.infra.jpa.entity.dto.es.VideoInfoEsDto;
import com.mylive.infra.jpa.entity.po.UserAction;
import com.mylive.infra.jpa.entity.po.VideoInfo;
import com.mylive.infra.jpa.entity.po.VideoInfoPost;
import com.mylive.infra.jpa.entity.vo.VideoDetailVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VideoInfoMapper {
    VideoDetailVo toDetailVo(VideoInfo videoInfo, List<UserAction> userActionList);

    VideoDetailVo.VideoInfoVo toVideoInfoVo(VideoInfo videoInfo);

    void updateFromPost(VideoInfoPost videoInfoPost, @MappingTarget VideoInfo videoInfo);

    @Mapping(target = "tags", expression = "java(com.mylive.utils.StringTools.parseCommaDistinctList(videoInfo.getTags()))")
    @Mapping(target = "createdAt", expression = "java(com.mylive.utils.DateUtils.toDate(videoInfo.getCreatedAt()))")
    VideoInfoEsDto toEsDto(VideoInfo videoInfo);
}