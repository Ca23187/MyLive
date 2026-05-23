package com.mylive.infra.jpa.entity.po;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频弹幕
 */
@Setter
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class VideoDanmaku implements Serializable {

    /**
     * 自增ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long danmakuId;

    /**
     * 视频ID
     */
    private String videoId;

    /**
     * 唯一ID
     */
    private String fileId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 发布时间
     */
    @CreatedDate
    private LocalDateTime postedAt;

    /**
     * 内容
     */
    private String text;

    /**
     * 展示位置
     */
    private Integer mode;

    /**
     * 颜色
     */
    private String color;

    /**
     * 展示时间
     */
    private Integer time;

    private Long videoUserId;
}
