package com.mylive.infra.jpa.entity.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class UserInfoVo implements Serializable {
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer gender;
    private String profile;
    private String noticeInfo;
    private Integer level;
    private String birthday;
    private String school;
    private Integer fanCount;
    private Integer followCount;
    private Boolean haveFollowed;
    private Integer theme;
}
