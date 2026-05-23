package com.mylive.utils;

import com.mylive.constants.Constants;
import com.mylive.exception.BusinessException;
import com.mylive.response.ResponseCodeEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

@Slf4j
public final class VideoTools {
    private VideoTools() {}

    public static void cutVideo(Path sourceFile, boolean delSource) {
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new IllegalArgumentException("输入文件不存在或不是文件: " + sourceFile);
        }

        Path parent = sourceFile.getParent();
        if (parent == null) {
            parent = Paths.get(".").toAbsolutePath().normalize();
        }

        String fileName = sourceFile.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = (dot > 0) ? fileName.substring(0, dot) : fileName;

        Path m3u8Path = parent.resolve(Constants.M3U8_NAME);
        Path segTmpl  = parent.resolve("%04d.ts");

        List<String> cmd = Arrays.asList(
                "ffmpeg", "-y",
                "-i", sourceFile.toAbsolutePath().toString(),
                "-map", "0:v:0",
                "-map", "0:a:0?",
                "-c:v", "copy",
                "-c:a", "copy",
                "-bsf:v", "h264_mp4toannexb",
                "-f", "hls",
                "-hls_time", "30",
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segTmpl.toAbsolutePath().toString(),
                m3u8Path.toAbsolutePath().toString()
        );

        try {
            int code = ProcessUtils.exec(cmd, parent, false);
            if (code != 0) {
                throw new RuntimeException("ffmpeg 执行失败，退出码: " + code);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("ffmpeg 执行异常", e);
        }

        if (delSource) {
            try {
                Files.deleteIfExists(sourceFile);
            } catch (IOException e) {
                throw new RuntimeException("切片成功，但删除源文件失败: " + sourceFile.toAbsolutePath(), e);
            }
        }
    }

    /**
     * 为视频生成封面（当前暂时跳过前2秒）
     */
    public static void createCover4Video(Path sourceFile,
                                         Integer width,
                                         Path targetFile) {
        // 输出先写到 tmp
        Path tmp = FileTools.getTmpPath(targetFile);
        // 使用：ffmpeg -y -i input -vframes 1 -vf scale=WIDTH:-1 output
        // 让高度按比例缩放
        List<String> cmd = Arrays.asList(
                "ffmpeg",
                "-y",
                "-i", sourceFile.toAbsolutePath().toString(),
                "-frames:v", "1",
                "-vf", "scale=" + width + ":-1",
                tmp.toAbsolutePath().toString()
        );

        int exitCode = ProcessUtils.exec(cmd, sourceFile.getParent(), false);
        if (exitCode != 0) {  // 失败清理 tmp
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            log.error("生成视频封面失败, exitCode={}, source={}, target={}",
                    exitCode, sourceFile, targetFile);
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }

        // 成功后 move tmp -> target（优先原子，失败降级）
        try {
            try {
                Files.move(tmp, targetFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, targetFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            log.error("移动视频封面失败, source={}, target={}", sourceFile, targetFile, e);
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        } finally {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
        }
    }

    /**
     * 获取视频时长，单位：秒
     */
    public static int getVideoDuration(Path sourceFile) {
        List<String> cmd = Arrays.asList(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                sourceFile.toAbsolutePath().toString()
        );

        ProcessUtils.ExecResult result =
                ProcessUtils.execForOutput(cmd, sourceFile.getParent(), false);

        if (result.exitCode() != 0) {
            throw new RuntimeException("获取视频时长失败: " + result.stderr());
        }

        String out = result.stdout().trim();
        if (out.isEmpty()) {
            throw new RuntimeException("ffprobe 未返回时长");
        }

        return new BigDecimal(out).intValue();
    }

    /**
     * 获取视频编码格式（如 h264 / hevc / vp9）
     */
    public static String getVideoCodec(Path sourceFile) {
        List<String> cmd = Arrays.asList(
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_name",
                "-of", "default=noprint_wrappers=1:nokey=1",
                sourceFile.toAbsolutePath().toString()
        );

        String out = ProcessUtils.execForStdout(cmd, sourceFile.getParent());
        return out == null ? null : out.trim().toLowerCase();
    }

    /**
     * 转码为 H264 MP4
     */
    /**
     * 将视频转码为标准 H264 MP4，并替换原文件
     */
    public static void transcodeToMp4Replace(Path sourceFile) {
        if (sourceFile == null || !Files.isRegularFile(sourceFile)) {
            throw new IllegalArgumentException("视频文件不存在或不是文件: " + sourceFile);
        }

        Path parent = sourceFile.getParent();
        if (parent == null) {
            parent = Paths.get(".").toAbsolutePath().normalize();
        }

        String fileName = sourceFile.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;

        Path tmp = parent.resolve(baseName + ".__transcode_tmp__.mp4");

        List<String> cmd = Arrays.asList(
                "ffmpeg", "-y",
                "-i", sourceFile.toAbsolutePath().toString(),
                "-map", "0:v:0",
                "-map", "0:a:0?",
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-crf", "23",
                "-profile:v", "main",
                "-level", "4.1",
                "-pix_fmt", "yuv420p",
                "-c:a", "aac",
                "-b:a", "128k",
                "-ac", "2",
                "-movflags", "+faststart",
                tmp.toAbsolutePath().toString()
        );

        int code = ProcessUtils.exec(cmd, parent, false);
        if (code != 0) {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            throw new RuntimeException("视频转码失败: " + sourceFile);
        }

        try {
            try {
                Files.move(tmp, sourceFile,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, sourceFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            throw new RuntimeException("替换转码后视频失败: " + sourceFile, e);
        }
    }
}
