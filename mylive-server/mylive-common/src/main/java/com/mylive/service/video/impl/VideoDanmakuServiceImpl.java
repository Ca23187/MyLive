package com.mylive.service.video.impl;

import com.mylive.constants.Constants;
import com.mylive.enums.SearchOrderTypeEnum;
import com.mylive.enums.StatisticTypeEnum;
import com.mylive.exception.BusinessException;
import com.mylive.infra.elasticsearch.ElasticSearchComponent;
import com.mylive.infra.jpa.entity.po.VideoDanmaku;
import com.mylive.infra.jpa.entity.po.VideoInfo;
import com.mylive.infra.jpa.entity.vo.AdminDanmakuVo;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.UCenterDanmakuVo;
import com.mylive.infra.jpa.repository.VideoDanmakuRepository;
import com.mylive.infra.jpa.repository.VideoInfoRepository;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.video.VideoDanmakuService;
import com.mylive.utils.StringTools;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoDanmakuServiceImpl implements VideoDanmakuService {
    private final VideoInfoRepository videoInfoRepository;
    private final VideoDanmakuRepository videoDanmakuRepository;
    private final ElasticSearchComponent elasticSearchComponent;
    private final RedisComponent redisComponent;

    @Override
    public List<VideoDanmaku> getDanmakuList(String fileId, String videoId) {
        VideoInfo videoInfo = videoInfoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(ResponseCodeEnum.BAD_REQUEST));
        if (videoInfo.getAllowDanmaku() != 1) {
            return List.of();
        }
        return videoDanmakuRepository.findByFileIdOrderByDanmakuIdAsc(fileId);
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void saveVideoDanmaku(VideoDanmaku danmaku) {
        int updated = videoInfoRepository.incrDanmakuCountIfAllowed(danmaku.getVideoId());
        if (updated == 0) {
            throw new BusinessException("Video not found or danmaku disabled");
        }
        videoDanmakuRepository.save(danmaku);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisComponent.updateStatisticCount(danmaku.getVideoUserId(), danmaku.getVideoId(), 1, StatisticTypeEnum.DANMAKU);
                try {
                    elasticSearchComponent.updateDocCount(danmaku.getVideoId(), SearchOrderTypeEnum.VIDEO_DANMAKU.getField(), 1);
                } catch (Exception ignored) {}
            }
        });
    }

    @Override
    public PaginationResultVo<UCenterDanmakuVo> getUCenterDanmakuPage(Integer pageNo, String videoId, Long userId) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        Pageable pageable = PageRequest.of(
                pageNo - 1,
                Constants.PAGE_SIZE,
                Sort.by(Sort.Order.desc("danmakuId"))
        );
        return PaginationResultVo.fromPage(videoDanmakuRepository.findUCenterVoPage(videoId, userId, pageable));
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public void deleteDanmaku(Long userId, Long danmakuId) {
        VideoDanmaku danmaku = videoDanmakuRepository.findById(danmakuId)
                .orElseThrow(() -> new BusinessException(ResponseCodeEnum.NOT_FOUND));

        String videoId = danmaku.getVideoId();
        Long videoUserId = danmaku.getVideoUserId();

        int deleted;
        if (userId == null) {
            deleted = videoDanmakuRepository.deleteByDanmakuId(danmakuId);
        } else {
            deleted = videoDanmakuRepository.deleteByDanmakuIdAndAuth(danmakuId, userId);
        }

        if (deleted == 0) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        if (0 == videoInfoRepository.decrDanmakuCount(videoId)) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisComponent.updateStatisticCount(videoUserId, videoId, -1, StatisticTypeEnum.DANMAKU);
                try {
                    elasticSearchComponent.updateDocCount(videoId, SearchOrderTypeEnum.VIDEO_DANMAKU.getField(), -1);
                } catch (Exception ignored) {}
            }
        });
    }

    @Override
    public PaginationResultVo<AdminDanmakuVo> getAdminDanmakuPage(Integer pageNo, String videoTitleFuzzy) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        Pageable pageable = PageRequest.of(
                pageNo - 1,
                Constants.PAGE_SIZE,
                Sort.by(Sort.Order.desc("danmakuId"))
        );
        Page<AdminDanmakuVo> page = videoDanmakuRepository.findAdminVoPage(
                StringTools.normalizeKeyword(videoTitleFuzzy), pageable);
        return PaginationResultVo.fromPage(page);
    }
}
