package com.mylive.infra.jpa.entity.po;

import com.mylive.infra.jpa.entity.po.id.UserVideoSeriesVideoId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@Entity
@IdClass(UserVideoSeriesVideoId.class)
public class UserVideoSeriesVideo implements Serializable {

    /**
     * 列表ID
     */
    @Id
    private Long seriesId;

    /**
     * 视频ID
     */
    @Id
    private String videoId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 排序
     */
    private Integer orderNum;

}
