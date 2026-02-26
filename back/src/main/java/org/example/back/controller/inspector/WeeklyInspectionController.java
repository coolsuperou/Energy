package org.example.back.controller.inspector;

import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.InspectionPlan;
import org.example.back.entity.User;
import org.example.back.service.inspector.InspectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 每周巡检控制器
 * 巡检员执行每周设备巡检
 *
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	巡检员每周巡检执行接口
 * -- =============================================
 */
@RestController
@RequestMapping("/weekly-inspection")
public class WeeklyInspectionController {

    @Autowired
    private InspectionService inspectionService;

    /**
     * 获取我的本周巡检计划
     */
    @GetMapping("/plans")
    public Result<List<InspectionPlan>> getMyPlans(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "请先登录");

        List<InspectionPlan> plans = inspectionService.getMyWeekPlans(user.getId());
        return Result.success(plans);
    }

    /**
     * 获取某计划下的设备巡检记录（按设备分组）
     */
    @GetMapping("/plans/{id}/records")
    public Result<List<Map<String, Object>>> getPlanRecords(@PathVariable Long id,
                                                             HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "请先登录");

        List<Map<String, Object>> data = inspectionService.getPlanRecordsByEquipment(id);
        return Result.success(data);
    }

    /**
     * 更新检查结果
     */
    @PutMapping("/records/{id}")
    public Result<Void> updateRecord(@PathVariable Long id,
                                      @RequestBody Map<String, String> body,
                                      HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "请先登录");

        String result = body.get("result");
        String remark = body.get("remark");
        inspectionService.updateCheckResult(id, result, remark);
        return Result.success(null);
    }

    /**
     * 异常转报修
     */
    @PostMapping("/records/{id}/repair")
    public Result<Void> convertToRepair(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "请先登录");

        inspectionService.convertToRepair(id, user.getId());
        return Result.success(null);
    }

    /**
     * 恢复报修设备为正常状态
     */
    @PostMapping("/plans/{planId}/equipment/{equipmentId}/restore")
    public Result<Void> restoreFromRepair(@PathVariable Long planId,
                                           @PathVariable Long equipmentId,
                                           HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return Result.error(401, "请先登录");

        inspectionService.restoreFromRepair(planId, equipmentId);
        return Result.success(null);
    }
}
