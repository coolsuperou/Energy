package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import org.example.back.entity.enums.ApplicationStatus;
import org.example.back.entity.enums.Urgency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用电申请实体
 */
@TableName("applications")
public class Application {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String applicationNo;
    private Long userId;
    private Long workshopId;
    private Long equipmentId;
    private BigDecimal power;
    private LocalDate applyDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;
    private Urgency urgency;
    private ApplicationStatus status;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String comment;
    private LocalTime adjustedStartTime;
    private LocalTime adjustedEndTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private String equipmentName;
    @TableField(exist = false)
    private String approverName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApplicationNo() { return applicationNo; }
    public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getWorkshopId() { return workshopId; }
    public void setWorkshopId(Long workshopId) { this.workshopId = workshopId; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public BigDecimal getPower() { return power; }
    public void setPower(BigDecimal power) { this.power = power; }
    public LocalDate getApplyDate() { return applyDate; }
    public void setApplyDate(LocalDate applyDate) { this.applyDate = applyDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public Urgency getUrgency() { return urgency; }
    public void setUrgency(Urgency urgency) { this.urgency = urgency; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalTime getAdjustedStartTime() { return adjustedStartTime; }
    public void setAdjustedStartTime(LocalTime adjustedStartTime) { this.adjustedStartTime = adjustedStartTime; }
    public LocalTime getAdjustedEndTime() { return adjustedEndTime; }
    public void setAdjustedEndTime(LocalTime adjustedEndTime) { this.adjustedEndTime = adjustedEndTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
}
