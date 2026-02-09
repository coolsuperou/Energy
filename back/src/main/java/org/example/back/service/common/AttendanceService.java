package org.example.back.service.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.back.entity.AttendanceRecord;
import org.example.back.entity.User;
import org.example.back.entity.enums.AttendanceStatus;
import org.example.back.entity.enums.ShiftType;
import org.example.back.mapper.AttendanceMapper;
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
                BigDecimal hours = BigDecimal.valueOf(duration.toMinutes()).divide(BigDecimal.valueOf(60), 2, BigDecimal.ROUND_HALF_UP);
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
     */
    public List<AttendanceRecord> getMonthlyAttendance(User user, int year, int month) {
        return attendanceMapper.getMonthlyAttendance(user.getId(), year, month);
    }

    /**
     * 获取月度考勤统计
     */
    public Map<String, Object> getAttendanceStats(User user, int year, int month) {
        List<Map<String, Object>> stats = attendanceMapper.getMonthlyStats(user.getId(), year, month);

        Map<String, Object> result = new HashMap<>();
        result.put("normalDays", 0);
        result.put("lateDays", 0);
        result.put("absentDays", 0);
        result.put("restDays", 0);
        result.put("earlyLeave", 0);
        result.put("holiday", 0);

        for (Map<String, Object> stat : stats) {
            String status = (String) stat.get("status");
            Long count = (Long) stat.get("count");

            switch (status) {
                case "normal":
                    result.put("normalDays", count.intValue());
                    break;
                case "late":
                    result.put("lateDays", count.intValue());
                    break;
                case "absent":
                    result.put("absentDays", count.intValue());
                    break;
                case "rest":
                    result.put("restDays", count.intValue());
                    break;
                case "early_leave":
                    result.put("earlyLeave", count.intValue());
                    break;
                case "holiday":
                    result.put("holiday", count.intValue());
                    break;
            }
        }

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
            
            // 如果是休息日，设置状态为休息
            if (shiftType == ShiftType.REST) {
                record.setStatus(AttendanceStatus.REST);
            }
            
            attendanceMapper.insert(record);
        } else {
            // 更新排班信息
            record.setShiftType(shiftType);
            record.setScheduledStartTime(startTime);
            record.setScheduledEndTime(endTime);
            
            if (shiftType == ShiftType.REST && record.getClockInTime() == null) {
                record.setStatus(AttendanceStatus.REST);
            }
            
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
