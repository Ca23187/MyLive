package com.mylive.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String projectFolder;
    private String adminAccount;
    private String adminPassword;
    private final Mail mail = new Mail();

    @Getter
    @Setter
    public static class Mail {
        /** 发件人 */
        private String from;
    }
}