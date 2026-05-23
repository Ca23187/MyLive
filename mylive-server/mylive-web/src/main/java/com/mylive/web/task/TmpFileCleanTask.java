package com.mylive.web.task;

import com.mylive.config.AppProperties;
import com.mylive.constants.Constants;
import com.mylive.service.file.storage.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
@Slf4j
public class TmpFileCleanTask {
    private final AppProperties appProperties;
    private final ObjectProvider<ObjectStorageService> objectStorageProvider;
    private ObjectStorageService oss() {
        return objectStorageProvider.getIfAvailable();
    }
    private boolean isMinioEnabled() {
        return oss() != null;
    }

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{8}");

    @Scheduled(cron = "0 0 3 * * ?")
    public void clean() {
        delTempFile();
        if (!isMinioEnabled()) {
            Path projectFileFolder = Paths.get(
                    appProperties.getProjectFolder(),
                    Constants.FILE_FOLDER
            );
            Path videoFolder = projectFileFolder.resolve(Constants.FILE_VIDEO);
            Path coverFolder = projectFileFolder.resolve(Constants.FILE_COVER);
            delEmptyExpiredFolder(videoFolder);
            delEmptyExpiredFolder(coverFolder);
        }
    }

    private void delTempFile() {
        Path tempFolder = Paths.get(
                appProperties.getProjectFolder(),
                Constants.FILE_FOLDER,
                Constants.FILE_FOLDER_TEMP
        );

        int expireDate = getExpireDate();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempFolder)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    continue;
                }

                String folderName = path.getFileName().toString();
                if (!DATE_PATTERN.matcher(folderName).matches()) {
                    continue;
                }

                int folderDate = Integer.parseInt(folderName);

                if (folderDate < expireDate) {
                    try {
                        FileSystemUtils.deleteRecursively(path);
                        log.info("Deleted temp folder: {}", path);
                    } catch (Exception e) {
                        log.warn("Failed to delete temp folder: {}", path, e);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan temp folder: {}", tempFolder, e);
        }
    }

    /**
     * 删除目录下过期且递归为空的日期目录
     */
    private void delEmptyExpiredFolder(Path folder) {
        int expireDate = getExpireDate();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    continue;
                }

                String folderName = path.getFileName().toString();
                if (!DATE_PATTERN.matcher(folderName).matches()) {
                    continue;
                }

                int folderDate = Integer.parseInt(folderName);

                if (folderDate < expireDate && isEmptyDirectoryRecursive(path)) {
                    try {
                        FileSystemUtils.deleteRecursively(path);
                        log.info("Deleted empty expired video folder: {}", path);
                    } catch (Exception e) {
                        log.warn("Failed to delete video folder: {}", path, e);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan video folder: {}", folder, e);
        }
    }

    /**
     * 判断一个目录递归之后是否为空：
     * - 里面没有文件
     * - 子目录也全部为空
     */
    private boolean isEmptyDirectoryRecursive(Path dir) {
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.noneMatch(Files::isRegularFile);
        } catch (IOException e) {
            log.warn("Failed to check empty directory: {}", dir, e);
            return false;
        }
    }

    private int getExpireDate() {
        return Integer.parseInt(
                LocalDate.now()
                        .minusDays(2)
                        .format(DateTimeFormatter.BASIC_ISO_DATE)
        );
    }
}
