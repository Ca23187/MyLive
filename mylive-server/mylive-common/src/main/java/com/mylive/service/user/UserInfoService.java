package com.mylive.service.user;

import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.po.UserInfo;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.UserCountInfoVo;
import com.mylive.infra.jpa.entity.vo.UserInfoVo;

public interface UserInfoService {
    void register(String email, String nickname, String registerPassword, String emailCode);

    TokenInfo login(String email, String password, String ip);

    UserInfoVo getUserDetail(Long targetUserId, Long currentUserId);

    void updateUserInfo(UserInfo userInfo, TokenInfo tokenInfo, String token);

    void updateTheme(Integer theme, Long userId);

    UserCountInfoVo getUserCountInfo(Long userId);

    void updateUserStatus(Long userId);

    PaginationResultVo<UserInfo> getUserInfo4Admin(Integer pageNo, Integer pageSize, String nicknameFuzzy, Integer status);

    void sendEmailCode(String email);
}
