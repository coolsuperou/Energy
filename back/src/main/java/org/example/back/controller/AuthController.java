package org.example.back.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.example.back.common.Result;
import org.example.back.dto.LoginRequest;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.RegisterRequest;
import org.example.back.dto.UserDTO;
import org.example.back.entity.User;
import org.example.back.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

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

    @PostMapping("/register")
    public Result<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        UserDTO user = userService.register(request);
        return Result.success("注册成功，请等待管理员分配角色", user);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.success("登出成功", null);
    }

    @GetMapping("/current")
    public Result<UserDTO> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        UserDTO user = userService.getUserById(userId);
        return Result.success(user);
    }
}
