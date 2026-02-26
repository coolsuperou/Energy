package org.example.back.controller.dispatcher;

import org.example.back.common.Result;
import org.example.back.entity.EnergyData;
import org.example.back.entity.User;
import org.example.back.service.common.EnergyDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 能耗数据控制器
 * 提供能耗数据查询接口，支持多角色访问
 */
@RestController
@RequestMapping("/energy-data")
public class EnergyDataController {

    @Autowired
    private EnergyDataService energyDataService;

    /**
     * 获取今日能耗曲线（按小时）
     * 车间用户：自动查看自己车间的数据
     * 其他角色：可指定车间ID，不指定则查看全部
     */
    @GetMapping("/today")
    public Result<List<EnergyData>> getTodayHourlyData(
            @RequestParam(required = false) Long workshopId,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        List<EnergyData> data = energyDataService.getTodayHourlyData(currentUser, workshopId);
        return Result.success(data);
    }

    /**
     * 获取指定日期范围的能耗数据
     */
    @GetMapping("/range")
    public Result<List<EnergyData>> getEnergyDataByDateRange(
            @RequestParam(required = false) Long workshopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        List<EnergyData> data = energyDataService.getEnergyDataByDateRange(
                currentUser, workshopId, startDate, endDate);
        return Result.success(data);
    }

    /**
     * 获取各车间能耗统计
     */
    @GetMapping("/workshop-stats")
    public Result<Map<String, Object>> getWorkshopEnergyStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        if (date == null) {
            date = LocalDate.now();
        }

        Map<String, Object> stats = energyDataService.getWorkshopEnergyStats(currentUser, date);
        return Result.success(stats);
    }

}
