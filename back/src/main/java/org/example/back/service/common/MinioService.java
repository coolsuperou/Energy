package org.example.back.service.common;

import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.config.MinioProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * MinIO 文件服务类
 * 提供文件上传、删除、获取URL等功能
 * 
 * @author 每天十点睡
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {
    
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    
    /**
     * 上传文件到 MinIO
     * 
     * @param file 上传的文件
     * @param userId 用户ID,用于生成唯一文件名
     * @return 文件访问路径
     */
    public String uploadFile(MultipartFile file, Long userId) {
        try {
            // 1. 确保 bucket 存在
            ensureBucketExists();
            
            // 2. 生成唯一文件名: userId_timestamp.扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = userId + "_" + System.currentTimeMillis() + extension;
            
            // 3. 上传文件
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            log.info("文件上传成功: {}", fileName);
            
            // 4. 返回文件访问路径
            return "/" + minioProperties.getBucketName() + "/" + fileName;
            
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除文件
     * 
     * @param filePath 文件路径,格式: /bucket/filename
     */
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        
        try {
            // 从路径中提取文件名: /avatars/123_1234567890.jpg -> 123_1234567890.jpg
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(fileName)
                    .build()
            );
            
            log.info("文件删除成功: {}", fileName);
            
        } catch (Exception e) {
            log.error("文件删除失败", e);
            // 删除失败不抛异常,避免影响主流程
        }
    }
    
    /**
     * 获取文件访问 URL
     * 
     * @param filePath 文件路径
     * @return 完整的访问 URL
     */
    public String getFileUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        
        // MinIO 访问地址: http://127.0.0.1:9000/avatars/123_1234567890.jpg
        // 注意: 这里使用 9000 端口(WebUI端口),因为浏览器通过这个端口访问文件
        String webEndpoint = minioProperties.getEndpoint().replace("9090", "9000");
        return webEndpoint + filePath;
    }
    
    /**
     * 确保存储桶存在,不存在则创建
     */
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder()
                .bucket(minioProperties.getBucketName())
                .build()
        );
        
        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .build()
            );
            
            // 设置 bucket 为公开访问(允许匿名读取)
            String policy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {"AWS": ["*"]},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                        }
                    ]
                }
                """.formatted(minioProperties.getBucketName());
            
            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .config(policy)
                    .build()
            );
            
            log.info("创建存储桶: {}", minioProperties.getBucketName());
        }
    }
}
