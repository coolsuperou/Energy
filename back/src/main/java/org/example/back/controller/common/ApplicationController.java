package org.example.back.controller.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.example.back.common.Result;
import org.example.back.dto.ApplicationRequest;
import org.example.back.dto.ApprovalRequest;
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

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

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
     * 获取待审批申请列表（调度员）
     */
    @GetMapping("/pending")
    public Result<IPage<Application>> getPendingApplications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long workshopId,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        IPage<Application> result = applicationService.getPendingApplications(page, size, workshopId);
        return Result.success(result);
    }

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
        application.setWorkshopId(user.getId());
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
     * 获取申请列表
     */
    @GetMapping
    public Result<IPage<Application>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long workshopId,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        IPage<Application> result;
        String role = user.getRole().getValue();
        
        if ("workshop".equals(role)) {
            result = applicationService.getUserApplications(user.getId(), page, size, status);
        } else if ("dispatcher".equals(role)) {
            result = applicationService.getPendingApplications(page, size, workshopId);
        } else {
            result = applicationService.getAllApplications(page, size, status, workshopId);
        }
        
        return Result.success(result);
    }

    /**
     * 获取申请详情
     */
    @GetMapping("/{id}")
    public Result<Application> getById(@PathVariable Long id) {
        Application application = applicationService.getById(id);
        if (application == null) {
            return Result.error(404, "申请不存在");
        }
        return Result.success(application);
    }

    /**
     * 批准申请
     */
    @PutMapping("/{id}/approve")
    public Result<Application> approve(@PathVariable Long id, 
                                       @RequestBody ApprovalRequest request,
                                       HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        
        Application result = applicationService.approve(id, user.getId(), request.getComment());
        return Result.success(result);
    }

    /**
     * 拒绝申请
     */
    @PutMapping("/{id}/reject")
    public Result<Application> reject(@PathVariable Long id,
                                      @RequestBody ApprovalRequest request,
                                      HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        
        Application result = applicationService.reject(id, user.getId(), request.getComment());
        return Result.success(result);
    }

    /**
     * 调整并批准
     */
    @PutMapping("/{id}/adjust")
    public Result<Application> adjust(@PathVariable Long id,
                                      @RequestBody ApprovalRequest request,
                                      HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        
        Application result = applicationService.adjust(id, user.getId(), request.getComment(),
                request.getAdjustedStartTime(), request.getAdjustedEndTime());
        return Result.success(result);
    }
}
