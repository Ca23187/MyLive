package com.mylive.web.controller;

import com.mylive.infra.jpa.entity.dto.SysSettingDto;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/sysSetting")
@RequiredArgsConstructor
public class SysSettingController {

    private final RedisComponent redisComponent;

    @PostMapping("/getSetting")
    public ResponseVo<SysSettingDto> getSetting() {
        return ResponseVo.ok(redisComponent.getSysSettingDto());
    }
}