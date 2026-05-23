package com.mylive.admin.controller;

import com.mylive.infra.jpa.entity.dto.statistic.AdminStatisticDto;
import com.mylive.response.ResponseVo;
import com.mylive.service.statistic.admin.AdminStatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/index")
@RequiredArgsConstructor
public class IndexController {
    private final AdminStatisticService service;

    @PostMapping("/getRealTimeStatisticInfo")
    public ResponseVo<Map<String, Object>> getRealTimeStatisticInfo() {
        return ResponseVo.ok(service.getRealTimeStatisticInfo());
    }

    @PostMapping("/getWeekStatisticInfo")
    public ResponseVo<List<AdminStatisticDto>> getWeekStatisticInfo(Integer dataType) {
        return ResponseVo.ok(service.getWeekStatisticInfo(dataType));
    }
}
