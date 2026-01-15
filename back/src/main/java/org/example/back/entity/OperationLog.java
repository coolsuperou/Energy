package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 记录系统管理员和重要操作的日志
 * 用于审计和安全追踪
 */
@Data
@TableName("operation_logs")
public class OperationLog {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 操作人用户ID */
    private Long userId;
    
    /** 操作类型 如login登录 create创建 update更新 delete删除 */
    private String operationType;
    
    /** 操作描述 */
    private String operationDesc;
    
    /** 操作IP地址 */
    private String ipAddress;
    
    /** 创建时间 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /** 操作人姓名 非数据库字段 用于前端显示 */
    @TableField(exist = false)
    private String userName;
}
