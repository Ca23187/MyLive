package com.mylive.infra.mapstruct;

import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.po.UserInfo;
import com.mylive.infra.jpa.entity.vo.UserInfoVo;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserInfoMapper {
    TokenInfo toTokenDto(UserInfo userInfo);

    UserInfoVo toVo(UserInfo userInfo);
}