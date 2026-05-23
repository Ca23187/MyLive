package com.mylive.infra.jpa.entity.po;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 评论
 */
@Setter
@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
public class VideoComment implements Serializable {
    /**
     * 评论ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    /**
     * 父级评论ID
     */
    private Long parentCommentId;

    /**
     * 视频ID
     */
    private String videoId;

    /**
     * 视频用户ID
     */
    private Long videoUserId;

    /**
     * 回复内容
     */
    private String content;

    /**
     * 图片
     */
    private String imgPath;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 回复人ID
     */
    private Long replyUserId;

    /**
     * 0:未置顶  1:置顶
     */
    private Integer topType;

    /**
     * 发布时间
     */
    @CreatedDate
    private LocalDateTime postedAt;

    /**
     * 喜欢数量
     */
    private Integer likeCount;

    /**
     * 讨厌数量
     */
    private Integer dislikeCount;

    private String nickname;

    private String avatar;

    private String replyNickname;

    private Integer replyCount;

    private String mentionJson;
}
