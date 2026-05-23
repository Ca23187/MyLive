package com.mylive.infra.jpa.entity.vo;

import com.mylive.infra.jpa.entity.po.UserAction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class VideoCommentVo implements Serializable {
    private PaginationResultVo<VideoCommentVoItem> commentData;
    private List<UserAction> userActionList;
}
