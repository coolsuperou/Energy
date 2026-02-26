package org.example.back.controller.manager;

import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.User;
import org.example.back.service.manager.ManagerAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 经理数据分析控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	经理数据分析接口，提供车间对比、时段分析、成本统计
 * -- =============================================
 */
@RestController
@RequestMapping("/energy-data")
public class ManagerAnalysisController {

    @Autowired
    private ManagerAnalysisService managerAnalysisService;

    /**
     * 获取能耗数据汇总分析
     * 包含：车间对比、时段分析、成本统计
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param workshopId 车间ID（可选，不传则查询全部车间）
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> getEnergySummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long workshopId,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        Map<String, Object> summaryData = managerAnalysisService.getEnergySummary(startDate, endDate, workshopId);
        return Result.success(summaryData);
    }

    /**
     * 获取车间能耗对比数据
     */
    @GetMapping("/workshop-compare")
    public Result<Map<String, Object>> getWorkshopCompare(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        Map<String, Object> compareData = managerAnalysisService.getWorkshopCompare(startDate, endDate);
        return Result.success(compareData);
    }

    /**
     * 获取时段用电分析（峰/平/谷占比）
     */
    @GetMapping("/period-analysis")
    public Result<Map<String, Object>> getPeriodAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long workshopId,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        Map<String, Object> periodData = managerAnalysisService.getPeriodAnalysis(startDate, endDate, workshopId);
        return Result.success(periodData);
    }

    /**
     * 获取成本趋势数据
     */
    @GetMapping("/cost-trend")
    public Result<Map<String, Object>> getCostTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long workshopId,
            @RequestParam(defaultValue = "daily") String granularity,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        Map<String, Object> trendData = managerAnalysisService.getCostTrend(startDate, endDate, workshopId, granularity);
        return Result.success(trendData);
    }
}
