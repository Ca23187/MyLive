package com.mylive.utils;

import com.mylive.exception.BusinessException;
import com.mylive.response.ResponseCodeEnum;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class ProcessUtils {

    private static final long DEFAULT_TIMEOUT_MINUTES = 30L;

    private ProcessUtils() {}

    // ================== 返回结果对象 ==================

    public record ExecResult(int exitCode, String stdout, String stderr) {
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    // ================== 对外方法：只返回退出码 ==================

    public static int exec(List<String> cmd, Path workDir, boolean printOutput) {
        return exec(cmd, workDir, DEFAULT_TIMEOUT_MINUTES, printOutput);
    }

    public static int exec(List<String> cmd,
                           Path workDir,
                           long timeoutMinutes,
                           boolean printOutput) {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workDir != null) {
            pb.directory(workDir.toFile());
        }
        return doExecWithOutput(pb, timeoutMinutes, printOutput, cmd.toString()).exitCode();
    }

    public static int exec(String cmd, Path workDir, boolean printOutput) {
        return exec(cmd, workDir, DEFAULT_TIMEOUT_MINUTES, printOutput);
    }

    public static int exec(String cmd,
                           Path workDir,
                           long timeoutMinutes,
                           boolean printOutput) {
        ProcessBuilder pb = buildShellProcess(cmd);
        if (workDir != null) {
            pb.directory(workDir.toFile());
        }
        return doExecWithOutput(pb, timeoutMinutes, printOutput, cmd).exitCode();
    }

    // ================== 对外方法：返回 stdout / stderr ==================

    public static ExecResult execForOutput(List<String> cmd,
                                           Path workDir,
                                           boolean printOutput) {
        return execForOutput(cmd, workDir, DEFAULT_TIMEOUT_MINUTES, printOutput);
    }

    public static ExecResult execForOutput(List<String> cmd,
                                           Path workDir,
                                           long timeoutMinutes,
                                           boolean printOutput) {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (workDir != null) {
            pb.directory(workDir.toFile());
        }
        return doExecWithOutput(pb, timeoutMinutes, printOutput, cmd.toString());
    }

    public static ExecResult execForOutput(String cmd,
                                           Path workDir,
                                           boolean printOutput) {
        return execForOutput(cmd, workDir, DEFAULT_TIMEOUT_MINUTES, printOutput);
    }

    public static ExecResult execForOutput(String cmd,
                                           Path workDir,
                                           long timeoutMinutes,
                                           boolean printOutput) {
        ProcessBuilder pb = buildShellProcess(cmd);
        if (workDir != null) {
            pb.directory(workDir.toFile());
        }
        return doExecWithOutput(pb, timeoutMinutes, printOutput, cmd);
    }

    public static String execForStdout(List<String> cmd, Path workDir) {
        ExecResult result = execForOutput(cmd, workDir, false);
        if (!result.isSuccess()) {
            throw new RuntimeException("命令执行失败: " + result.stderr());
        }
        return result.stdout();
    }

    public static int execNoThrow(List<String> cmd,
                                  Path workDir,
                                  long timeoutMinutes,
                                  boolean printOutput) {
        try {
            return exec(cmd, workDir, timeoutMinutes, printOutput);
        } catch (Exception e) {
            return -1;
        }
    }

    // ================== 内部实现 ==================

    private static ProcessBuilder buildShellProcess(String cmd) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new ProcessBuilder("cmd.exe", "/c", cmd);
        }
        return new ProcessBuilder("/bin/sh", "-c", cmd);
    }

    private static ExecResult doExecWithOutput(ProcessBuilder pb,
                                               long timeoutMinutes,
                                               boolean printOutput,
                                               String cmdForLog) {
        try {
            Process process = pb.start();

            int keepLines = printOutput ? Integer.MAX_VALUE : 200;

            StreamGobbler stdoutGobbler =
                    new StreamGobbler(process.getInputStream(), keepLines);
            StreamGobbler stderrGobbler =
                    new StreamGobbler(process.getErrorStream(), keepLines);

            stdoutGobbler.start();
            stderrGobbler.start();

            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);

                try { process.getInputStream().close(); } catch (Exception ignored) {}
                try { process.getErrorStream().close(); } catch (Exception ignored) {}
                try { process.getOutputStream().close(); } catch (Exception ignored) {}

                log.error("命令执行超时: {}", cmdForLog);
                throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
            }

            stdoutGobbler.join(TimeUnit.SECONDS.toMillis(5));
            stderrGobbler.join(TimeUnit.SECONDS.toMillis(5));

            int exitCode = process.exitValue();
            String stdout = stdoutGobbler.getContent();
            String stderr = stderrGobbler.getContent();

            if (printOutput) {
                log.info("命令执行完毕: {}\n退出码: {}\nstdout:\n{}\nstderr:\n{}",
                        cmdForLog, exitCode, stdout, stderr);
            } else {
                if (exitCode != 0) {
                    log.warn("命令执行失败: {}, exitCode={}, stderr(last):\n{}",
                            cmdForLog, exitCode, stderr);
                } else {
                    log.info("命令执行完毕: {}, 退出码: {}", cmdForLog, exitCode);
                }
            }

            return new ExecResult(exitCode, stdout, stderr);

        } catch (IOException e) {
            log.error("执行命令 IO 异常, cmd={}", cmdForLog, e);
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("执行命令被中断, cmd={}", cmdForLog, e);
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR);
        }
    }

    private static class StreamGobbler extends Thread {
        private final InputStream inputStream;
        private final int keepLines;
        private final ArrayDeque<String> lastLines = new ArrayDeque<>();

        StreamGobbler(InputStream inputStream, int keepLines) {
            this.inputStream = inputStream;
            this.keepLines = Math.max(0, keepLines);
            setDaemon(true);
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (keepLines == 0) {
                        continue;
                    }

                    if (keepLines == Integer.MAX_VALUE) {
                        lastLines.addLast(line);
                    } else {
                        if (lastLines.size() >= keepLines) {
                            lastLines.removeFirst();
                        }
                        lastLines.addLast(line);
                    }
                }

            } catch (IOException e) {
                log.warn("读取子进程输出时出错: {}", e.getMessage());
            }
        }

        String getContent() {
            if (lastLines.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (String line : lastLines) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }
}