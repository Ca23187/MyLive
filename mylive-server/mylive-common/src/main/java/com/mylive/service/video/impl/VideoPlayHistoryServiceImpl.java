package com.mylive.service.video.impl;

import com.mylive.constants.Constants;
import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.po.VideoInfo;
import com.mylive.infra.jpa.entity.po.VideoInfoFile;
import com.mylive.infra.jpa.entity.po.VideoPlayHistory;
import com.mylive.infra.jpa.entity.po.id.VideoPlayHistoryId;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.repository.VideoInfoFileRepository;
import com.mylive.infra.jpa.repository.VideoInfoRepository;
import com.mylive.infra.jpa.repository.VideoPlayHistoryRepository;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.infra.redis.RedisUtils;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.video.VideoPlayHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VideoPlayHistoryServiceImpl implements VideoPlayHistoryService {
    private final VideoInfoFileRepository videoInfoFileRepository;
    private final VideoInfoRepository videoInfoRepository;
    private final VideoPlayHistoryRepository videoPlayHistoryRepository;
    private final RedisComponent redisComponent;
    private final RedisUtils redisUtils;

    @Override
    public void reportPlayProgress(Long userId, String videoId, String fileId, Integer progress, Integer finished) {
        VideoPlayHistory cache = redisComponent.getVideoPlayHistory(userId, videoId);

        if (cache == null) {
            VideoInfo videoInfo = videoInfoRepository.findById(videoId)
                    .orElseThrow(() -> new BusinessException(ResponseCodeEnum.BAD_REQUEST));

            VideoInfoFile videoFile = videoInfoFileRepository.findByFileIdAndVideoId(fileId, videoId);
            if (videoFile == null) {
                throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
            }

            cache = new VideoPlayHistory();
            cache.setUserId(userId);
            cache.setVideoId(videoId);
            cache.setVideoTitle(videoInfo.getVideoTitle());
            cache.setVideoCover(videoInfo.getVideoCover());
            cache.setFileId(fileId);
            cache.setDuration(videoFile.getDuration());
            cache.setFileTitle(videoFile.getTitle());
        } else {
            if (!videoId.equals(cache.getVideoId())) {
                throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
            }

            if (!fileId.equals(cache.getFileId())) {
                VideoInfoFile videoFile = videoInfoFileRepository.findByFileIdAndVideoId(fileId, videoId);
                if (videoFile == null) {
                    throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
                }

                cache.setFileId(fileId);
                cache.setDuration(videoFile.getDuration());
                cache.setFileTitle(videoFile.getTitle());
            }
        }

        progress = Math.max(progress == null ? 0 : progress, 0);

        int duration = cache.getDuration() == null ? 0 : cache.getDuration();

        cache.setProgress(progress);
        cache.setFinished(duration > 0 && progress >= duration - 5 ? 1 : 0);
        cache.setLastPlayedAt(LocalDateTime.now());

        redisComponent.saveVideoPlayHistory(cache);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveHistory(String field, String oldValue, VideoPlayHistory history) {
        videoPlayHistoryRepository.save(history);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        redisUtils.hDeleteIfEquals(
                                Constants.REDIS_KEY_VIDEO_PLAY_HISTORY,
                                field,
                                oldValue
                        );
                    }
                }
        );
    }

    @Override
    public PaginationResultVo<VideoPlayHistory> getHistoryPage(Long userId, Integer pageNo) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        Pageable pageable = PageRequest.of(
                pageNo - 1,
                Constants.PAGE_SIZE, Sort.by(Sort.Order.desc("lastPlayedAt")));
        Page<VideoPlayHistory> page = videoPlayHistoryRepository.getVideoPage(userId, pageable);
        return PaginationResultVo.fromPage(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUserId(Long userId) {
        videoPlayHistoryRepository.deleteAllByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSingleHistory(Long userId, String videoId) {
        videoPlayHistoryRepository.deleteById(new VideoPlayHistoryId(userId, videoId));
    }
}
