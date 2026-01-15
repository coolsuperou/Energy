package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息通知实体类
 * 系统自动触发的业务通知
 * 包括申请审批通知、任务分配通知、预警通知等
 */
@Data
@TableName("notifications")
public class Notification {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 接收人用户ID */
    private Long userId;
    
    /** 通知类型 approval审批通知 task任务通知 alert预警通知 system系统通知 */
    private String type;
    
    /** 通知标题 */
    private String title;
    
    /** 通知内容 */
    private String content;
    
    /** 关联业务ID 如申请ID、任务ID等 */
    private Long relatedId;
    
    /** 是否已读 */
    private Boolean isRead;
    
    /** 创建时间 */
    private LocalDateTime createdAt;
}
