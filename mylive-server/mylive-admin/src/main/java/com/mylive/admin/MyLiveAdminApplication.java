package com.mylive.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.mylive"})
@EnableJpaRepositories(basePackages = "com.mylive.infra.jpa.repository")
@EntityScan(basePackages = "com.mylive.infra.jpa.entity")
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
@EnableJpaAuditing
@EnableTransactionManagement
public class MyLiveAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyLiveAdminApplication.class, args);
    }
}