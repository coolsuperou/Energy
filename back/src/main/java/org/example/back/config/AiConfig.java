package org.example.back.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 讯飞星火大模型配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.spark")
public class AiConfig {
    
    /** 应用ID */
    private String appId;
    
    /** API Key */
    private String apiKey;
    
    /** API Secret */
    private String apiSecret;
    
    /** WebSocket API 地址 */
    private String hostUrl = "wss://spark-api.xf-yun.com/v1/x1";
    
    /** 模型域名 */
    private String domain = "spark-x";
}
