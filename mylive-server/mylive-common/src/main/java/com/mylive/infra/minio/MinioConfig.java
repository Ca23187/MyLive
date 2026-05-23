package com.mylive.infra.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnProperty(
        prefix = "storage",
        name = "type",
        havingValue = "minio"
)
@Slf4j
public class MinioConfig {

    private final MinioProperties minioProperties;

    public MinioConfig(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    @Bean
    public OkHttpClient minioOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofMinutes(5))
                .writeTimeout(Duration.ofMinutes(5))
                .callTimeout(Duration.ofMinutes(10))
                .build();
    }

    @Bean
    public MinioClient minioClient(MinioProperties properties, OkHttpClient minioOkHttpClient) {
        MinioClient client =  MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .httpClient(minioOkHttpClient)
                .build();
        ensureMinioBucketExists(client);
        return client;
    }

    @Bean
    public MinioAsyncClient minioAsyncClient(MinioProperties properties, OkHttpClient minioOkHttpClient) {
        return MinioAsyncClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .httpClient(minioOkHttpClient)
                .build();
    }

    @PostConstruct
    private void ensureMinioBucketExists(MinioClient minioClient) {
        if (!minioProperties.isAutoCreateBucket()) {
            return;
        }
        String bucketName = minioProperties.getBucketName();
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("MinIO bucket created: {}", bucketName);
            }
            log.info("MinIO bucket check success");
        } catch (Exception e) {
            throw new IllegalStateException("MinIO bucket initialization failed: " + bucketName, e);
        }
    }
}
