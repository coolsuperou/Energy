package org.example.back.service.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.entity.AttendanceRecord;
import org.example.back.entity.User;
import org.example.back.entity.enums.AttendanceStatus;
import org.example.back.entity.enums.ShiftType;
import org.example.back.entity.enums.UserRole;
import org.example.back.exception.BusinessException;
import org.example.back.exception.ErrorCode;
import org.example.back.mapper.common.AttendanceMapper;
import org.example.back.mapper.common.UserMapper;
import org.example.back.service.common.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 排班考勤管理服务
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	经理排班考勤管理逻辑（排班生成、调整、考勤查看、补卡）
 * -- =============================================
 */
@Service
public class ScheduleAttendanceService {

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    // 白班时间
    private static final LocalTime DAY_SHIFT_START = LocalTime.of(8, 30);
    private static final LocalTime DAY_SHIFT_END = LocalTime.of(17, 30);
    
    // 夜班时间
    private static final LocalTime NIGHT_SHIFT_START = LocalTime.of(20, 0);
    private static final LocalTime NIGHT_SHIFT_END = LocalTime.of(8, 0);

    /**
     * 自动生成月度排班计划
     * 
     * @param year 年份
     * @param month 月份
     * @return 生成的排班记录数
     */
    @Transactional
    public int generateMonthlySchedule(int year, int month) {
        // 获取除经理外的所有活跃用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.ne(User::getRole, UserRole.MANAGER);
        userWrapper.eq(User::getStatus, "active");
        List<User> users = userMapper.selectList(userWrapper);

        if (users.isEmpty()) {
            return 0;
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        int generatedCount = 0;
        
        // 用于巡检员轮班的计数器
        Map<Long, Integer> inspectorShiftCounter = new HashMap<>();

        for (User user : users) {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                // 检查是否已存在排班记录
                if (attendanceMapper.countByUserAndDate(user.getId(), date) > 0) {
                    continue;
                }

                AttendanceRecord record = new AttendanceRecord();
                record.setUserId(user.getId());
                record.setAttendanceDate(date);

                // 根据角色和日期设置班次
                DayOfWeek dayOfWeek = date.getDayOfWeek();
                boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;

                if (isWeekend) {
                    // 周末默认休息，只设置 shiftType，status 保持 null
                    // 前端根据 shiftType 判断休息日，status 只记录打卡结果
                    record.setShiftType(ShiftType.REST);
                } else if (user.getRole() == UserRole.INSPECTOR) {
                    // 巡检员按白班/夜班轮换
                    int counter = inspectorShiftCounter.getOrDefault(user.getId(), 0);
                    if (counter % 2 == 0) {
                        record.setShiftType(ShiftType.DAY);
                        record.setScheduledStartTime(DAY_SHIFT_START);
                        record.setScheduledEndTime(DAY_SHIFT_END);
                    } else {
                        record.setShiftType(ShiftType.NIGHT);
                        record.setScheduledStartTime(NIGHT_SHIFT_START);
                        record.setScheduledEndTime(NIGHT_SHIFT_END);
                    }
                    inspectorShiftCounter.put(user.getId(), counter + 1);
                } else {
                    // 其他角色工作日默认白班
                    record.setShiftType(ShiftType.DAY);
                    record.setScheduledStartTime(DAY_SHIFT_START);
                    record.setScheduledEndTime(DAY_SHIFT_END);
                }

                attendanceMapper.insert(record);
                generatedCount++;
            }
        }

        return generatedCount;
    }


    /**
     * 获取所有人员的排班表
     * 
     * @param page 当前页
     * @param size 每页大小
     * @param year 年份
     * @param month 月份
     * @param role 角色筛选（可选）
     * @param department 部门筛选（可选）
     * @return 分页排班列表（包含用户信息）
     */
    public IPage<Map<String, Object>> getScheduleList(int page, int size, int year, int month, 
                                                       String role, String department) {
        // 先获取符合条件的用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.ne(User::getRole, UserRole.MANAGER);
        
        if (StringUtils.hasText(role)) {
            try {
                UserRole userRole = UserRole.valueOf(role.toUpperCase());
                userWrapper.eq(User::getRole, userRole);
            } catch (IllegalArgumentException e) {
                // 忽略无效的角色参数
            }
        }
        
        if (StringUtils.hasText(department)) {
            userWrapper.eq(User::getDepartment, department);
        }
        
        List<User> users = userMapper.selectList(userWrapper);
        
        if (users.isEmpty()) {
            Page<Map<String, Object>> emptyPage = new Page<>(page, size);
            emptyPage.setTotal(0);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }

        // 分页处理用户
        int total = users.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        
        if (start >= total) {
            Page<Map<String, Object>> emptyPage = new Page<>(page, size);
            emptyPage.setTotal(total);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }
        
        List<User> pagedUsers = users.subList(start, end);
        
        // 获取每个用户的月度排班
        List<Map<String, Object>> records = new ArrayList<>();
        for (User user : pagedUsers) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", user.getId());
            item.put("userName", user.getName());
            item.put("role", user.getRole());
            item.put("department", user.getDepartment());
            
            // 获取该用户的月度排班
            List<AttendanceRecord> schedules = attendanceMapper.getMonthlyAttendance(user.getId(), year, month);
            item.put("schedules", schedules);
            
            records.add(item);
        }
        
