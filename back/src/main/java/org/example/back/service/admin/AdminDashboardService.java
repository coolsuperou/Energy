package org.example.back.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.back.entity.AttendanceRecord;
import org.example.back.entity.OperationLog;
import org.example.back.entity.User;
import org.example.back.entity.enums.UserRole;
import org.example.back.entity.enums.UserStatus;
import org.example.back.mapper.admin.OperationLogMapper;
import org.example.back.mapper.common.AttendanceMapper;
import org.example.back.mapper.common.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 系统管理员首页概览服务
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	系统管理员首页概览数据统计逻辑
 * -- =============================================
 */
@Service
public class AdminDashboardService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private DataSource dataSource;

    /**
     * 获取管理员首页概览数据
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> result = new HashMap<>();
        
        // 1. 用户统计
        result.put("userStats", getUserStats());
        
        // 2. 各角色用户数量
        result.put("roleDistribution", getRoleDistribution());
        
        // 3. 今日活跃用户数
        result.put("todayActiveUsers", getTodayActiveUsers());
        
        // 4. 系统运行状态
        result.put("systemStatus", getSystemStatus());
        
        // 5. 最近操作日志
        result.put("recentLogs", getRecentLogs(10));
        
        return result;
    }

    /**
     * 获取用户统计数据
     */
    private Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 用户总数
        long totalUsers = userMapper.selectCount(null);
        stats.put("totalUsers", totalUsers);
        
        // 活跃用户数（状态为active）
        LambdaQueryWrapper<User> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(User::getStatus, UserStatus.ACTIVE);
        long activeUsers = userMapper.selectCount(activeWrapper);
        stats.put("activeUsers", activeUsers);
        
        // 禁用用户数
        LambdaQueryWrapper<User> disabledWrapper = new LambdaQueryWrapper<>();
        disabledWrapper.eq(User::getStatus, UserStatus.DISABLED);
        long disabledUsers = userMapper.selectCount(disabledWrapper);
        stats.put("disabledUsers", disabledUsers);
        
        // 本月新增用户数
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        LambdaQueryWrapper<User> newUserWrapper = new LambdaQueryWrapper<>();
        newUserWrapper.ge(User::getCreatedAt, firstDayOfMonth.atStartOfDay());
        long newUsersThisMonth = userMapper.selectCount(newUserWrapper);
        stats.put("newUsersThisMonth", newUsersThisMonth);
        
        return stats;
    }

    /**
     * 获取各角色用户数量分布
     */
    private List<Map<String, Object>> getRoleDistribution() {
        List<Map<String, Object>> distribution = new ArrayList<>();
        
        // 角色名称映射
        Map<UserRole, String> roleNames = new LinkedHashMap<>();
        roleNames.put(UserRole.ADMIN, "系统管理员");
        roleNames.put(UserRole.MANAGER, "经理");
        roleNames.put(UserRole.DISPATCHER, "调度员");
        roleNames.put(UserRole.INSPECTOR, "巡检员");
        roleNames.put(UserRole.WORKSHOP, "车间用户");
        
        for (Map.Entry<UserRole, String> entry : roleNames.entrySet()) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getRole, entry.getKey());
            long count = userMapper.selectCount(wrapper);
            
            Map<String, Object> item = new HashMap<>();
            item.put("role", entry.getKey().name().toLowerCase());
            item.put("roleName", entry.getValue());
            item.put("count", count);
            distribution.add(item);
        }
        
        return distribution;
    }

    /**
     * 获取今日活跃用户数（今日有打卡记录的用户）
     */
    private long getTodayActiveUsers() {
        LocalDate today = LocalDate.now();
        
        // 查询今日有考勤记录的用户数
        QueryWrapper<AttendanceRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("attendance_date", today);
        wrapper.isNotNull("clock_in_time");
        wrapper.select("DISTINCT user_id");
        
        return attendanceMapper.selectCount(wrapper);
    }

    /**
     * 获取系统运行状态
     */
    private Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // 数据库连接状态
        status.put("database", checkDatabaseStatus());
        
        // MinIO状态（简单检查，实际可以调用MinIO健康检查接口）
        status.put("minio", checkMinioStatus());
        
        // API服务状态（当前服务正在运行，所以肯定是正常的）
        Map<String, Object> apiStatus = new HashMap<>();
        apiStatus.put("status", "normal");
        apiStatus.put("message", "正常运行");
        status.put("api", apiStatus);
        
        // 系统运行时间
        status.put("uptime", getSystemUptime());
        
        return status;
    }

    /**
     * 检查数据库连接状态
     */
    private Map<String, Object> checkDatabaseStatus() {
        Map<String, Object> dbStatus = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(5)) {
                dbStatus.put("status", "normal");
                dbStatus.put("message", "正常运行");
            } else {
                dbStatus.put("status", "warning");
                dbStatus.put("message", "连接超时");
            }
        } catch (Exception e) {
            dbStatus.put("status", "error");
            dbStatus.put("message", "连接失败: " + e.getMessage());
        }
        return dbStatus;
    }

    /**
     * 检查MinIO状态
     */
    private Map<String, Object> checkMinioStatus() {
        Map<String, Object> minioStatus = new HashMap<>();
        // 简单返回正常状态，实际项目中可以调用MinIO的健康检查接口
        minioStatus.put("status", "normal");
        minioStatus.put("message", "正常运行");
        return minioStatus;
    }

    /**
     * 获取系统运行时间
     */
    private String getSystemUptime() {
        // 获取JVM启动时间
        long uptimeMillis = System.currentTimeMillis() - 
                java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();
        
        long seconds = uptimeMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "天" + (hours % 24) + "小时";
        } else if (hours > 0) {
            return hours + "小时" + (minutes % 60) + "分钟";
        } else {
            return minutes + "分钟";
        }
    }

    /**
     * 获取最近操作日志
     */
    private List<Map<String, Object>> getRecentLogs(int limit) {
        List<Map<String, Object>> logs = new ArrayList<>();
        
        // 查询最近的操作日志
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        wrapper.last("LIMIT " + limit);
        
        List<OperationLog> logList = operationLogMapper.selectList(wrapper);
        
        for (OperationLog log : logList) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", log.getId());
            item.put("userId", log.getUserId());
            item.put("operationType", log.getOperationType());
            item.put("operationDesc", log.getOperationDesc());
            item.put("ipAddress", log.getIpAddress());
            item.put("createdAt", log.getCreatedAt());
            
            // 获取操作人姓名
            User user = userMapper.selectById(log.getUserId());
            item.put("userName", user != null ? user.getName() : "未知用户");
            
            // 计算相对时间
            item.put("relativeTime", getRelativeTime(log.getCreatedAt()));
            
            logs.add(item);
        }
        
        return logs;
    }

    /**
     * 计算相对时间
     */
    private String getRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "未知";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();
        
        if (minutes < 1) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (minutes < 1440) {
            return (minutes / 60) + "小时前";
        } else {
            return (minutes / 1440) + "天前";
        }
    }
}
