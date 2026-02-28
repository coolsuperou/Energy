package org.example.back.controller.dispatcher;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.Application;
import org.example.back.entity.AttendanceRecord;
import org.example.back.entity.EnergyData;
import org.example.back.entity.Task;
import org.example.back.entity.Quota;
import org.example.back.entity.User;
import org.example.back.entity.enums.ApplicationStatus;
import org.example.back.entity.enums.TaskStatus;
import org.example.back.entity.enums.UserRole;
import org.example.back.mapper.common.AttendanceMapper;
import org.example.back.mapper.common.EnergyDataMapper;
import org.example.back.mapper.common.QuotaMapper;
import org.example.back.mapper.common.UserMapper;
import org.example.back.mapper.dispatcher.TaskMapper;
import org.example.back.mapper.workshop.ApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调度员工作台控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	调度员工作台首页概览相关接口
 * -- =============================================
 */
@RestController
@RequestMapping("/dispatcher")
public class DispatcherController {

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private EnergyDataMapper energyDataMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private QuotaMapper quotaMapper;

    /**
     * 获取各车间用电分配数据（车间名称+配额）
     * 返回真实车间列表及其当月配额信息
     */
    @GetMapping("/workshops")
    public Result<List<Map<String, Object>>> getWorkshops() {
        // 查询所有车间用户
        List<User> workshopUsers = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getRole, UserRole.WORKSHOP)
                .eq(User::getStatus, "active")
                .orderByAsc(User::getId)
        );

        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<Map<String, Object>> result = new ArrayList<>();

        for (int i = 0; i < workshopUsers.size(); i++) {
            User user = workshopUsers.get(i);
            int workshopId = i + 1;

            // 查询该车间当月配额
            Quota quota = quotaMapper.selectOne(
                new LambdaQueryWrapper<Quota>()
                    .eq(Quota::getWorkshopId, workshopId)
                    .eq(Quota::getYearMonth, yearMonth)
            );

            // 查询该车间今日实时功率（取最新一小时的能耗数据求和）
            LambdaQueryWrapper<EnergyData> powerWrapper = new LambdaQueryWrapper<>();
            powerWrapper.eq(EnergyData::getWorkshopId, workshopId);
            powerWrapper.eq(EnergyData::getRecordDate, LocalDate.now());
            powerWrapper.select(EnergyData::getPower);
            List<EnergyData> todayData = energyDataMapper.selectList(powerWrapper);
            BigDecimal currentPower = todayData.stream()
                    .map(EnergyData::getPower)
                    .filter(p -> p != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::max);

            Map<String, Object> item = new HashMap<>();
            item.put("workshopId", workshopId);
            item.put("name", user.getDepartment());
            item.put("userId", user.getId());
            item.put("currentPower", currentPower);
            if (quota != null) {
                item.put("totalQuota", quota.getTotalQuota());
                item.put("usedQuota", quota.getUsedQuota());
            } else {
                item.put("totalQuota", new BigDecimal("50000"));
                item.put("usedQuota", BigDecimal.ZERO);
            }

            result.add(item);
        }

        return Result.success(result);
    }

    /**
     * 获取所有在职巡检员列表（含今日排班信息）
     * 用于反馈转工单时选择巡检员
     */
    @GetMapping("/inspectors")
    public Result<List<Map<String, Object>>> getInspectors() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, "active");
        wrapper.eq(User::getRole, UserRole.INSPECTOR);
        wrapper.orderByAsc(User::getName);
        List<User> inspectors = userMapper.selectList(wrapper);
        
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (User u : inspectors) {
            // 查询今日排班
            AttendanceRecord record = attendanceMapper.getTodayAttendance(u.getId(), today);
            
            // 只返回今天在班的巡检员（排除休息和未排班的）
            if (record == null || record.getShiftType() == null) {
                continue;
            }
            String shift = record.getShiftType().name();
            if ("REST".equals(shift)) {
                continue;
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("name", u.getName());
            item.put("department", u.getDepartment());
            item.put("phone", u.getPhone());
            
            item.put("shiftType", shift.toLowerCase());
            item.put("scheduledStartTime", record.getScheduledStartTime());
            item.put("scheduledEndTime", record.getScheduledEndTime());
            item.put("attendanceStatus", record.getStatus() != null ? record.getStatus().name().toLowerCase() : null);
            
            // 查询当前进行中的任务数
            LambdaQueryWrapper<Task> taskWrapper = new LambdaQueryWrapper<>();
            taskWrapper.eq(Task::getAssignedTo, u.getId());
            taskWrapper.eq(Task::getStatus, TaskStatus.IN_PROGRESS);
            long taskCount = taskMapper.selectCount(taskWrapper);
            item.put("inProgressTasks", taskCount);
            
            result.add(item);
        }
        
        return Result.success(result);
    }

    /**
     * 获取调度员工作台概览数据
     * 返回待审批数、待处理预警数、进行中工单数、今日关键指标
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        Map<String, Object> dashboard = new HashMap<>();

        // 1. 待审批申请数量
        long pendingApplicationCount = getPendingApplicationCount();
        dashboard.put("pendingApplicationCount", pendingApplicationCount);

        // 2. 进行中工单数量
        long inProgressTaskCount = getInProgressTaskCount();
        dashboard.put("inProgressTaskCount", inProgressTaskCount);

        // 3. 今日关键指标
        Map<String, Object> todayMetrics = getTodayMetrics();
        dashboard.put("todayMetrics", todayMetrics);

        return Result.success(dashboard);
    }

    /**
     * 获取待审批申请数量
     */
    private long getPendingApplicationCount() {
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Application::getStatus, ApplicationStatus.PENDING);
        return applicationMapper.selectCount(wrapper);
    }

    /**
     * 获取进行中工单数量
     */
    private long getInProgressTaskCount() {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getStatus, TaskStatus.IN_PROGRESS);
        return taskMapper.selectCount(wrapper);
    }

    /**
     * 获取今日关键指标
     */
    private Map<String, Object> getTodayMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        LocalDate today = LocalDate.now();

        // 今日总用电量
        BigDecimal todayTotalEnergy = getTodayTotalEnergy();
        metrics.put("todayTotalEnergy", todayTotalEnergy);

        // 今日总费用
        BigDecimal todayTotalCost = getTodayTotalCost();
        metrics.put("todayTotalCost", todayTotalCost);

        // 本月总用电量
        BigDecimal monthTotalEnergy = getMonthTotalEnergy();
        metrics.put("monthTotalEnergy", monthTotalEnergy);

        // 今日预警次数（从预警数据获取）
        LambdaQueryWrapper<EnergyData> alertWrapper = new LambdaQueryWrapper<>();
        alertWrapper.eq(EnergyData::getRecordDate, today);
        alertWrapper.select(EnergyData::getWorkshopId, EnergyData::getPower);
        List<EnergyData> todayData = energyDataMapper.selectList(alertWrapper);
        
        // 统计超限次数（功率超过阈值的记录数）
        long alertCount = todayData.stream()
                .filter(data -> data.getPower() != null && data.getPower().compareTo(new BigDecimal("700")) > 0)
                .count();
        metrics.put("todayAlertCount", alertCount);

        // 今日审批数量
        long todayApprovedCount = getTodayApprovedCount();
        metrics.put("todayApprovedCount", todayApprovedCount);

        // 今日完成工单数
        long todayCompletedTaskCount = getTodayCompletedTaskCount();
        metrics.put("todayCompletedTaskCount", todayCompletedTaskCount);

        return metrics;
    }

    /**
     * 获取今日总用电量
     */
    private BigDecimal getTodayTotalEnergy() {
        LambdaQueryWrapper<EnergyData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyData::getRecordDate, LocalDate.now());
        wrapper.select(EnergyData::getEnergy);
        
        List<EnergyData> dataList = energyDataMapper.selectList(wrapper);
        return dataList.stream()
                .map(EnergyData::getEnergy)
                .filter(e -> e != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取今日总费用
     */
    private BigDecimal getTodayTotalCost() {
        LambdaQueryWrapper<EnergyData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyData::getRecordDate, LocalDate.now());
        wrapper.select(EnergyData::getCost);
        
        List<EnergyData> dataList = energyDataMapper.selectList(wrapper);
        return dataList.stream()
                .map(EnergyData::getCost)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取本月总用电量
     */
    private BigDecimal getMonthTotalEnergy() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        LambdaQueryWrapper<EnergyData> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(EnergyData::getRecordDate, startDate, endDate);
        wrapper.select(EnergyData::getEnergy);
        
        List<EnergyData> dataList = energyDataMapper.selectList(wrapper);
        return dataList.stream()
                .map(EnergyData::getEnergy)
                .filter(e -> e != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取今日审批数量
     */
    private long getTodayApprovedCount() {
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Application::getApprovedAt);
        wrapper.apply("DATE(approved_at) = {0}", LocalDate.now());
        return applicationMapper.selectCount(wrapper);
    }

    /**
     * 获取今日完成工单数
     */
    private long getTodayCompletedTaskCount() {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getStatus, TaskStatus.COMPLETED);
        wrapper.apply("DATE(completed_at) = {0}", LocalDate.now());
        return taskMapper.selectCount(wrapper);
    }
}
