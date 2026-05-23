package com.mylive.infra.jpa.entity.po;

import com.mylive.infra.jpa.entity.po.id.UserFollowId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 粉丝，关注表
 */
@Entity
@Setter
@Getter
@IdClass(UserFollowId.class)
@EntityListeners(AuditingEntityListener.class)
public class UserFollow implements Serializable {


    /**
     * 用户ID
     */
    @Id
    private Long userId;

    /**
     * 关注用户ID
     */
    @Id
    private Long followUserId;

    /**
     * 关注时间
     */
    @CreatedDate
    private LocalDateTime followAt;
}
