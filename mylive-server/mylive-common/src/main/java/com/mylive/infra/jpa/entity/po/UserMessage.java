package com.mylive.infra.jpa.entity.po;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 用户消息表
 */
@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserMessage implements Serializable {

    /**
     * 消息ID自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer messageId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 主体ID
     */
    private String videoId;

    /**
     * 消息类型
     */
    private Integer messageType;

    /**
     * 发送人ID
     */
    private Long sendUserId;

    /**
     * 0:未读 1:已读
     */
    private Integer readType;

    /**
     * 创建时间
     */
    @CreatedDate
    private LocalDateTime createdAt;

    private Long commentId;

    /**
     * 扩展信息
     */
    private String extendJson;
}