        Page<Map<String, Object>> resultPage = new Page<>(page, size);
        resultPage.setTotal(total);
        resultPage.setRecords(records);
        
        return resultPage;
    }

    /**
     * 调整排班
     * 
     * @param recordId 排班记录ID
     * @param shiftType 新班次类型
     * @param scheduledStartTime 新计划上班时间
     * @param scheduledEndTime 新计划下班时间
     * @param managerId 操作经理ID
     * @return 更新后的排班记录
     */
    @Transactional
    public AttendanceRecord adjustSchedule(Long recordId, ShiftType shiftType, 
                                           LocalTime scheduledStartTime, LocalTime scheduledEndTime,
                                           Long managerId) {
        AttendanceRecord record = attendanceMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 记录原班次用于通知
        ShiftType oldShiftType = record.getShiftType();
        
        // 更新排班信息
        record.setShiftType(shiftType);
        
        if (shiftType == ShiftType.REST) {
            record.setScheduledStartTime(null);
            record.setScheduledEndTime(null);
            // 不设置 status，status 只由打卡决定
        } else {
            record.setScheduledStartTime(scheduledStartTime != null ? scheduledStartTime : 
                    (shiftType == ShiftType.DAY ? DAY_SHIFT_START : NIGHT_SHIFT_START));
            record.setScheduledEndTime(scheduledEndTime != null ? scheduledEndTime : 
                    (shiftType == ShiftType.DAY ? DAY_SHIFT_END : NIGHT_SHIFT_END));
            // 不操作 status，status 只由打卡决定
        }
        
        attendanceMapper.updateById(record);
        
        // 发送通知给被调整的人员
        User user = userMapper.selectById(record.getUserId());
        if (user != null) {
            String title = "排班调整通知";
            String content = String.format("您 %s 的排班已调整：%s → %s", 
                    record.getAttendanceDate(), 
                    getShiftTypeName(oldShiftType), 
                    getShiftTypeName(shiftType));
            notificationService.createNotification(user.getId(), "system", title, content, recordId);
        }
        
        return record;
    }

    /**
     * 获取所有人员的考勤记录
     * 
     * @param page 当前页
     * @param size 每页大小
     * @param userId 用户ID筛选（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param status 考勤状态筛选（可选）
     * @return 分页考勤记录列表
     */
    public IPage<Map<String, Object>> getAttendanceList(int page, int size, Long userId,
                                                         LocalDate startDate, LocalDate endDate,
                                                         String status) {
        Page<AttendanceRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        
        // 用户筛选
        if (userId != null) {
            wrapper.eq(AttendanceRecord::getUserId, userId);
        }
        
        // 日期范围筛选
        if (startDate != null) {
            wrapper.ge(AttendanceRecord::getAttendanceDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(AttendanceRecord::getAttendanceDate, endDate);
        }
        
        // 状态筛选
        if (StringUtils.hasText(status)) {
            try {
                AttendanceStatus attendanceStatus = AttendanceStatus.valueOf(status.toUpperCase());
                wrapper.eq(AttendanceRecord::getStatus, attendanceStatus);
            } catch (IllegalArgumentException e) {
                // 忽略无效的状态参数
            }
        }
        
        // 只查询今天及之前的记录（未来排班不显示在考勤列表）
        wrapper.le(AttendanceRecord::getAttendanceDate, LocalDate.now());
        
        // 只查询有打卡记录或非休息状态的记录
        wrapper.and(w -> w.isNotNull(AttendanceRecord::getClockInTime)
                .or().isNotNull(AttendanceRecord::getClockOutTime)
                .or().ne(AttendanceRecord::getShiftType, ShiftType.REST));
        
        wrapper.orderByDesc(AttendanceRecord::getAttendanceDate);
        
        IPage<AttendanceRecord> recordPage = attendanceMapper.selectPage(pageParam, wrapper);
        
        // 转换为包含用户信息的结果
        Page<Map<String, Object>> resultPage = new Page<>(page, size);
        resultPage.setTotal(recordPage.getTotal());
        resultPage.setPages(recordPage.getPages());
        
        List<Map<String, Object>> records = recordPage.getRecords().stream().map(record -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("userId", record.getUserId());
            item.put("attendanceDate", record.getAttendanceDate());
            item.put("shiftType", record.getShiftType());
            item.put("scheduledStartTime", record.getScheduledStartTime());
            item.put("scheduledEndTime", record.getScheduledEndTime());
            item.put("clockInTime", record.getClockInTime());
            item.put("clockOutTime", record.getClockOutTime());
            item.put("status", record.getStatus());
            
            // 修正状态：没有打卡记录时不应显示"正常"
            if (record.getClockInTime() == null && record.getClockOutTime() == null
                    && record.getShiftType() != ShiftType.REST) {
                if (record.getAttendanceDate().isBefore(LocalDate.now())) {
                    item.put("status", "ABSENT"); // 过去的日期没打卡 → 缺勤
                } else {
                    item.put("status", "PENDING"); // 今天还没打卡 → 未打卡
                }
            }
            item.put("workHours", record.getWorkHours());
            item.put("remark", record.getRemark());
            
            // 获取用户信息
            User user = userMapper.selectById(record.getUserId());
            if (user != null) {
                item.put("userName", user.getName());
                item.put("userRole", user.getRole());
                item.put("department", user.getDepartment());
            }
            
            return item;
        }).toList();
        
        resultPage.setRecords(records);
        return resultPage;
    }


    /**
     * 获取每日考勤汇总统计
     * 
     * @param date 日期
     * @return 考勤汇总（正常/迟到/早退/缺勤人数）
     */
    public Map<String, Object> getDailySummary(LocalDate date) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("date", date);
        
        // 统计各状态人数
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceRecord::getAttendanceDate, date);
        wrapper.ne(AttendanceRecord::getShiftType, ShiftType.REST);
        
        List<AttendanceRecord> records = attendanceMapper.selectList(wrapper);
        
        int normalCount = 0;
        int lateCount = 0;
        int earlyLeaveCount = 0;
        int absentCount = 0;
        int totalCount = records.size();
        
        for (AttendanceRecord record : records) {
            if (record.getStatus() == null) {
                // 未打卡，判断是否缺勤
                if (date.isBefore(LocalDate.now())) {
                    absentCount++;
                }
            } else {
                switch (record.getStatus()) {
                    case NORMAL -> normalCount++;
                    case LATE -> lateCount++;
                    case EARLY_LEAVE -> earlyLeaveCount++;
                    case ABSENT -> absentCount++;
                    default -> {}
                }
            }
        }
        
        summary.put("totalCount", totalCount);
        summary.put("normalCount", normalCount);
        summary.put("lateCount", lateCount);
        summary.put("earlyLeaveCount", earlyLeaveCount);
        summary.put("absentCount", absentCount);
        
        return summary;
    }

    /**
     * 补卡操作
     * 
     * @param recordId 考勤记录ID
     * @param clockInTime 补录上班打卡时间（可选）
     * @param clockOutTime 补录下班打卡时间（可选）
     * @param reason 补卡原因
     * @param managerId 操作经理ID
     * @return 更新后的考勤记录
     */
    @Transactional
    public AttendanceRecord supplementAttendance(Long recordId, LocalTime clockInTime, 
                                                  LocalTime clockOutTime, String reason,
                                                  Long managerId) {
        AttendanceRecord record = attendanceMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        
        if (record.getShiftType() == ShiftType.REST) {
            throw new BusinessException("休息日无需补卡");
        }
        
        // 补录打卡时间
        if (clockInTime != null) {
            record.setClockInTime(clockInTime);
        }
        if (clockOutTime != null) {
            record.setClockOutTime(clockOutTime);
        }
        
        // 重新计算考勤状态
        recalculateAttendanceStatus(record);
        
        // 添加备注
        String remarkPrefix = "经理补卡";
        String newRemark = StringUtils.hasText(reason) ? 
                remarkPrefix + "：" + reason : remarkPrefix;
        
        if (StringUtils.hasText(record.getRemark())) {
            record.setRemark(record.getRemark() + "；" + newRemark);
        } else {
            record.setRemark(newRemark);
        }
        
        attendanceMapper.updateById(record);
        
        return record;
    }

    /**
     * 重新计算考勤状态和工作时长
     */
    private void recalculateAttendanceStatus(AttendanceRecord record) {
        LocalTime clockIn = record.getClockInTime();
        LocalTime clockOut = record.getClockOutTime();
        LocalTime scheduledStart = record.getScheduledStartTime();
        LocalTime scheduledEnd = record.getScheduledEndTime();
        
        if (clockIn == null && clockOut == null) {
            // 无打卡记录
            if (record.getAttendanceDate().isBefore(LocalDate.now())) {
                record.setStatus(AttendanceStatus.ABSENT);
            }
            record.setWorkHours(null);
            return;
        }
        
        // 判断迟到/早退
        boolean isLate = clockIn != null && scheduledStart != null && clockIn.isAfter(scheduledStart);
        boolean isEarlyLeave = clockOut != null && scheduledEnd != null && clockOut.isBefore(scheduledEnd);
        
        if (isLate && isEarlyLeave) {
            // 既迟到又早退，按迟到处理
            record.setStatus(AttendanceStatus.LATE);
        } else if (isLate) {
            record.setStatus(AttendanceStatus.LATE);
        } else if (isEarlyLeave) {
            record.setStatus(AttendanceStatus.EARLY_LEAVE);
        } else if (clockIn != null && clockOut != null) {
            record.setStatus(AttendanceStatus.NORMAL);
        } else if (clockIn != null) {
            // 只有上班打卡，暂不判定状态
            record.setStatus(null);
        }
        
        // 计算工作时长
        if (clockIn != null && clockOut != null) {
            // 处理夜班跨天的情况
            long minutes;
            if (record.getShiftType() == ShiftType.NIGHT && clockOut.isBefore(clockIn)) {
                // 夜班跨天：从打卡到午夜 + 午夜到下班
                minutes = Duration.between(clockIn, LocalTime.MAX).toMinutes() + 1 +
                          Duration.between(LocalTime.MIN, clockOut).toMinutes();
            } else {
                minutes = Duration.between(clockIn, clockOut).toMinutes();
            }
            BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            record.setWorkHours(hours);
        }
    }

    /**
     * 获取月度考勤统计报表
     * 
     * @param year 年份
     * @param month 月份
     * @return 月度统计报表
     */
    public List<Map<String, Object>> getMonthlyReport(int year, int month) {
        // 获取除经理外的所有用户
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.ne(User::getRole, UserRole.MANAGER);
        userWrapper.eq(User::getStatus, "active");
        List<User> users = userMapper.selectList(userWrapper);
        
        List<Map<String, Object>> report = new ArrayList<>();
        
        for (User user : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", user.getId());
            item.put("userName", user.getName());
            item.put("role", user.getRole());
            item.put("department", user.getDepartment());
            
            // 获取月度考勤统计
            List<Map<String, Object>> stats = attendanceMapper.getMonthlyStats(user.getId(), year, month);
            
            int normalDays = 0;
            int lateDays = 0;
            int earlyLeaveDays = 0;
            int absentDays = 0;
            int restDays = 0;
            
            for (Map<String, Object> stat : stats) {
                String status = String.valueOf(stat.get("status"));
                int count = ((Number) stat.get("count")).intValue();
                
                switch (status.toLowerCase()) {
                    case "normal", "正常" -> normalDays = count;
                    case "late", "迟到" -> lateDays = count;
                    case "early_leave", "早退" -> earlyLeaveDays = count;
                    case "absent", "缺勤" -> absentDays = count;
                    case "rest", "休息" -> restDays = count;
                }
            }
            
            item.put("normalDays", normalDays);
            item.put("lateDays", lateDays);
            item.put("earlyLeaveDays", earlyLeaveDays);
            item.put("absentDays", absentDays);
            item.put("restDays", restDays);
            item.put("workDays", normalDays + lateDays + earlyLeaveDays);
            
            // 计算出勤率
            int totalWorkDays = normalDays + lateDays + earlyLeaveDays + absentDays;
            double attendanceRate = totalWorkDays > 0 ? 
                    (double)(normalDays + lateDays + earlyLeaveDays) / totalWorkDays * 100 : 0;
            item.put("attendanceRate", Math.round(attendanceRate * 100) / 100.0);
            
            report.add(item);
        }
        
        return report;
    }

    /**
     * 获取班次类型名称
     */
    private String getShiftTypeName(ShiftType shiftType) {
        if (shiftType == null) return "未排班";
        return switch (shiftType) {
            case DAY -> "白班";
            case NIGHT -> "夜班";
            case REST -> "休息";
        };
    }
}
