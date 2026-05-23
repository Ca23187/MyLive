package com.mylive.service.video;

import com.mylive.infra.jpa.entity.po.VideoDanmaku;
import com.mylive.infra.jpa.entity.vo.AdminDanmakuVo;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.UCenterDanmakuVo;

import java.util.List;

public interface VideoDanmakuService {
    List<VideoDanmaku> getDanmakuList(String fileId, String videoId);

    void saveVideoDanmaku(VideoDanmaku danmaku);

    PaginationResultVo<UCenterDanmakuVo> getUCenterDanmakuPage(Integer pageNo, String videoId, Long userId);

    void deleteDanmaku(Long userId, Long danmakuId);

    PaginationResultVo<AdminDanmakuVo> getAdminDanmakuPage(Integer pageNo, String videoTitleFuzzy);
}
