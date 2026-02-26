package org.example.back.controller.manager;

import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.User;
import org.example.back.service.manager.ManagerDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 经理首页概览控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	经理首页概览数据接口
 * -- =============================================
 */
@RestController
@RequestMapping("/manager")
public class ManagerDashboardController {

    @Autowired
    private ManagerDashboardService managerDashboardService;

    /**
     * 获取经理首页概览数据
     * 包含：全厂用电指标、车间排名、待处理事项、用电趋势
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        Map<String, Object> dashboardData = managerDashboardService.getDashboardData();
        return Result.success(dashboardData);
    }
}
