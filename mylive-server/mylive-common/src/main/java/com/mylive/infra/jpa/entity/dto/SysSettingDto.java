package com.mylive.infra.jpa.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysSettingDto implements Serializable {
    private Integer registerCoinCount = 10;
    private Integer postVideoCoinCount = 5;
    private Integer videoSize = 1024;
    private Integer videoPartCount = 10;
    private Integer videoCount = 10;
    private Integer commentCount = 20;
    private Integer danmakuCount = 20;

    private String registerEmailTitle = "Welcome to MyLive";

    private String registerEmailContent = """
        <div style="font-family: Arial, sans-serif; font-size: 14px; color: #333;">
            <p>Hello,</p>

            <p>You are registering for <b>MyLive</b>.</p>

            <p>Your email verification code is:</p>

            <p style="font-size: 20px; font-weight: bold; color: #1a73e8;">
                %s
            </p>

            <p>This code is valid for 15 minutes.</p>

            <p>If you did not request this, please ignore this email.</p>
        </div>
        """;
}
