package com.mylive.service.file.transcode;

import com.mylive.enums.VideoFileTranscodeResultEnum;
import com.mylive.enums.VideoStatusEnum;
import com.mylive.infra.jpa.entity.dto.TranscodeResult;
import com.mylive.infra.jpa.entity.po.VideoInfoFilePost;
import com.mylive.infra.jpa.repository.VideoInfoFilePostRepository;
import com.mylive.infra.jpa.repository.VideoInfoPostRepository;
import com.mylive.infra.redis.RedisComponent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileTranscodeTxServiceImpl implements FileTranscodeTxService {

    private final VideoInfoFilePostRepository videoInfoFilePostRepository;
    private final RedisComponent redisComponent;
    private final VideoInfoPostRepository videoInfoPostRepository;

    @Transactional(rollbackFor = Exception.class)
    public void updateSuccess(String postId, TranscodeResult result) {

        VideoInfoFilePost post = videoInfoFilePostRepository.findById(postId).orElseThrow();

        post.setDuration(result.getDuration());
        post.setFileSize(result.getFileSize());
        post.setFilePath(result.getFilePath());
        post.setTranscodeResult(VideoFileTranscodeResultEnum.SUCCESS.getStatus());

        videoInfoFilePostRepository.save(post);

        updateVideoStatus(post.getVideoId());

        redisComponent.delUploadingFileInfo(post.getUserId(), post.getUploadId());
    }

    private void updateVideoStatus(String videoId) {
        // NOTE: 坑：多个分P同时转码完成，但查库时都统计到 transcodeCount > 0，导致 videoInfoPost 不更新
        // NOTE: 需要先给数据行加悲观锁，让该方法串行化执行
        // TODO: 分P数量很多时建议上 Redis
        videoInfoPostRepository.lockByVideoId(videoId);

        long failCount = videoInfoFilePostRepository
                .countByVideoIdAndTranscodeResult(videoId, VideoFileTranscodeResultEnum.FAIL.getStatus());

        if (failCount > 0) {
            videoInfoPostRepository.updateStatusByVideoId(
                    VideoStatusEnum.TRANSCODE_FAILED.getStatus(), videoId);
            return;
        }

        long transcodeCount = videoInfoFilePostRepository
                .countByVideoIdAndTranscodeResult(videoId, VideoFileTranscodeResultEnum.TRANSCODING.getStatus());

        if (transcodeCount == 0) {
            int duration = videoInfoFilePostRepository.sumDuration(videoId);
            videoInfoPostRepository.updateStatusAndDurationByVideoId(
                    VideoStatusEnum.PENDING.getStatus(), duration, videoId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFail(String postId) {

        VideoInfoFilePost post = videoInfoFilePostRepository.findById(postId).orElseThrow();

        post.setTranscodeResult(VideoFileTranscodeResultEnum.FAIL.getStatus());
        videoInfoFilePostRepository.save(post);

        videoInfoPostRepository.updateStatusByVideoId(
                VideoStatusEnum.TRANSCODE_FAILED.getStatus(),
                post.getVideoId()
        );
    }
}
