package com.mylive.infra.mapstruct;

import com.mylive.infra.jpa.entity.po.VideoComment;
import com.mylive.infra.jpa.entity.vo.VideoCommentVoItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VideoCommentMapper {
    @Mapping(target = "replyCount", defaultValue = "0")
    @Mapping(target = "likeCount", defaultValue = "0")
    @Mapping(target = "dislikeCount", defaultValue = "0")
    VideoCommentVoItem toItemVo(VideoComment videoComment);
}