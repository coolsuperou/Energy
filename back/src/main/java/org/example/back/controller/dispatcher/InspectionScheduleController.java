package org.example.back.controller.dispatcher;

import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.InspectionPlan;
import org.example.back.entity.InspectionRecord;
import org.example.back.entity.User;
import org.example.back.service.dispatcher.InspectionScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 巡检排班控制器
 * 调度员管理每周巡检排班计划
 *
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	调度员巡检排班管理接口
 * -- =============================================
 */
@RestController
@RequestMapping("/inspection-schedule")
public class InspectionScheduleController {

    @Autowired
    private InspectionScheduleService scheduleService;

    /**
     * 获取某周排班列表
     */
    @GetMapping("/plans")
    public Result<List<InspectionPlan>> getWeekPlans(
            @RequestParam String weekStart,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "请先登录");

        LocalDate date = LocalDate.parse(weekStart);
        List<InspectionPlan> plans = scheduleService.getWeekPlans(date);
        return Result.success(plans);
    }

    /**
     * 添加排班
     */
    @PostMapping("/plans")
    public Result<InspectionPlan> addPlan(@RequestBody Map<String, Object> body,
                                          HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "请先登录");

        Long inspectorId = Long.valueOf(body.get("inspectorId").toString());
        Long workshopId = Long.valueOf(body.get("workshopId").toString());
        LocalDate weekStart = LocalDate.parse(body.get("weekStart").toString());

        InspectionPlan plan = scheduleService.addPlan(inspectorId, workshopId, weekStart);
        return Result.success(plan);
    }

    /**
     * 删除排班
     */
    @DeleteMapping("/plans/{id}")
    public Result<Void> deletePlan(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "请先登录");

        scheduleService.deletePlan(id);
        return Result.success(null);
    }

    /**
     * 自动排班
     */
    @PostMapping("/auto")
    public Result<List<InspectionPlan>> autoSchedule(@RequestParam String weekStart,
                                                      HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "请先登录");

        LocalDate date = LocalDate.parse(weekStart);
        List<InspectionPlan> plans = scheduleService.autoSchedule(date);
        return Result.success(plans);
    }

    /**
     * 查看某计划的巡检记录详情
     */
    @GetMapping("/plans/{id}/records")
    public Result<List<InspectionRecord>> getPlanRecords(@PathVariable Long id,
                                                          HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "请先登录");

        List<InspectionRecord> records = scheduleService.getPlanRecords(id);
        return Result.success(records);
    }

    /**
     * 获取所有在职巡检员列表
     */
    @GetMapping("/inspectors")
    public Result<List<User>> getInspectors(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "请先登录");

        return Result.success(scheduleService.getInspectors());
    }

    /**
     * 获取所有车间列表
     */
    @GetMapping("/workshops")
    public Result<List<Map<String, Object>>> getWorkshops(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "请先登录");

        return Result.success(scheduleService.getWorkshops());
    }
}
