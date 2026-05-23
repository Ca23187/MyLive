package com.mylive.infra.jpa.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class TranscodeResult {
    private int duration;
    private long fileSize;
    private String filePath;
}
