package com.mylive.admin.controller;

import com.mylive.infra.jpa.entity.po.UserInfo;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.response.ResponseVo;
import com.mylive.service.user.UserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserInfoService service;

    @PostMapping("/loadUser")
    public ResponseVo<PaginationResultVo<UserInfo>> loadUser(Integer pageNo,
                                                             Integer pageSize,
                                                             String nicknameFuzzy,
                                                             Integer status) {
        return ResponseVo.ok(service.getUserInfo4Admin(pageNo, pageSize, nicknameFuzzy, status));
    }

    @PostMapping("/changeStatus")
    public ResponseVo<Void> changeStatus(Long userId) {
        service.updateUserStatus(userId);
        return ResponseVo.ok();
    }
}
