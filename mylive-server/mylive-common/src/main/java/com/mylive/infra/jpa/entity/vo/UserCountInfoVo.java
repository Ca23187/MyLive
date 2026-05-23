package com.mylive.infra.jpa.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserCountInfoVo {
    private Integer followCount;
    private Integer fanCount;
    private Integer currentCoinCount;
}