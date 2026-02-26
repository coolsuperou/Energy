package org.example.back.controller.inspector;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.SkillCertification;
import org.example.back.entity.Task;
import org.example.back.entity.User;
import org.example.back.entity.enums.CertificationStatus;
import org.example.back.entity.enums.TaskStatus;
import org.example.back.mapper.dispatcher.TaskMapper;
import org.example.back.service.common.NotificationService;
import org.example.back.service.inspector.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 设备巡检员工作台控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	设备巡检员工作台首页概览相关接口
 * -- =============================================
 */
@RestController
@RequestMapping("/inspector")
public class InspectorController {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SkillService skillService;

    /**
     * 获取巡检员工作台概览数据
     * 返回任务统计、今日待完成任务、工作量趋势、技能列表
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        Map<String, Object> dashboard = new HashMap<>();

        Long userId = user.getId();

        // 1. 任务统计（待处理、进行中、已完成数量）
        Map<String, Object> taskStats = getTaskStats(userId);
        dashboard.put("taskStats", taskStats);

        // 2. 今日待完成任务列表
        List<Task> todayTasks = getTodayTasks(userId);
        dashboard.put("todayTasks", todayTasks);

        // 3. 工作量趋势数据（近7天）
        List<Map<String, Object>> workloadTrend = getWorkloadTrend(userId);
        dashboard.put("workloadTrend", workloadTrend);

        // 4. 技能列表（模拟数据，实际应从技能认证表获取）
        List<Map<String, Object>> skills = getSkillList(userId);
        dashboard.put("skills", skills);

        // 5. 未读消息数
        int unreadCount = notificationService.getUnreadCount(userId);
        dashboard.put("unreadCount", unreadCount);

        return Result.success(dashboard);
    }


    /**
     * 获取任务统计数据
     */
    private Map<String, Object> getTaskStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 待处理任务数（PENDING状态，已分配给当前用户）
        LambdaQueryWrapper<Task> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Task::getAssignedTo, userId);
        pendingWrapper.eq(Task::getStatus, TaskStatus.PENDING);
        long pendingCount = taskMapper.selectCount(pendingWrapper);
        stats.put("pendingCount", pendingCount);

        // 进行中任务数
        LambdaQueryWrapper<Task> inProgressWrapper = new LambdaQueryWrapper<>();
        inProgressWrapper.eq(Task::getAssignedTo, userId);
        inProgressWrapper.eq(Task::getStatus, TaskStatus.IN_PROGRESS);
        long inProgressCount = taskMapper.selectCount(inProgressWrapper);
        stats.put("inProgressCount", inProgressCount);

        // 已完成任务数（本月）
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        LambdaQueryWrapper<Task> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(Task::getAssignedTo, userId);
        completedWrapper.eq(Task::getStatus, TaskStatus.COMPLETED);
        completedWrapper.ge(Task::getCompletedAt, firstDayOfMonth.atStartOfDay());
        long completedCount = taskMapper.selectCount(completedWrapper);
        stats.put("completedCount", completedCount);

        // 今日完成数
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Task> todayCompletedWrapper = new LambdaQueryWrapper<>();
        todayCompletedWrapper.eq(Task::getAssignedTo, userId);
        todayCompletedWrapper.eq(Task::getStatus, TaskStatus.COMPLETED);
        todayCompletedWrapper.ge(Task::getCompletedAt, today.atStartOfDay());
        todayCompletedWrapper.lt(Task::getCompletedAt, today.plusDays(1).atStartOfDay());
        long todayCompletedCount = taskMapper.selectCount(todayCompletedWrapper);
        stats.put("todayCompletedCount", todayCompletedCount);

        // 本周完成数
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LambdaQueryWrapper<Task> weekCompletedWrapper = new LambdaQueryWrapper<>();
        weekCompletedWrapper.eq(Task::getAssignedTo, userId);
        weekCompletedWrapper.eq(Task::getStatus, TaskStatus.COMPLETED);
        weekCompletedWrapper.ge(Task::getCompletedAt, startOfWeek.atStartOfDay());
        long weekCompletedCount = taskMapper.selectCount(weekCompletedWrapper);
        stats.put("weekCompletedCount", weekCompletedCount);

        // 计算按时完成率（本月）
        LambdaQueryWrapper<Task> monthTaskWrapper = new LambdaQueryWrapper<>();
        monthTaskWrapper.eq(Task::getAssignedTo, userId);
        monthTaskWrapper.eq(Task::getStatus, TaskStatus.COMPLETED);
        monthTaskWrapper.ge(Task::getCompletedAt, firstDayOfMonth.atStartOfDay());
        List<Task> monthTasks = taskMapper.selectList(monthTaskWrapper);
        
        long onTimeCount = monthTasks.stream()
                .filter(task -> task.getDueDate() != null && task.getCompletedAt() != null)
                .filter(task -> !task.getCompletedAt().toLocalDate().isAfter(task.getDueDate()))
                .count();
        
        double onTimeRate = monthTasks.isEmpty() ? 100.0 : (onTimeCount * 100.0 / monthTasks.size());
        stats.put("onTimeRate", Math.round(onTimeRate * 10) / 10.0);

        return stats;
    }

    /**
     * 获取今日待完成任务列表
     * 截止日期≤今天的未完成任务
     */
    private List<Task> getTodayTasks(Long userId) {
        LocalDate today = LocalDate.now();
        
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getAssignedTo, userId);
        wrapper.in(Task::getStatus, TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
        wrapper.and(w -> w.isNull(Task::getDueDate).or().le(Task::getDueDate, today));
        wrapper.orderByAsc(Task::getDueDate);
        wrapper.orderByDesc(Task::getPriority);
        wrapper.last("LIMIT 10");
        
        return taskMapper.selectList(wrapper);
    }


    /**
     * 获取近7天工作量趋势数据
     */
    private List<Map<String, Object>> getWorkloadTrend(Long userId) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, Object> dayData = new HashMap<>();
            
            // 日期
            dayData.put("date", date.toString());
            dayData.put("dayOfWeek", getDayOfWeekName(date.getDayOfWeek()));
            
            // 当天完成的工单数
            LambdaQueryWrapper<Task> completedWrapper = new LambdaQueryWrapper<>();
            completedWrapper.eq(Task::getAssignedTo, userId);
            completedWrapper.eq(Task::getStatus, TaskStatus.COMPLETED);
            completedWrapper.ge(Task::getCompletedAt, date.atStartOfDay());
            completedWrapper.lt(Task::getCompletedAt, date.plusDays(1).atStartOfDay());
            long completedCount = taskMapper.selectCount(completedWrapper);
            dayData.put("completedCount", completedCount);
            
            // 当天分配的任务数
            LambdaQueryWrapper<Task> assignedWrapper = new LambdaQueryWrapper<>();
            assignedWrapper.eq(Task::getAssignedTo, userId);
            assignedWrapper.ge(Task::getCreatedAt, date.atStartOfDay());
            assignedWrapper.lt(Task::getCreatedAt, date.plusDays(1).atStartOfDay());
            long assignedCount = taskMapper.selectCount(assignedWrapper);
            dayData.put("assignedCount", assignedCount);
            
            trend.add(dayData);
        }
        
        return trend;
    }

    /**
     * 获取星期名称
     */
    private String getDayOfWeekName(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "周一";
            case TUESDAY: return "周二";
            case WEDNESDAY: return "周三";
            case THURSDAY: return "周四";
            case FRIDAY: return "周五";
            case SATURDAY: return "周六";
            case SUNDAY: return "周日";
            default: return "";
        }
    }

    /**
     * 获取技能列表
     * 从技能认证表获取用户的技能认证数据
     */
    private List<Map<String, Object>> getSkillList(Long userId) {
        List<Map<String, Object>> skills = new ArrayList<>();
        
        // 从数据库查询用户的技能认证
        List<SkillCertification> certifications = skillService.getUserSkills(userId);
        
        for (SkillCertification cert : certifications) {
            Map<String, Object> skill = new HashMap<>();
            skill.put("id", cert.getId());
            skill.put("name", cert.getSkillName());
            skill.put("status", cert.getStatus().getValue());
            skill.put("certificateUrl", cert.getCertificateUrl());
            
            if (cert.getStatus() == CertificationStatus.CERTIFIED && cert.getReviewedAt() != null) {
                skill.put("certifiedAt", cert.getReviewedAt().toLocalDate().toString());
            } else {
                skill.put("appliedAt", cert.getCreatedAt().toLocalDate().toString());
            }
            
            if (cert.getStatus() == CertificationStatus.REJECTED) {
                skill.put("rejectReason", cert.getRejectReason());
            }
            
            skills.add(skill);
        }
        
        return skills;
    }
}
