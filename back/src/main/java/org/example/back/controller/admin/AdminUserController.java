package org.example.back.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.User;
import org.example.back.entity.enums.UserStatus;
import org.example.back.service.admin.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统管理员用户管理控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	系统管理员用户CRUD和启用/禁用接口
 * -- =============================================
 */
@RestController
@RequestMapping("/admin")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    /**
     * 获取用户列表（分页）
     * 
     * @param page 当前页，默认1
     * @param size 每页大小，默认10
     * @param role 角色筛选（可选）
     * @param keyword 关键词搜索（可选）
     */
    @GetMapping("/users")
    public Result<IPage<User>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        IPage<User> users = adminUserService.getUserList(page, size, role, keyword);
        return Result.success(users);
    }

    /**
     * 获取用户详情
     * 
     * @param id 用户ID
     */
    @GetMapping("/users/{id}")
    public Result<User> getUserById(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        User user = adminUserService.getUserById(id);
        return Result.success(user);
    }

    /**
     * 创建用户
     * 
     * @param user 用户信息
     */
    @PostMapping("/users")
    public Result<User> createUser(@RequestBody User user, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        User createdUser = adminUserService.createUser(user);
        return Result.success("用户创建成功", createdUser);
    }

    /**
     * 更新用户信息
     * 
     * @param id 用户ID
     * @param user 更新的用户信息
     */
    @PutMapping("/users/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody User user, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        User updatedUser = adminUserService.updateUser(id, user);
        return Result.success("用户更新成功", updatedUser);
    }

    /**
     * 删除用户
     * 
     * @param id 用户ID
     */
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        adminUserService.deleteUser(id);
        return Result.success("用户删除成功", null);
    }

    /**
     * 启用/禁用用户
     * 
     * @param id 用户ID
     * @param requestBody 包含status字段
     */
    @PutMapping("/users/{id}/status")
    public Result<User> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        String statusStr = requestBody.get("status");
        if (statusStr == null) {
            return Result.error("状态参数不能为空");
        }
        
        UserStatus status;
        try {
            status = UserStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Result.error("无效的状态值");
        }
        
        User updatedUser = adminUserService.updateUserStatus(id, status);
        return Result.success("用户状态更新成功", updatedUser);
    }

    /**
     * 重置用户密码
     * 
     * @param id 用户ID
     * @param requestBody 包含password字段
     */
    @PutMapping("/users/{id}/reset-password")
    public Result<User> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error(401, "未登录");
        }
        
        String newPassword = requestBody.get("password");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.error("新密码不能为空");
        }
        
        User updatedUser = adminUserService.resetPassword(id, newPassword);
        return Result.success("密码重置成功", updatedUser);
    }
}
