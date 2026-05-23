package com.mylive.service.video.post;

import com.mylive.config.AppProperties;
import com.mylive.constants.Constants;
import com.mylive.enums.MessageTypeEnum;
import com.mylive.enums.VideoFileUpdateTypeEnum;
import com.mylive.enums.VideoStatusEnum;
import com.mylive.exception.BusinessException;
import com.mylive.infra.elasticsearch.ElasticSearchComponent;
import com.mylive.infra.jpa.entity.dto.SysSettingDto;
import com.mylive.infra.jpa.entity.dto.UserMessageExtendDto;
import com.mylive.infra.jpa.entity.dto.request.AdminVideoInfoPostQuery;
import com.mylive.infra.jpa.entity.po.*;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.VideoInfoPostVo;
import com.mylive.infra.jpa.entity.vo.VideoPartVo;
import com.mylive.infra.jpa.repository.*;
import com.mylive.infra.mapstruct.VideoInfoFilePostMapper;
import com.mylive.infra.mapstruct.VideoInfoMapper;
import com.mylive.infra.mapstruct.VideoInfoPostMapper;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.file.storage.ObjectStorageService;
import com.mylive.service.user.UserMessageService;
import com.mylive.utils.StringTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminVideoInfoPostServiceImpl implements AdminVideoInfoPostService {

    private final VideoInfoPostRepository videoInfoPostRepository;
    private final VideoInfoFilePostRepository videoInfoFilePostRepository;
    private final VideoInfoRepository videoInfoRepository;
    private final RedisComponent redisComponent;
    private final VideoInfoPostMapper videoInfoPostMapper;
    private final VideoInfoFileRepository videoInfoFileRepository;
    private final VideoInfoFilePostMapper videoInfoFilePostMapper;
    private final ObjectProvider<ObjectStorageService> objectStorageProvider;
    private final AppProperties appProperties;
    private final UserInfoRepository userInfoRepository;
    private final VideoInfoMapper videoInfoMapper;
    private final ElasticSearchComponent elasticSearchComponent;
    private final UserMessageService userMessageService;

    private ObjectStorageService oss() {  // 可选注入，切 minio 就启用
        return objectStorageProvider.getIfAvailable();
    }

    private boolean isMinioEnabled() {
        return oss() != null;
    }

    @Override
    public PaginationResultVo<VideoInfoPostVo> findPostPage4Admin(AdminVideoInfoPostQuery query) {
        Integer pageNo = query.getPageNo();
        Integer pageSize = query.getPageSize();
        if (pageNo == null || pageNo < 1) pageNo = 1;
        if (pageSize == null || pageSize < 1) pageSize = Constants.PAGE_SIZE;
        Page<VideoInfoPostVo> page = videoInfoPostRepository.findPostPage4Admin(
                StringTools.normalizeKeyword(query.getVideoTitleFuzzy()),
                query.getCategoryId(),
                query.getParentCategoryId(),
                query.getRecommendType(),
                PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Order.desc("lastUpdatedAt")))
        );
        return PaginationResultVo.fromPage(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewVideo(String videoId, Long userId, Integer status, String reason) {
        VideoStatusEnum videoStatusEnum = VideoStatusEnum.getByStatus(status);
        if (videoStatusEnum == null) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        VideoInfoPost videoInfoPost = videoInfoPostRepository.findByVideoId(videoId);
        if (videoInfoPost == null || !Objects.equals(videoInfoPost.getUserId(), userId)) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        int reviewCount = videoInfoPostRepository.updateStatusByVideoIdAndStatus(
                status, videoId, VideoStatusEnum.PENDING.getStatus()
        );
        if (reviewCount == 0) {
            throw new BusinessException("Review failed, please try again later");
        }

        UserMessage userMessage = new UserMessage();
        userMessage.setVideoId(videoId);
        userMessage.setUserId(userId);
        userMessage.setMessageType(MessageTypeEnum.SYS.getType());
        UserMessageExtendDto dto = new UserMessageExtendDto();
        dto.setReviewStatus(status);
        dto.setVideoTitle(videoInfoPost.getVideoTitle());
        dto.setVideoCover(videoInfoPost.getVideoCover());

        // 更新视频状态
        videoInfoFilePostRepository.updateUpdateTypeByVideoId(
                VideoFileUpdateTypeEnum.NO_UPDATE.getStatus(), videoId);

        if (VideoStatusEnum.REJECTED == videoStatusEnum) {
            dto.setMessageContent(reason == null ? "" : reason.trim());
            userMessageService.saveMessage(userMessage, dto);
            return;
        }

        // 如果是第一次发布则增加用户积分
        VideoInfo entity = videoInfoRepository.findByVideoId(videoId);
        if (entity == null) {
            SysSettingDto sysSettingDto = redisComponent.getSysSettingDto();
            userInfoRepository.increaseCoinCount(videoInfoPost.getUserId(), sysSettingDto.getPostVideoCoinCount());
            // 更新发布信息到正式表（注意 VideoInfo 得开 DynamicInsert）
            entity = videoInfoPostMapper.toVideoInfo(videoInfoPost);
            videoInfoRepository.save(entity);

        }
        else {
            videoInfoMapper.updateFromPost(videoInfoPost, entity);
            // entity 自动脏检查更新，不用手动 save
        }

        // 更新视频信息到正式表，先删除再添加
        videoInfoFileRepository.deleteByVideoId(videoId);

        // 查询发布表中的视频信息
        List<VideoInfoFilePost> videoInfoFilePostList = videoInfoFilePostRepository.findByVideoId(videoId);
        List<VideoInfoFile> videoInfoFileList = videoInfoFilePostMapper.toVideoInfoFileList(videoInfoFilePostList);
        videoInfoFileRepository.saveAll(videoInfoFileList);
        elasticSearchComponent.saveDoc(entity);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisComponent.cleanUserCountInfo(videoInfoPost.getUserId());
                    userMessageService.saveMessage(userMessage, dto);

                    // 删除文件
                    List<String> filePathList = redisComponent.getDelFilePathList(videoId);
                    if (filePathList != null) {
                        for (String path : filePathList) {
                            if (isMinioEnabled()) {
                                try {
                                    oss().deleteByPrefix(path);
                                } catch (Exception e) {
                                    log.error("MinIO删除文件失败 path={}", path, e);
                                }
                            } else {
                                Path filePath = Paths.get(
                                        appProperties.getProjectFolder(),
                                        Constants.FILE_FOLDER,
                                        path);
                                if (Files.exists(filePath)) {
                                    try {
                                        FileUtils.deleteDirectory(filePath.toFile());
                                    } catch (IOException e) {
                                        log.error("删除文件失败 path={}", path, e);
                                    }
                                }
                            }
                        }
                    }
                    redisComponent.cleanDelFilePathList(videoId);
                }
            }
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recommendVideo(String videoId) {
        videoInfoRepository.updateRecommendTypeByVideoId(videoId);
    }

    @Override
    public VideoInfoFilePost getFilePostByFileId(String fileId) {
        return videoInfoFilePostRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ResponseCodeEnum.BAD_REQUEST));
    }

    @Override
    public List<VideoPartVo> getVideoPartList(String videoId) {
        return videoInfoFilePostRepository.findVideoPartList(videoId);
    }
}
