package com.mylive.utils;

import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.dto.FileReadResourceDto;
import com.mylive.response.ResponseCodeEnum;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

public final class ServletNetUtils {

    private ServletNetUtils() {}

    public static void writeResource(HttpServletResponse response, FileReadResourceDto res) {
        String contentType = StringUtils.hasText(res.getContentType())
                ? res.getContentType()
                : "application/octet-stream";

        response.setContentType(contentType);
        response.setContentLengthLong(res.getContentLength());

        try (InputStream in = res.getOpenStream().get()) {
            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();

        } catch (IOException | UncheckedIOException e) {
            if (isClientAbort(e)) {
                return;
            }
            throw new BusinessException(ResponseCodeEnum.INTERNAL_ERROR, e);
        }
    }

    public static boolean isClientAbort(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String className = t.getClass().getName();

            // Tomcat / Undertow / Netty 等
            if (className.contains("ClientAbortException")) {
                return true;
            }

            String msg = t.getMessage();
            if (msg == null) continue;

            String m = msg.toLowerCase();

            // 各平台 / 代理常见断连信息
            if (m.contains("broken pipe")
                    || m.contains("connection reset")
                    || m.contains("connection aborted")
                    || m.contains("reset by peer")) {
                return true;
            }
        }
        return false;
    }
}
