package com.mylive.service.video.post;

import com.mylive.constants.Constants;
import com.mylive.enums.VideoFileTranscodeResultEnum;
import com.mylive.enums.VideoFileUpdateTypeEnum;
import com.mylive.enums.VideoStatusEnum;
import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.dto.request.VideoInfoPostDto;
import com.mylive.infra.jpa.entity.po.VideoInfoFilePost;
import com.mylive.infra.jpa.entity.po.VideoInfoPost;
import com.mylive.infra.jpa.entity.vo.*;
import com.mylive.infra.jpa.repository.VideoInfoFilePostRepository;
import com.mylive.infra.jpa.repository.VideoInfoPostRepository;
import com.mylive.infra.mapstruct.VideoInfoPostMapper;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.utils.StringTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserVideoInfoPostServiceImpl implements UserVideoInfoPostService {

    private final RedisComponent redisComponent;
    private final VideoInfoPostRepository videoInfoPostRepository;
    private final VideoInfoFilePostRepository videoInfoFilePostRepository;
    private final VideoInfoPostMapper videoInfoPostMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveVideoInfoPost(Long userId, VideoInfoPostDto dto) {
        trimVideoInfoPostDto(dto);
        List<VideoInfoFilePost> submittedFileList = dto.getFileInfoList();
        if (submittedFileList.size() > redisComponent.getSysSettingDto().getVideoPartCount()) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        String videoId = dto.getVideoId();
        List<String> toDeleteFilePathList = new ArrayList<>();
        List<String> newFileIdList;

        boolean isFirstUpload = !StringUtils.hasText(videoId);
        if (isFirstUpload) {
            VideoInfoPost videoInfoPost = videoInfoPostMapper.toVideoInfoPost(dto);
            videoInfoPost.setUserId(userId);
            videoId = StringTools.getRandomString(Constants.VIDEO_ID_LENGTH);
            videoInfoPost.setVideoId(videoId);
            videoInfoPost.setStatus(VideoStatusEnum.TRANSCODING.getStatus());
            videoInfoPost.setLastUpdatedAt(LocalDateTime.now());
            videoInfoPostRepository.save(videoInfoPost);

            int index = 1;
            for (VideoInfoFilePost item : submittedFileList) {
                item.setFileIndex(index++);
                item.setVideoId(videoId);
                item.setUserId(userId);
                item.setFileId(StringTools.getRandomString(Constants.FILE_ID_LENGTH));
                item.setUpdateType(VideoFileUpdateTypeEnum.UPDATED.getStatus());
                item.setTranscodeResult(VideoFileTranscodeResultEnum.TRANSCODING.getStatus());
            }
            videoInfoFilePostRepository.saveAll(submittedFileList);

            newFileIdList = submittedFileList.stream()
                    .map(VideoInfoFilePost::getFileId)
                    .toList();

        } else { // 如果不是首次上传，说明是在编辑已有视频
            VideoInfoPost entity = videoInfoPostRepository.findByVideoId(dto.getVideoId());
            // 不允许编辑不存在、正在转码或者等待审核的视频
            if (entity == null
                    || Objects.equals(VideoStatusEnum.TRANSCODING.getStatus(), entity.getStatus())
                    || Objects.equals(VideoStatusEnum.PENDING.getStatus(), entity.getStatus())) {
                throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
            }

            // 查询数据库中已有的视频文件
            List<VideoInfoFilePost> dbFilePostList = videoInfoFilePostRepository.findByVideoIdAndUserId(videoId, userId);
            Map<String, VideoInfoFilePost> submittedFileMap =
                    submittedFileList.stream().collect(
                            Collectors.toMap(
                                    VideoInfoFilePost::getUploadId,
                                    Function.identity(),
                                    (data1, data2) -> data2
                            )
                    );
            // 删除的文件 -> 数据库中有，但用户最新提交的 submittedFileList 里没有
            boolean isTitleUpdated = false;
            List<VideoInfoFilePost> toDeleteFileList = new ArrayList<>();
            for (VideoInfoFilePost dbFilePost : dbFilePostList) {
                VideoInfoFilePost matchedRecord = submittedFileMap.get(dbFilePost.getUploadId());
                if (matchedRecord == null) {
                    toDeleteFileList.add(dbFilePost);
                } else if (!matchedRecord.getTitle().equals(dbFilePost.getTitle())) {
                    // 数据库里有，用户也提交了，但文件名变了，需要更新
                    dbFilePost.setTitle(matchedRecord.getTitle());
                    isTitleUpdated = true;
                }
            }
            // 新增的文件 ->  没有 fileId 就是新增的文件（因为还未落库）
            List<VideoInfoFilePost> newFileList = submittedFileList.stream()
                    .filter(item -> !StringUtils.hasText(item.getFileId()))
                    .toList();

            // 判断视频信息是否有更改
            if (!newFileList.isEmpty()) {
                // 有新增视频。因此先设置状态为转码中
                entity.setStatus(VideoStatusEnum.TRANSCODING.getStatus());
            } else if (isVideoInfoChanged(dto, entity) || isTitleUpdated) {
                // 没新增视频就看是否改了视频信息，改了就变待审核
                entity.setStatus(VideoStatusEnum.PENDING.getStatus());
            }
            videoInfoPostMapper.updateFromDtoIgnoreNull(dto, entity);
            videoInfoPostRepository.save(entity);

            // 有增或有删都得重排顺序
            if (!newFileList.isEmpty() || !toDeleteFileList.isEmpty()) {
                List<VideoInfoFilePost> totalList = new ArrayList<>(dbFilePostList);
                totalList.removeAll(toDeleteFileList);  // 剔除要删除的
                totalList.addAll(newFileList);  // 追加新增了的

                int index = 1;
                for (VideoInfoFilePost item : totalList) {
                    item.setFileIndex(index++);
                }
            }

            // 新增追加视频
            for (VideoInfoFilePost item : newFileList) {
                item.setVideoId(videoId);
                item.setUserId(userId);
                item.setFileId(StringTools.getRandomString(Constants.FILE_ID_LENGTH));
                item.setUpdateType(VideoFileUpdateTypeEnum.UPDATED.getStatus());
                item.setTranscodeResult(VideoFileTranscodeResultEnum.TRANSCODING.getStatus());
            }
            // 已有视频会做脏检查自动更新
            videoInfoFilePostRepository.saveAll(newFileList);

            // 清除已经删除的视频
            if (!toDeleteFileList.isEmpty()) {
                List<String> delFileIdList = toDeleteFileList.stream()
                        .map(VideoInfoFilePost::getFileId)
                        .toList();
                videoInfoFilePostRepository.deleteAllByFileIdInAndUserId(delFileIdList, userId);
            }

            toDeleteFilePathList = toDeleteFileList.stream()
                    .map(VideoInfoFilePost::getFilePath)
                    .toList();
            newFileIdList = newFileList.stream()
                    .map(VideoInfoFilePost::getFileId)
                    .toList();
        }

        // 事务后提交待删除视频列表，并将待转码视频加入队列
        // 坑：事务比 Redis 迟，导致合并视频的 consumer 在数据库里查不到记录
        List<String> finalToDeleteFilePathList = toDeleteFilePathList;
        String finalVideoId = videoId;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (!finalToDeleteFilePathList.isEmpty()) {
                    redisComponent.saveDelFilePathList(finalVideoId, finalToDeleteFilePathList);
                }
                if (!newFileIdList.isEmpty()) {
                    redisComponent.addFileIds2TranscodeQueue(newFileIdList);
                }
            }
        });
    }

    private void trimVideoInfoPostDto(VideoInfoPostDto dto) {
        dto.setVideoTitle(dto.getVideoTitle().trim());
        for (VideoInfoFilePost file : dto.getFileInfoList()) {
            if (!StringUtils.hasText(file.getTitle()))
                throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
            file.setTitle(file.getTitle().trim());
        }
    }

    private boolean isVideoInfoChanged(VideoInfoPostDto dto, VideoInfoPost entity) {
        // 看标题，封面，标签，简介，转载类型，转载说明有没有变
        return !Objects.equals(dto.getVideoCover(), entity.getVideoCover())
                || !Objects.equals(dto.getVideoTitle(), entity.getVideoTitle())
                || !Objects.equals(dto.getTags(), entity.getTags())
                || !Objects.equals(dto.getIntroduction(), entity.getIntroduction())
                || !Objects.equals(dto.getPostType(), entity.getPostType())
                || !Objects.equals(dto.getOriginInfo(), entity.getOriginInfo());
    }

    @Override
    public PaginationResultVo<VideoInfoPostVo> findPostPage4User(Integer status, Integer pageNo, String videoTitleFuzzy, Long userId) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        List<Integer> excludeStatusList = null;
        if (status != null && status == -1) {
            excludeStatusList = List.of(VideoStatusEnum.PASSED.getStatus(), VideoStatusEnum.REJECTED.getStatus());
            status = null;
        }
        Page<VideoInfoPostVo> page = videoInfoPostRepository.findPostPage4User(
                userId,
                status,
                StringTools.normalizeKeyword(videoTitleFuzzy),
                excludeStatusList,
                PageRequest.of(pageNo - 1, Constants.PAGE_SIZE, Sort.by(Sort.Order.desc("createdAt")))
        );
        return PaginationResultVo.fromPage(page);
    }

    @Override
    public VideoStatusCountVo getStatusCount(Long userId) {
        return videoInfoPostRepository.countVideoStatus(
                userId,
                VideoStatusEnum.PASSED.getStatus(),
                VideoStatusEnum.REJECTED.getStatus()
        );
    }

    @Override
    public PostedVideoEditVo getEditVideoPost(Long userId, String videoId) {
        VideoInfoPost videoInfoPost = videoInfoPostRepository.findByUserIdAndVideoId(userId, videoId);
        if (videoInfoPost == null) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        List<VideoPartVo> list = videoInfoFilePostRepository.findVideoPartList(videoId);
        return videoInfoPostMapper.toEditVo(videoInfoPost, list);
    }

}
