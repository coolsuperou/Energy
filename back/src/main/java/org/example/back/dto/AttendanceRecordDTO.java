package org.example.back.dto;

import org.example.back.entity.AttendanceRecord;
import org.example.back.entity.enums.AttendanceStatus;
import org.example.back.entity.enums.ShiftType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 考勤记录 DTO
 * 用于前端展示，字段名称与前端期望一致
 */
public class AttendanceRecordDTO {

    private Long id;
    private Long userId;
    private String date;  // 前端期望的字段名
    private String shiftType;
    private String scheduledStartTime;
    private String scheduledEndTime;
    private String clockInTime;
    private String clockOutTime;
    private String status;  // 大写状态值
    private BigDecimal workHours;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }
    public String getScheduledStartTime() { return scheduledStartTime; }
    public void setScheduledStartTime(String scheduledStartTime) { this.scheduledStartTime = scheduledStartTime; }
    public String getScheduledEndTime() { return scheduledEndTime; }
    public void setScheduledEndTime(String scheduledEndTime) { this.scheduledEndTime = scheduledEndTime; }
    public String getClockInTime() { return clockInTime; }
    public void setClockInTime(String clockInTime) { this.clockInTime = clockInTime; }
    public String getClockOutTime() { return clockOutTime; }
    public void setClockOutTime(String clockOutTime) { this.clockOutTime = clockOutTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getWorkHours() { return workHours; }
    public void setWorkHours(BigDecimal workHours) { this.workHours = workHours; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public static AttendanceRecordDTO fromEntity(AttendanceRecord record) {
        if (record == null) return null;
        
        AttendanceRecordDTO dto = new AttendanceRecordDTO();
        dto.setId(record.getId());
        dto.setUserId(record.getUserId());
        dto.setDate(record.getAttendanceDate() != null ? record.getAttendanceDate().toString() : null);
        dto.setShiftType(record.getShiftType() != null ? record.getShiftType().name().toUpperCase() : null);
        dto.setScheduledStartTime(formatTime(record.getScheduledStartTime()));
        dto.setScheduledEndTime(formatTime(record.getScheduledEndTime()));
        dto.setClockInTime(formatTime(record.getClockInTime()));
        dto.setClockOutTime(formatTime(record.getClockOutTime()));
        dto.setStatus(record.getStatus() != null ? record.getStatus().name().toUpperCase() : null);
        dto.setWorkHours(record.getWorkHours());
        dto.setRemark(record.getRemark());
        
        return dto;
    }

    private static String formatTime(LocalTime time) {
        return time != null ? time.toString() : null;
    }
}
