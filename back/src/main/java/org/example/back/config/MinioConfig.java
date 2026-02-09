package org.example.back.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 配置类
 * 创建 MinioClient Bean 用于文件操作
 * 
 * @author 欧展煌
 */
@Configuration
@RequiredArgsConstructor
public class MinioConfig {
    
    private final MinioProperties minioProperties;
    
    /**
     * 创建 MinioClient 客户端
     * 用于连接 MinIO 服务器进行文件上传下载等操作
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }
}
