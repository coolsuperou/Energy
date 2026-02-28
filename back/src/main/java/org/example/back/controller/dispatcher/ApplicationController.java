package org.example.back.controller.dispatcher;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.dto.ApprovalRequest;
import org.example.back.entity.Application;
import org.example.back.entity.User;
import org.example.back.service.workshop.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用电申请审批控制器（调度员）
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	调度员审批用电申请相关接口
 * -- =============================================
 */
@RestController
@RequestMapping("/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

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
     * 获取申请列表（调度员/经理）
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

        // 调度员和经理都可以按状态筛选查看所有申请
        IPage<Application> result = applicationService.getAllApplications(page, size, status, workshopId);
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
