package com.mylive.service.video;

import com.mylive.infra.jpa.entity.po.UserVideoSeries;
import com.mylive.infra.jpa.entity.vo.UserVideoSeriesDetailVo;
import com.mylive.infra.jpa.entity.vo.VideoSeriesWithVideoVo;

import java.util.List;

public interface UserVideoSeriesService {
    List<UserVideoSeries> getAllSeries(Long userId);

    void saveUserVideoSeries(UserVideoSeries videoSeries, String videoIds);

    UserVideoSeriesDetailVo getVideoSeriesDetail(Long seriesId);

    void addSeriesVideo(Long userId, Long seriesId, String videoIds);

    void delSeriesVideo(Long userId, Long seriesId, String videoId);

    void delVideoSeries(Long userId, Long seriesId);

    void reorderVideoSeries(Long userId, String seriesIds);

    void reorderSeriesVideo(Long userId, Long seriesId, String videoIds);

    List<VideoSeriesWithVideoVo> getVideoSeriesWithVideo(Long userId);
}
