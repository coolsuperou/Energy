package org.example.back.controller.workshop;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.example.back.common.Result;
import org.example.back.dto.ApplicationRequest;
import org.example.back.entity.Application;
import org.example.back.entity.User;
import org.example.back.entity.enums.Urgency;
import org.example.back.service.workshop.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用电申请控制器（车间用户）
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	车间用户提交用电申请相关接口
 * -- =============================================
 */
@RestController
@RequestMapping("/applications")
public class WorkshopApplicationController {

    @Autowired
    private ApplicationService applicationService;

    /**
     * 提交用电申请
     */
    @PostMapping
    public Result<Application> submit(@Valid @RequestBody ApplicationRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        Application application = new Application();
        application.setUserId(user.getId());
        application.setWorkshopId(parseWorkshopIdFromDepartment(user.getDepartment(), user.getId()));
        application.setEquipmentId(request.getEquipmentId());
        application.setPower(request.getPower());
        application.setApplyDate(request.getApplyDate());
        application.setStartTime(request.getStartTime());
        application.setEndTime(request.getEndTime());
        application.setPurpose(request.getPurpose());
        application.setUrgency(Urgency.valueOf(request.getUrgency().toUpperCase()));

        Application result = applicationService.submitApplication(application);
        return Result.success(result);
    }

    /**
     * 获取我的申请列表（车间用户）
     */
    @GetMapping("/my")
    public Result<IPage<Application>> getMyApplications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        IPage<Application> result = applicationService.getUserApplications(user.getId(), page, size, status);
        return Result.success(result);
    }

    /**
     * 撤回用电申请（仅待审批状态可撤回）
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        applicationService.cancel(id, user.getId());
        return Result.success(null);
    }

    /**
     * 从部门名称解析车间ID
     * 例如："第一车间" → 1, "第二车间" → 2
     * 如果解析失败，返回 fallback 值
     */
    private Long parseWorkshopIdFromDepartment(String department, Long fallback) {
        if (department == null) return fallback;
        Long workshopId = Map.of(
            "第一车间", 1L, "第二车间", 2L,
            "第三车间", 3L, "第四车间", 4L
        ).get(department);
        return workshopId != null ? workshopId : fallback;
    }
}
