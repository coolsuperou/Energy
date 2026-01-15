package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 业务评论实体类
 * 用于用电申请、工单等业务对象的评论沟通
 * 支持多人在业务详情页进行留言交流
 */
@Data
@TableName("comments")
public class Comment {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 关联业务类型 application用电申请 task工单 */
    private String relatedType;
    
    /** 关联业务ID */
    private Long relatedId;
    
    /** 评论人用户ID */
    private Long userId;
    
    /** 评论内容 */
    private String content;
    
    /** 创建时间 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /** 评论人姓名 非数据库字段 用于前端显示 */
    @TableField(exist = false)
    private String userName;
}
