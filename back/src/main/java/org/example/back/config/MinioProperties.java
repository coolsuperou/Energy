package org.example.back.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 配置属性类
 * 从 application.yml 中读取 minio 配置
 * 
 * @author 欧展煌
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    
    /** MinIO 服务器地址 */
    private String endpoint;
    
    /** 访问密钥 */
    private String accessKey;
    
    /** 密钥 */
    private String secretKey;
    
    /** 存储桶名称 */
    private String bucketName;
}
