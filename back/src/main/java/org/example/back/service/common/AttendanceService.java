package org.example.back.service.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.back.entity.AttendanceRecord;
import org.example.back.entity.User;
import org.example.back.entity.enums.AttendanceStatus;
import org.example.back.entity.enums.ShiftType;
import org.example.back.mapper.common.AttendanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 考勤服务类
 */
@Service
public class AttendanceService {

    @Autowired
    private AttendanceMapper attendanceMapper;

    private static final LocalTime WORK_START_TIME = LocalTime.of(8, 30); // 上班时间 8:30
    private static final LocalTime WORK_END_TIME = LocalTime.of(17, 30);   // 下班时间 17:30
    private static final int LATE_THRESHOLD_MINUTES = 10; // 迟到阈值 10分钟

    /**
     * 打卡
     */
    public AttendanceRecord clockIn(User user, String type) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        AttendanceRecord record = attendanceMapper.getTodayAttendance(user.getId(), today);

        if (record == null) {
            // 首次打卡（上班）
            record = new AttendanceRecord();
            record.setUserId(user.getId());
            record.setAttendanceDate(today);
            record.setShiftType(ShiftType.DAY); // 默认白班
            record.setScheduledStartTime(WORK_START_TIME);
            record.setScheduledEndTime(WORK_END_TIME);
            record.setClockInTime(now);

            // 判断是否迟到
            LocalTime scheduledStart = record.getScheduledStartTime() != null ? record.getScheduledStartTime() : WORK_START_TIME;
            if (now.isAfter(scheduledStart.plusMinutes(LATE_THRESHOLD_MINUTES))) {
                record.setStatus(AttendanceStatus.LATE);
            } else {
                record.setStatus(AttendanceStatus.NORMAL);
            }

            attendanceMapper.insert(record);
        } else if (record.getClockOutTime() == null) {
            // 第二次打卡（下班）
            record.setClockOutTime(now);

            // 计算工作时长
            if (record.getClockInTime() != null) {
                Duration duration = Duration.between(record.getClockInTime(), now);
                BigDecimal hours = BigDecimal.valueOf(duration.toMinutes()).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
                record.setWorkHours(hours);
            }

            // 判断是否早退
            LocalTime scheduledEnd = record.getScheduledEndTime() != null ? record.getScheduledEndTime() : WORK_END_TIME;
            if (now.isBefore(scheduledEnd) && record.getStatus() == AttendanceStatus.NORMAL) {
                record.setStatus(AttendanceStatus.EARLY_LEAVE);
            }

            attendanceMapper.updateById(record);
        }

        return record;
    }

    /**
     * 获取今日考勤
     */
    public AttendanceRecord getTodayAttendance(User user) {
        LocalDate today = LocalDate.now();
        return attendanceMapper.getTodayAttendance(user.getId(), today);
    }

    /**
     * 获取月度考勤记录
     * 结合排班数据修正状态：过去日期有排班无打卡→缺勤，未来日期保留排班状态
     */
    public List<AttendanceRecord> getMonthlyAttendance(User user, int year, int month) {
        List<AttendanceRecord> records = attendanceMapper.getMonthlyAttendance(user.getId(), year, month);
        LocalDate today = LocalDate.now();
        
        for (AttendanceRecord record : records) {
            if (record.getAttendanceDate() == null) continue;
            
            boolean isPast = record.getAttendanceDate().isBefore(today);
            boolean isToday = record.getAttendanceDate().isEqual(today);
            boolean hasClockIn = record.getClockInTime() != null;
            boolean isRest = record.getShiftType() == ShiftType.REST;
            
            if (isRest && !hasClockIn) {
                // 排班为休息且没有打卡 → 休息
                record.setStatus(AttendanceStatus.REST);
            } else if (!isRest && isPast && !hasClockIn) {
                // 非休息日、过去的日期、没有打卡 → 缺勤
                record.setStatus(AttendanceStatus.ABSENT);
            } else if (!isRest && isToday && !hasClockIn) {
                // 非休息日、今天、还没打卡 → 保持当前状态
            }
            // 有打卡的记录保持原有状态（NORMAL/LATE/EARLY_LEAVE等）
            // 未来日期保留排班状态，前端根据日期判断显示
        }
        
        return records;
    }

    /**
     * 获取月度考勤统计
     * 基于修正后的月度数据统计，确保排班+打卡逻辑一致
     */
    public Map<String, Object> getAttendanceStats(User user, int year, int month) {
        // 复用getMonthlyAttendance的修正逻辑
        List<AttendanceRecord> records = getMonthlyAttendance(user, year, month);
        LocalDate today = LocalDate.now();
        
        int normalDays = 0, lateDays = 0, absentDays = 0, restDays = 0, earlyLeaveDays = 0, holidayDays = 0;
        
        for (AttendanceRecord record : records) {
            if (record.getAttendanceDate() == null) continue;
            // 只统计今天及之前的日期
            if (record.getAttendanceDate().isAfter(today)) continue;
            
            if (record.getStatus() == null) continue;
            switch (record.getStatus()) {
                case NORMAL: normalDays++; break;
                case LATE: lateDays++; break;
                case ABSENT: absentDays++; break;
                case REST: restDays++; break;
                case EARLY_LEAVE: earlyLeaveDays++; break;
                case HOLIDAY: holidayDays++; break;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("normalDays", normalDays);
        result.put("lateDays", lateDays);
        result.put("absentDays", absentDays);
        result.put("restDays", restDays);
        result.put("earlyLeaveDays", earlyLeaveDays);
        result.put("holiday", holidayDays);

        return result;
    }

    /**
     * 获取本周排班
     */
    public List<AttendanceRecord> getWeeklySchedule(User user) {
        LocalDate today = LocalDate.now();
        // 获取本周一
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        // 获取本周日
        LocalDate sunday = monday.plusDays(6);
        
        return attendanceMapper.getWeeklySchedule(user.getId(), monday, sunday);
    }

    /**
     * 获取月度排班记录（不修正状态，返回原始排班数据）
     */
    public List<AttendanceRecord> getMonthlySchedule(User user, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return attendanceMapper.getMonthlySchedule(user.getId(), startDate, endDate);
    }

    /**
     * 创建或更新排班记录
     */
    public void createOrUpdateSchedule(Long userId, LocalDate date, ShiftType shiftType, 
                                       LocalTime startTime, LocalTime endTime) {
        AttendanceRecord record = attendanceMapper.getTodayAttendance(userId, date);
        
        if (record == null) {
            // 创建新排班记录
            record = new AttendanceRecord();
            record.setUserId(userId);
            record.setAttendanceDate(date);
            record.setShiftType(shiftType);
            record.setScheduledStartTime(startTime);
            record.setScheduledEndTime(endTime);
            // 不设置 status，status 只由打卡决定，前端根据 shiftType 判断休息日
            
            attendanceMapper.insert(record);
        } else {
            // 更新排班信息
            record.setShiftType(shiftType);
            record.setScheduledStartTime(startTime);
            record.setScheduledEndTime(endTime);
            // 不操作 status，status 只由打卡决定
            
            attendanceMapper.updateById(record);
        }
    }

    /**
     * 批量创建排班（用于初始化数据）
     */
    public void batchCreateSchedule(Long userId, List<AttendanceRecord> schedules) {
        for (AttendanceRecord schedule : schedules) {
            createOrUpdateSchedule(userId, schedule.getAttendanceDate(), 
                schedule.getShiftType(), 
                schedule.getScheduledStartTime(), 
                schedule.getScheduledEndTime());
        }
    }
}
