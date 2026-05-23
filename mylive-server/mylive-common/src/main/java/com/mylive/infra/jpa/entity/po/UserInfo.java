package com.mylive.infra.jpa.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 用户信息
 */
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class UserInfo implements Serializable {

    /**
     * 用户ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 密码
     */
    @JsonIgnore
    private String password;

    /**
     * 0:女 1:男 2:保密
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private String birthday;

    /**
     * 学校
     */
    private String school;

    /**
     * 个人简介
     */
    private String profile;

    /**
     * 加入时间
     */
    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * 最后登录时间
     */
    @LastModifiedDate
    private LocalDateTime lastLoginAt;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 0:禁用 1:正常
     */
    private Integer status;

    /**
     * 空间公告
     */
    private String noticeInfo;

    /**
     * 硬币总数量
     */
    private Integer totalCoinCount;

    /**
     * 当前硬币数
     */
    private Integer currentCoinCount;

    /**
     * 主题
     */
    private Integer theme;
}
