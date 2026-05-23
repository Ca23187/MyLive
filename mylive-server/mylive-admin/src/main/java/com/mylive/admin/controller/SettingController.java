package com.mylive.admin.controller;

import com.mylive.infra.jpa.entity.dto.SysSettingDto;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/setting")
@RequiredArgsConstructor
public class SettingController {

    private final RedisComponent redisComponent;

    @PostMapping("/getSetting")
    public ResponseVo<SysSettingDto> getSetting() {
        return ResponseVo.ok(redisComponent.getSysSettingDto());
    }

    @PostMapping("/saveSetting")
    public ResponseVo<Void> saveSetting(SysSettingDto sysSettingDto) {
        redisComponent.saveSysSettingDto(sysSettingDto);
        return ResponseVo.ok();
    }
}
