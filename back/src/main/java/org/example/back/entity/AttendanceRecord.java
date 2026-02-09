package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.example.back.entity.enums.AttendanceStatus;
import org.example.back.entity.enums.ShiftType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 考勤排班记录实体类
 * 整合员工每日的排班信息和打卡考勤情况
 */
@Data
@TableName("attendance_records")
public class AttendanceRecord {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 考勤日期 */
    private LocalDate attendanceDate;

    // ========== 排班信息 ==========
    
    /** 班次类型 DAY白班 NIGHT夜班 REST休息 */
    private ShiftType shiftType;

    /** 计划上班时间 */
    private LocalTime scheduledStartTime;

    /** 计划下班时间 */
    private LocalTime scheduledEndTime;

    // ========== 打卡信息 ==========
    
    /** 实际上班打卡时间 */
    private LocalTime clockInTime;

    /** 实际下班打卡时间 */
    private LocalTime clockOutTime;

    // ========== 考勤状态 ==========
    
    /** 考勤状态 NORMAL正常 LATE迟到 EARLY_LEAVE早退 ABSENT缺勤 REST休息 HOLIDAY假期 */
    private AttendanceStatus status;

    /** 工作时长(小时) */
    private BigDecimal workHours;

    /** 备注 */
    private String remark;

    /** 创建时间 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
