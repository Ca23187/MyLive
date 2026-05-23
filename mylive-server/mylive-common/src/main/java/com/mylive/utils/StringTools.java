package com.mylive.utils;

import org.springframework.util.StringUtils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class StringTools {
    private StringTools() {}

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String RANDOM_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String getRandomString(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            int index = RANDOM.nextInt(RANDOM_CHARS.length());
            builder.append(RANDOM_CHARS.charAt(index));
        }
        return builder.toString();
    }

    public static String getRandomNumber(int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    public static List<String> parseCommaDistinctList(String input) {
        return parseDelimitedDistinctList(input, ",");
    }

    public static List<String> parseDelimitedDistinctList(String input, String delimiter) {
        if (!StringUtils.hasText(input)) {
            return List.of();
        }
        return Arrays.stream(input.split(delimiter))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    public static String getSuffix(String fileName) {
        if (fileName == null) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index <= 0 || index == fileName.length() - 1) {
            // 形如 ".gitignore" 或 "a." 都视为无后缀
            return "";
        }
        return fileName.substring(index);
    }

    // =================== 路径校验 ===================

    public static boolean isPathSegmentOk(String s) {
        if (!StringUtils.hasText(s)) return false;

        String v = s.trim();

        // 常见前端空值
        if ("null".equalsIgnoreCase(v) || "undefined".equalsIgnoreCase(v)) return false;

        // 单段不允许分隔符
        if (v.indexOf('/') >= 0 || v.indexOf('\\') >= 0) return false;

        // 长度限制
        if (v.length() > 180) return false;

        // 禁止当前目录 / 上级目录这种特殊段
        if (".".equals(v) || "..".equals(v)) return false;

        // 白名单：字母数字 . _ -
        if (!v.matches("[A-Za-z0-9][A-Za-z0-9._-]*")) return false;

        return true;
    }

    public static boolean isRelPathOk(String path) {
        if (!StringUtils.hasText(path)) return false;

        String v = path.trim();

        if ("null".equalsIgnoreCase(v) || "undefined".equalsIgnoreCase(v)) return false;

        // 相对路径可稍长一点
        if (v.length() > 300) return false;

        // 先统一分隔符
        v = v.replace('\\', '/');

        // 基础结构限制
        if (v.startsWith("/") || v.endsWith("/")) return false;
        if (v.contains("//")) return false;

        String[] parts = v.split("/");
        if (parts.length == 0) return false;

        // 先做 segment 白名单校验
        for (String seg : parts) {
            if (!isPathSegmentOk(seg)) return false;
        }

        // 再做规范化校验，防语义级绕过
        try {
            Path normalized = Paths.get(v).normalize();

            // 必须仍然是相对路径
            if (normalized.isAbsolute()) return false;

            // normalize 后不能跳到上级
            if (normalized.startsWith("..")) return false;

            // normalize 后不应为空
            String normalizedStr = normalized.toString().replace('\\', '/');
            if (!StringUtils.hasText(normalizedStr)) return false;

            // 可选：规范化后再做一次分段校验，确保结果仍符合白名单
            String[] normalizedParts = normalizedStr.split("/");
            for (String seg : normalizedParts) {
                if (!isPathSegmentOk(seg)) return false;
            }

            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }

    public static String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;
        return keyword.trim().toLowerCase(Locale.ROOT);
    }
}
