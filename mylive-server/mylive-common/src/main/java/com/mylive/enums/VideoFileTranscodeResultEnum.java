package com.mylive.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VideoFileTranscodeResultEnum {
    TRANSCODING(0, "转码中"),
    SUCCESS(1, "转码成功"),
    FAIL(2, "转码失败");
    private final Integer status;
    private final String desc;
}