package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.example.back.entity.enums.Priority;
import org.example.back.entity.enums.TaskStatus;
import org.example.back.entity.enums.TaskType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工单任务实体类
 * 调度员创建并分配给巡检员的设备巡检、维修、维护任务
 * 巡检员完成任务后提交巡检报告
 */
@Data
@TableName("tasks")
public class Task {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 任务类型 INSPECTION巡检 REPAIR维修 MAINTENANCE维护 */
    private TaskType taskType;
    
    /** 任务标题 */
    private String title;
    
    /** 任务描述 */
    private String description;
    
    /** 关联设备ID */
    private Long equipmentId;
    
    /** 关联反馈ID（反馈转工单时使用，便于追溯） */
    private Long feedbackId;
    
    /** 分配给谁 巡检员用户ID */
    private Long assignedTo;
    
    /** 分配人 调度员用户ID */
    private Long assignedBy;
    
    /** 优先级 NORMAL普通 HIGH高 URGENT紧急 CRITICAL严重 */
    private Priority priority;
    
    /** 任务状态 PENDING待处理 IN_PROGRESS进行中 COMPLETED已完成 */
    private TaskStatus status;
    
    /** 截止日期 */
    private LocalDate dueDate;
    
    /** 完成时间 */
    private LocalDateTime completedAt;
    
    /** 巡检报告内容 */
    private String report;
    
    /** 完成工单时上传的图片URL（多个用逗号分隔） */
    private String reportImages;
    
    /** 创建时间 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /** 设备名称 非数据库字段 用于前端显示 */
    @TableField(exist = false)
    private String equipmentName;
    
    /** 处理人姓名 非数据库字段 用于前端显示 */
    @TableField(exist = false)
    private String assigneeName;
    
    /** 分配人姓名 非数据库字段 用于前端显示 */
    @TableField(exist = false)
    private String assignerName;
}
