package com.mylive.infra.jpa.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FollowOrFanListVo implements Serializable {
    private Long otherUserId;
    private String otherNickname;
    private String otherAvatar;
    private String otherProfile;
    private Integer followType;
}
