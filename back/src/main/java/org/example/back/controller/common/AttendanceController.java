package org.example.back.controller.common;

import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.dto.AttendanceRecordDTO;
import org.example.back.entity.AttendanceRecord;
import org.example.back.entity.User;
import org.example.back.service.common.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 考勤控制器
 * 提供考勤打卡、查询等接口
 */
@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /**
     * 打卡
     * @param type 打卡类型 in-上班 out-下班
     */
    @PostMapping("/clock")
    public Result<AttendanceRecord> clockIn(@RequestParam String type, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        try {
            AttendanceRecord record = attendanceService.clockIn(currentUser, type);
            return Result.success(record);
        } catch (Exception e) {
            return Result.error("打卡失败: " + e.getMessage());
        }
    }

    /**
     * 获取今日考勤
     */
    @GetMapping("/today")
    public Result<AttendanceRecordDTO> getTodayAttendance(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        AttendanceRecord record = attendanceService.getTodayAttendance(currentUser);
        AttendanceRecordDTO dto = AttendanceRecordDTO.fromEntity(record);
        return Result.success(dto);
    }

    /**
     * 获取月度考勤记录
     */
    @GetMapping("/monthly")
    public Result<List<AttendanceRecordDTO>> getMonthlyAttendance(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }

        List<AttendanceRecord> records = attendanceService.getMonthlyAttendance(currentUser, year, month);
        List<AttendanceRecordDTO> dtos = records.stream()
                .map(AttendanceRecordDTO::fromEntity)
                .collect(Collectors.toList());
        return Result.success(dtos);
    }

    /**
     * 获取月度考勤统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getAttendanceStats(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        if (year == null || month == null) {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }

        Map<String, Object> stats = attendanceService.getAttendanceStats(currentUser, year, month);
        return Result.success(stats);
    }

    /**
     * 获取本周排班
     */
    @GetMapping("/schedule/weekly")
    public Result<List<AttendanceRecordDTO>> getWeeklySchedule(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        List<AttendanceRecord> schedule = attendanceService.getWeeklySchedule(currentUser);
        List<AttendanceRecordDTO> dtos = schedule.stream()
                .map(AttendanceRecordDTO::fromEntity)
                .collect(Collectors.toList());
        return Result.success(dtos);
    }
}
