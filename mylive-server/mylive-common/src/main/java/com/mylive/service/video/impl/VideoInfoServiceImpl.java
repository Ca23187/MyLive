package com.mylive.service.video.impl;

import com.mylive.config.AppProperties;
import com.mylive.constants.Constants;
import com.mylive.enums.MessageTypeEnum;
import com.mylive.enums.UserActionTypeEnum;
import com.mylive.enums.VideoOrderTypeEnum;
import com.mylive.enums.VideoRecommendTypeEnum;
import com.mylive.exception.BusinessException;
import com.mylive.infra.elasticsearch.ElasticSearchComponent;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.dto.UserMessageExtendDto;
import com.mylive.infra.jpa.entity.po.*;
import com.mylive.infra.jpa.entity.vo.*;
import com.mylive.infra.jpa.repository.*;
import com.mylive.infra.mapstruct.VideoInfoMapper;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.file.storage.BasicStorageService;
import com.mylive.service.file.storage.ObjectStorageService;
import com.mylive.service.user.UserMessageService;
import com.mylive.service.video.VideoInfoService;
import com.mylive.utils.StringTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoInfoServiceImpl implements VideoInfoService {
    public static ExecutorService pool = Executors.newFixedThreadPool(10);

    private final RedisComponent redisComponent;
    private final VideoInfoRepository videoInfoRepository;
    private final VideoInfoPostRepository videoInfoPostRepository;
    private final VideoInfoFileRepository videoInfoFileRepository;
    private final VideoInfoFilePostRepository videoInfoFilePostRepository;
    private final UserActionRepository userActionRepository;
    private final VideoInfoMapper videoInfoMapper;
    private final BasicStorageService basicStorageService;
    private final VideoDanmakuRepository videoDanmakuRepository;
    private final VideoCommentRepository videoCommentRepository;
    private final ElasticSearchComponent elasticSearchComponent;
    private final UserMessageService userMessageService;
    private final ObjectProvider<ObjectStorageService> objectStorageProvider;
    private final AppProperties appProperties;

    private ObjectStorageService oss() {
        return objectStorageProvider.getIfAvailable();
    }

    private boolean isMinioEnabled() {
        return oss() != null;
    }

    @Override
    public List<BasicVideoInfoVo> getRecommendVideoList() {
        return videoInfoRepository.getRecommendVideoList(VideoRecommendTypeEnum.RECOMMEND.getType());
    }

    @Override
    public PaginationResultVo<PortalVideoInfoVo> getPortalVideoPage(Integer parentCategoryId, Integer categoryId, Integer pageNo) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        Page<PortalVideoInfoVo> page = videoInfoRepository.getPortalVideoPage(
                parentCategoryId,
                categoryId,
                VideoRecommendTypeEnum.NO_RECOMMEND.getType(),
                PageRequest.of(pageNo - 1, Constants.PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt")))
        );
        return PaginationResultVo.fromPage(page);
    }

    @Override
    public VideoDetailVo getVideoDetail(String videoId, String token) {
        VideoInfo videoInfo = videoInfoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(ResponseCodeEnum.NOT_FOUND));

        List<UserAction> userActionList = new ArrayList<>();
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        if (null != tokenInfo) {
            userActionList = userActionRepository.findByVideoIdAndUserIdAndActionTypeIn(
                    videoId, tokenInfo.getUserId(),
                    List.of(
                            UserActionTypeEnum.VIDEO_LIKE.getType(),
                            UserActionTypeEnum.VIDEO_SAVE.getType(),
                            UserActionTypeEnum.VIDEO_COIN.getType()
                    )
            );

        }
        return videoInfoMapper.toDetailVo(videoInfo, userActionList);
    }

    @Override
    public List<VideoPartVo> getVideoPartList(String videoId) {
        return videoInfoFileRepository.findVideoPartList(videoId);
    }

    @Override
    public VideoInfoFile getVideoInfoFileByFileId(String fileId) {
        return videoInfoFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ResponseCodeEnum.NOT_FOUND));
    }

    @Override
    public PaginationResultVo<UHomeVideoInfoVo> getUHomeVideoPage(Long userId, Integer type, Integer pageNo, String videoTitleFuzzy, Integer orderType) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        int pageSize = type == null ? Constants.PAGE_SIZE : Constants.PAGE_SIZE_UHOME_DEFAULT;
        VideoOrderTypeEnum typeEnum = VideoOrderTypeEnum.getOrDefaultByType(orderType);
        Pageable pageable = PageRequest.of(
                pageNo - 1,
                pageSize,
                Sort.by(Sort.Order.desc(typeEnum.getField())));
        Page<UHomeVideoInfoVo> page = videoInfoRepository.findUHomeVideoPage(
                userId, StringTools.normalizeKeyword(videoTitleFuzzy), pageable);
        return PaginationResultVo.fromPage(page);
    }

    @Override
    public PaginationResultVo<UHomeVideoInfoVo> getUHomeSavedVideoPage(Long userId, Integer pageNo) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        Pageable pageable = PageRequest.of(
                pageNo - 1,
                Constants.PAGE_SIZE,
                Sort.by(Sort.Order.desc("actionTime")));
        Page<UHomeVideoInfoVo> page = videoInfoRepository.findUHomeSavedVideoPage(userId, UserActionTypeEnum.VIDEO_SAVE.getType(), pageable);
        return PaginationResultVo.fromPage(page);
    }

    @Override
    public List<UHomeVideoInfoVo> getUHomeVideoList(Long seriesId, Long userId) {
        return videoInfoRepository.findUHomeVideoList(userId, seriesId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeInteraction(String videoId, Long userId, Integer allowDanmaku, Integer allowComment) {
        videoInfoRepository.updateInteraction(videoId, userId, allowDanmaku, allowComment);
        videoInfoPostRepository.updateInteraction(videoId, userId, allowDanmaku, allowComment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVideo(String videoId, Long userId, String reason) {
        VideoInfoPost post = videoInfoPostRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(ResponseCodeEnum.NOT_FOUND));
        if (userId != null) {  // 用户操作
            if (!userId.equals(post.getUserId())) {
                throw new BusinessException(ResponseCodeEnum.NOT_FOUND);
            }
        }

        List<String> toDeleteFilePaths = videoInfoFilePostRepository.findFilePathByVideoId(videoId);

        videoInfoPostRepository.delete(post);
        videoInfoRepository.deleteById(videoId);

        videoInfoFileRepository.deleteAllByVideoId(videoId);
        videoInfoFilePostRepository.deleteAllByVideoId(videoId);

        videoDanmakuRepository.deleteAllByVideoId(videoId);
        videoCommentRepository.deleteAllByVideoId(videoId);

        // 保留用户收藏记录
        userActionRepository.deleteAllByVideoIdAndActionTypeIsNot(videoId, UserActionTypeEnum.VIDEO_SAVE.getType());
        // 这里不删用户的播放历史，以及 series 条目

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (userId == null) {
                    UserMessage userMessage = new UserMessage();
                    userMessage.setVideoId(videoId);
                    userMessage.setUserId(post.getUserId());
                    userMessage.setMessageType(MessageTypeEnum.VIDEO_DELETE.getType());
                    UserMessageExtendDto dto = new UserMessageExtendDto();
                    dto.setMessageContent(reason);
                    dto.setVideoTitle(post.getVideoTitle());
                    dto.setVideoCover(post.getVideoCover());
                    userMessageService.saveMessage(userMessage, dto);
                }
                try {
                    basicStorageService.delete(post.getVideoCover());
                } catch (Exception e) {
                    log.error("failed to delete cover {}", post.getVideoCover());
                }
                pool.submit(() -> toDeleteFilePaths.parallelStream()
                        .forEach(path -> {
                            try {
                                if (isMinioEnabled()) {
                                    oss().deleteByPrefix(path);
                                } else {
                                    FileUtils.deleteDirectory(Paths.get(
                                            appProperties.getProjectFolder(),
                                            Constants.FILE_FOLDER,
                                            path
                                    ).toFile());
                                }
                            } catch (Exception e) {
                                log.error("failed to delete file {}", path);
                            }
                        })
                );
                elasticSearchComponent.delDoc(videoId);
            }
        });
    }

    @Override
    public List<BasicVideoInfoVo> getBasicVideoInfoVo(Long userId) {
        return videoInfoRepository.getBasicVoList(userId);
    }

    @Override
    public PaginationResultVo<PortalVideoInfoVo> getHotVideoList(Integer pageNo) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        Page<PortalVideoInfoVo> page = videoInfoRepository.getHotVideoList(
                LocalDateTime.now().minusHours(24),
                PageRequest.of(
                        pageNo - 1,
                        Constants.PAGE_SIZE,
                        Sort.by(Sort.Order.desc("playCount"))
                )
        );
        return PaginationResultVo.fromPage(page);
    }
}
