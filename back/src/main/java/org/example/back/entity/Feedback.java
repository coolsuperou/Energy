package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题反馈实体类
 * 车间用户提交的设备故障、建议、问题等反馈
 * 由调度员或管理员处理并回复
 */
@Data
@TableName("feedbacks")
public class Feedback {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 反馈编号 格式FB+时间戳 */
    private String feedbackNo;
    
    /** 反馈人用户ID */
    private Long userId;
    
    /** 反馈类型 fault故障 suggestion建议 question问题 */
    private String type;
    
    /** 问题位置 */
    private String location;
    
    /** 紧急程度 normal普通 urgent紧急 */
    private String urgency;
    
    /** 问题描述 */
    private String description;
    
    /** 处理状态 pending待处理 processing处理中 resolved已解决 */
    private String status;
    
    /** 处理回复 */
    private String reply;
    
    /** 处理人用户ID */
    private Long handledBy;
    
    /** 处理时间 */
    private LocalDateTime handledAt;
    
    /** 创建时间 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    /** 更新时间 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    /** 反馈人姓名 非数据库字段 用于前端显示 */
    @TableField(exist = false)
    private String userName;
    
    /** 处理人姓名 非数据库字段 用于前端显示 */
    @TableField(exist = false)
    private String handlerName;
}
