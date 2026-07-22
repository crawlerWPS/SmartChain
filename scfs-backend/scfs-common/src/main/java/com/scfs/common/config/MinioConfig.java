package com.scfs.common.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置 - 对应 RFC 4.1.3 FileStorageService
 */
@Configuration
public class MinioConfig {

    @Value("${scfs.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${scfs.minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${scfs.minio.secret-key:minioadmin}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
