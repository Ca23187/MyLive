package com.mylive.infra.jpa.entity.po;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 视频信息
 */
@Setter
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class VideoInfoPost implements Serializable {

    /**
     * 视频ID
     */
    @Id
    private String videoId;

    /**
     * 视频封面
     */
    private String videoCover;

    /**
     * 视频名称
     */
    private String videoTitle;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    @LastModifiedDate
    private LocalDateTime lastUpdatedAt;

    /**
     * 父级分类ID
     */
    private Integer parentCategoryId;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 0:转码中 1转码失败 2:待审核 3:审核成功 4:审核失败
     */
    private Integer status;

    /**
     * 0:自制作  1:转载
     */
    private Integer postType;

    /**
     * 原资源说明
     */
    private String originInfo;

    /**
     * 标签
     */
    private String tags;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 互动设置
     */
    private Integer allowDanmaku;
    private Integer allowComment;

    /**
     * 持续时间（秒）
     */
    private Integer duration;
}
