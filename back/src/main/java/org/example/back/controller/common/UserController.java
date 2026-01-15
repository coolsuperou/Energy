package org.example.back.controller.common;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.example.back.common.Result;
import org.example.back.dto.LoginRequest;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.RegisterRequest;
import org.example.back.dto.UserDTO;
import org.example.back.entity.User;
import org.example.back.entity.enums.UserRole;
import org.example.back.service.common.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 * 提供用户认证、用户管理等功能
 */
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        LoginResponse response = userService.login(request);
        // 获取完整的 User 对象存入 Session
        User user = userService.getUserEntityById(response.getUserId());
        session.setAttribute("user", user);
        session.setAttribute("userId", response.getUserId());
        session.setAttribute("username", response.getUsername());
        session.setAttribute("role", response.getRole());
        return Result.success("登录成功", response);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        UserDTO user = userService.register(request);
        return Result.success("注册成功，请等待管理员分配角色", user);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.success("登出成功", null);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public Result<UserDTO> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        UserDTO user = userService.getUserById(userId);
        return Result.success(user);
    }

    /**
     * 获取用户列表
     * 支持按角色筛选，用于派单等场景
     * 
     * @param role 用户角色，可选参数
     * @return 用户列表
     */
    @GetMapping
    public Result<List<UserDTO>> getUserList(@RequestParam(required = false) UserRole role) {
        List<UserDTO> users = userService.getUserList(role);
        return Result.success(users);
    }

    /**
     * 根据ID获取用户详情
     * 
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return Result.success(user);
    }
}
