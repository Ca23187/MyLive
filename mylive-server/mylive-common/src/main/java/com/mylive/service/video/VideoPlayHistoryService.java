package com.mylive.service.video;

import com.mylive.infra.jpa.entity.po.VideoPlayHistory;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;

public interface VideoPlayHistoryService {
    void reportPlayProgress(Long userId, String videoId, String fileId, Integer progress, Integer finished);

    void saveHistory(String field, String oldValue, VideoPlayHistory history);

    PaginationResultVo<VideoPlayHistory> getHistoryPage(Long userId, Integer pageNo);

    void deleteByUserId(Long userId);

    void deleteSingleHistory(Long userId, String videoId);
}
