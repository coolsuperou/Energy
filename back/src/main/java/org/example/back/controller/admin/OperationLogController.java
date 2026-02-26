package org.example.back.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.OperationLog;
import org.example.back.entity.User;
import org.example.back.service.admin.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	系统管理员查看操作日志接口
 * -- =============================================
 */
@RestController
@RequestMapping("/admin")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    /**
     * 获取操作日志列表（分页）
     * 
     * @param page 当前页，默认1
     * @param size 每页大小，默认10
     * @param operationType 操作类型筛选（可选）
     * @param userId 用户ID筛选（可选）
     * @param startDate 开始日期筛选（可选，格式：yyyy-MM-dd）
     * @param endDate 结束日期筛选（可选，格式：yyyy-MM-dd）
     */
    @GetMapping("/logs")
    public Result<IPage<OperationLog>> getLogList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        IPage<OperationLog> logs = operationLogService.getLogList(page, size, operationType, userId, startDate, endDate);
        return Result.success(logs);
    }

    /**
     * 获取所有操作类型（用于筛选下拉框）
     */
    @GetMapping("/logs/types")
    public Result<?> getOperationTypes(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        return Result.success(operationLogService.getOperationTypes());
    }
}
