package org.example.back.service.common;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.config.MinioProperties;
import org.example.back.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.UUID;

/**
 * MinIO 文件服务类
 * 提供文件上传、删除、获取URL等功能
 * 支持头像上传、反馈图片上传、技能证书上传
 * 
 * @author 每天十点睡
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {
    
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    
    /** 允许的图片类型 */
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp"
    );
    
    /** 允许的文档类型（用于技能证书） */
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp",
            "application/pdf"
    );
    
    /** 单个文件最大大小：15MB */
    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024;
    
    /**
     * 文件类型枚举
     */
    public enum FileType {
        /** 用户头像 */
        AVATAR("avatar"),
        /** 反馈图片 */
        FEEDBACK("feedback"),
        /** 技能证书 */
        CERTIFICATE("certificate"),
        /** 工单完成图片 */
        REPORT("report");
        
        private final String prefix;
        
        FileType(String prefix) {
            this.prefix = prefix;
        }
        
        public String getPrefix() {
            return prefix;
        }
    }
    
    /**
     * 上传文件到 MinIO（通用方法）
     * 
     * @param file 上传的文件
     * @param userId 用户ID,用于生成唯一文件名
     * @return 文件访问路径
     */
    public String uploadFile(MultipartFile file, Long userId) {
        return uploadFile(file, userId, FileType.AVATAR);
    }
    
    /**
     * 上传文件到 MinIO（指定文件类型）
     * 
     * @param file 上传的文件
     * @param userId 用户ID,用于生成唯一文件名
     * @param fileType 文件类型
     * @return 文件访问路径
     */
    public String uploadFile(MultipartFile file, Long userId, FileType fileType) {
        // 验证文件
        validateFile(file, fileType);
        
        try {
            // 1. 确保 bucket 存在
            ensureBucketExists();
            
            // 2. 生成唯一文件名: type_userId_timestamp.扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = fileType.getPrefix() + "_" + userId + "_" + System.currentTimeMillis() + extension;
            
            // 3. 上传文件
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
            
            log.info("文件上传成功: type={}, fileName={}", fileType, fileName);
            
            // 4. 返回文件访问路径
            return "/" + minioProperties.getBucketName() + "/" + fileName;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传头像
     * 
     * @param file 头像文件
     * @param userId 用户ID
     * @return 文件访问路径
     */
    public String uploadAvatar(MultipartFile file, Long userId) {
        return uploadFile(file, userId, FileType.AVATAR);
    }
    
    /**
     * 上传反馈图片
     * 
     * @param file 图片文件
     * @param userId 用户ID
     * @return 文件访问路径
     */
    public String uploadFeedbackImage(MultipartFile file, Long userId) {
        return uploadFile(file, userId, FileType.FEEDBACK);
    }
    
    /**
     * 上传技能证书
     * 
     * @param file 证书文件（图片或PDF）
     * @param userId 用户ID
     * @return 文件访问路径
     */
    public String uploadCertificate(MultipartFile file, Long userId) {
        return uploadFile(file, userId, FileType.CERTIFICATE);
    }
    
    /**
     * 验证文件
     * 
     * @param file 文件
     * @param fileType 文件类型
     */
    private void validateFile(MultipartFile file, FileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        
        // 验证文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过15MB");
        }
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException("无法识别文件类型");
        }
        
        Set<String> allowedTypes = (fileType == FileType.CERTIFICATE) 
                ? ALLOWED_DOCUMENT_TYPES 
                : ALLOWED_IMAGE_TYPES;
        
        if (!allowedTypes.contains(contentType.toLowerCase())) {
            if (fileType == FileType.CERTIFICATE) {
                throw new BusinessException("仅支持图片格式(jpg/png/gif/webp/bmp)或PDF格式");
            } else {
                throw new BusinessException("仅支持图片格式(jpg/png/gif/webp/bmp)");
            }
        }
        
        // 验证文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException("文件名格式不正确");
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
     * 使用预签名URL,有效期7天
     * 
     * @param filePath 文件路径
     * @return 完整的访问 URL
     */
    public String getFileUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        
        try {
            // 从路径中提取文件名
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            
            // 生成预签名URL,有效期7天
            String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioProperties.getBucketName())
                    .object(fileName)
                    .expiry(7, java.util.concurrent.TimeUnit.DAYS)
                    .build()
            );
            
            return url;
        } catch (Exception e) {
            log.error("获取文件URL失败", e);
            // 降级方案:返回直接访问URL
            String webEndpoint = minioProperties.getEndpoint().replace("9090", "9000");
            return webEndpoint + filePath;
        }
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
