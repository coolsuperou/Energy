package org.example.back.controller.manager;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.AttendanceRecord;
import org.example.back.entity.User;
import org.example.back.entity.enums.ShiftType;
import org.example.back.service.manager.ScheduleAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 排班考勤管理控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	经理排班考勤管理接口（排班生成、调整、考勤查看、补卡）
 * -- =============================================
 */
@RestController
@RequestMapping("/manager/schedule")
public class ScheduleAttendanceController {

    @Autowired
    private ScheduleAttendanceService scheduleAttendanceService;

    /**
     * 自动生成月度排班计划
     * 
     * @param year 年份
     * @param month 月份
     */
    @PostMapping("/generate")
    public Result<Map<String, Object>> generateSchedule(
            @RequestParam int year,
            @RequestParam int month,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        int count = scheduleAttendanceService.generateMonthlySchedule(year, month);
        return Result.success("排班生成成功", Map.of("generatedCount", count, "year", year, "month", month));
    }

    /**
     * 获取所有人员的排班表
     * 
     * @param page 当前页，默认1
     * @param size 每页大小，默认10
     * @param year 年份
     * @param month 月份
     * @param role 角色筛选（可选）
     * @param department 部门筛选（可选）
     */
    @GetMapping("/list")
    public Result<IPage<Map<String, Object>>> getScheduleList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        IPage<Map<String, Object>> schedules = scheduleAttendanceService.getScheduleList(
                page, size, year, month, role, department);
        return Result.success(schedules);
    }

    /**
     * 调整排班
     * 
     * @param id 排班记录ID
     * @param requestBody 包含 shiftType, scheduledStartTime, scheduledEndTime
     */
    @PutMapping("/{id}")
    public Result<AttendanceRecord> adjustSchedule(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        String shiftTypeStr = (String) requestBody.get("shiftType");
        if (shiftTypeStr == null) {
            return Result.error("班次类型不能为空");
        }
        
        ShiftType shiftType;
        try {
            shiftType = ShiftType.valueOf(shiftTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Result.error("无效的班次类型");
        }
        
        LocalTime scheduledStartTime = null;
        LocalTime scheduledEndTime = null;
        
        if (requestBody.get("scheduledStartTime") != null) {
            scheduledStartTime = LocalTime.parse((String) requestBody.get("scheduledStartTime"));
        }
        if (requestBody.get("scheduledEndTime") != null) {
            scheduledEndTime = LocalTime.parse((String) requestBody.get("scheduledEndTime"));
        }
        
        AttendanceRecord record = scheduleAttendanceService.adjustSchedule(
                id, shiftType, scheduledStartTime, scheduledEndTime, currentUser.getId());
        return Result.success("排班调整成功", record);
    }


    // ==================== 考勤管理接口 ====================

    /**
     * 获取所有人员的考勤记录
     * 
     * @param page 当前页，默认1
     * @param size 每页大小，默认10
     * @param userId 用户ID筛选（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param status 考勤状态筛选（可选）
     */
    @GetMapping("/attendance")
    public Result<IPage<Map<String, Object>>> getAttendanceList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        IPage<Map<String, Object>> attendance = scheduleAttendanceService.getAttendanceList(
                page, size, userId, startDate, endDate, status);
        return Result.success(attendance);
    }

    /**
     * 获取每日考勤汇总统计
     * 
     * @param date 日期
     */
    @GetMapping("/attendance/daily-summary")
    public Result<Map<String, Object>> getDailySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        Map<String, Object> summary = scheduleAttendanceService.getDailySummary(date);
        return Result.success(summary);
    }

    /**
     * 补卡操作
     * 
     * @param id 考勤记录ID
     * @param requestBody 包含 clockInTime, clockOutTime, reason
     */
    @PutMapping("/attendance/{id}/supplement")
    public Result<AttendanceRecord> supplementAttendance(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        LocalTime clockInTime = null;
        LocalTime clockOutTime = null;
        String reason = (String) requestBody.get("reason");
        
        if (requestBody.get("clockInTime") != null) {
            clockInTime = LocalTime.parse((String) requestBody.get("clockInTime"));
        }
        if (requestBody.get("clockOutTime") != null) {
            clockOutTime = LocalTime.parse((String) requestBody.get("clockOutTime"));
        }
        
        if (clockInTime == null && clockOutTime == null) {
            return Result.error("请至少提供一个打卡时间");
        }
        
        AttendanceRecord record = scheduleAttendanceService.supplementAttendance(
                id, clockInTime, clockOutTime, reason, currentUser.getId());
        return Result.success("补卡成功", record);
    }

    /**
     * 获取月度考勤统计报表
     * 
     * @param year 年份
     * @param month 月份
     */
    @GetMapping("/attendance/monthly-report")
    public Result<List<Map<String, Object>>> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        List<Map<String, Object>> report = scheduleAttendanceService.getMonthlyReport(year, month);
        return Result.success(report);
    }
}
