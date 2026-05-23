package com.mylive.infra.jpa.entity.po;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户行为 点赞、评论
 */
@Setter
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class UserAction implements Serializable {
    /**
     * 自增ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionId;

    /**
     * 视频ID
     */
    private String videoId;

    /**
     * 视频用户ID
     */
    private Long videoUserId;

    /**
     * 评论ID
     */
    private Long commentId;

    /**
     * 0:评论喜欢点赞 1:讨厌评论 2:视频点赞 3:视频收藏 4:视频投币
     */
    private Integer actionType;

    /**
     * 数量
     */
    private Integer actionCount;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 操作时间
     */
    @CreatedDate
    private LocalDateTime actionTime;
}
