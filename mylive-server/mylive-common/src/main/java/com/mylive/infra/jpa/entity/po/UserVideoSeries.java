package com.mylive.infra.jpa.entity.po;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 用户视频序列归档
 */
@Setter
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class UserVideoSeries implements Serializable {
    /**
     * 列表ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seriesId;

    /**
     * 列表名称
     */
    private String seriesName;

    /**
     * 描述
     */
    private String seriesDescription;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 排序
     */
    private Integer orderNum;

    /**
     * 更新时间
     */
    @CreatedDate
    private LocalDateTime updatedAt;

    private String cover;
}
