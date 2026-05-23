package com.mylive.web;

import com.mylive.infra.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final RedisUtils redisUtils;

    @Override
    public void run(ApplicationArguments args) {
        checkDatabase();
        checkRedis();
        log.info("Application startup initialization completed");
    }

    private void checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(3)) {
                throw new IllegalStateException("Database connection invalid");
            }
            log.info("Database connection success");
        } catch (Exception e) {
            throw new IllegalStateException("Database startup check failed", e);
        }
    }

    private void checkRedis() {
        try {
            redisUtils.get("startup:check");
            log.info("Redis connection success");
        } catch (Exception e) {
            throw new IllegalStateException("Redis startup check failed", e);
        }
    }


}