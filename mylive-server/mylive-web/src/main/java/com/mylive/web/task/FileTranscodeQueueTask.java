package com.mylive.web.task;

import com.mylive.constants.Constants;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.service.file.transcode.FileTranscodeService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileTranscodeQueueTask {

    private final ExecutorService executorService = Executors.newFixedThreadPool(Constants.TRANSCODE_THREAD_POOL_NUM);
    private final FileTranscodeService fileTranscodeService;
    private final RedisComponent redisComponent;

    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }

    @PostConstruct
    public void consumeTranscodeFileQueue() {
        for (int i = 0; i < Constants.TRANSCODE_THREAD_POOL_NUM; i++) {
            executorService.execute(this::consume);
        }
    }

    private void consume() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String fileId = redisComponent.getFileIdFromTranscodeQueue();
                if (fileId == null) {
                    continue;
                }
                fileTranscodeService.transcodeVideoFile(fileId);
            } catch (Exception e) {
                log.error("处理转码文件队列失败", e);
            }
        }
    }
}
