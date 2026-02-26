package org.example.back.controller.admin;

import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.dto.SystemConfigDTO;
import org.example.back.entity.User;
import org.example.back.service.admin.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	系统配置管理接口，包含电价参数、预警阈值、时段配置
 * -- =============================================
 */
@RestController
@RequestMapping("/admin")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    /**
     * 获取系统配置
     * GET /api/admin/config
     * 
     * @return 系统配置信息
     */
    @GetMapping("/config")
    public Result<SystemConfigDTO> getConfig(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        SystemConfigDTO config = systemConfigService.getConfig();
        return Result.success(config);
    }

    /**
     * 更新系统配置
     * PUT /api/admin/config
     * 
     * @param config 新的配置参数
     * @return 更新后的配置
     */
    @PutMapping("/config")
    public Result<SystemConfigDTO> updateConfig(
            @RequestBody SystemConfigDTO config,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        SystemConfigDTO updatedConfig = systemConfigService.updateConfig(config);
        return Result.success("配置更新成功", updatedConfig);
    }
}
