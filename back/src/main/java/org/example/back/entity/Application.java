package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.example.back.entity.enums.ApplicationStatus;
import org.example.back.entity.enums.Urgency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用电申请实体类
 * 车间用户提交的用电需求申请，包含设备、功率、时间段等信息
 * 需要经过调度员审批后才能生效并产生能耗数据
 */
@Data
@TableName("applications")
public class Application {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 申请编号 格式APP+时间戳 */
    private String applicationNo;
    
    /** 申请人用户ID */
    private Long userId;
    
    /** 车间ID */
    private Long workshopId;
    
    /** 设备ID */
    private Long equipmentId;
    
    /** 申请功率 单位kW */
    private BigDecimal power;
    
    /** 申请日期 */
    private LocalDate applyDate;
    
    /** 开始时间 */
    private LocalTime startTime;
    
    /** 结束时间 */
    private LocalTime endTime;
    
    /** 用电用途说明 */
    private String purpose;
    
    /** 紧急程度 NORMAL普通 URGENT紧急 CRITICAL严重 */
    private Urgency urgency;
    
    /** 申请状态 PENDING待审批 APPROVED已批准 REJECTED已拒绝 */
    private ApplicationStatus status;
    
    /** 审批人用户ID */
    private Long approvedBy;
    
    /** 审批时间 */
    private LocalDateTime approvedAt;
    
    /** 审批意见或拒绝原因 */
    private String comment;
    
    /** 调整后的开始时间 调度员可调整时段 */
    private LocalTime adjustedStartTime;
    
    /** 调整后的结束时间 调度员可调整时段 */
    private LocalTime adjustedEndTime;
    
    /** 创建时间 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /** 申请人姓名 非数据库字段 用于前端显示 */
    @TableField(exist = false)
    private String userName;
    
    /** 设备名称 非数据库字段 用于前端显示 */
    @TableField(exist = false)
    private String equipmentName;
    
    /** 审批人姓名 非数据库字段 用于前端显示 */
    @TableField(exist = false)
    private String approverName;
}
