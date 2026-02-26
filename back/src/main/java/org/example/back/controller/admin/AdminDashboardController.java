package org.example.back.controller.admin;

import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.User;
import org.example.back.service.admin.AdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统管理员首页概览控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	系统管理员首页概览数据接口
 * -- =============================================
 */
@RestController
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService adminDashboardService;

    /**
     * 获取管理员首页概览数据
     * 包含：用户统计、角色分布、今日活跃用户、系统状态、最近操作日志
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        Map<String, Object> dashboardData = adminDashboardService.getDashboardData();
        return Result.success(dashboardData);
    }
}
