package org.example.back.controller.common;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.example.back.common.Result;
import org.example.back.dto.LoginRequest;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.RegisterRequest;
import org.example.back.dto.UserDTO;
import org.example.back.entity.User;
import org.example.back.entity.SkillCertification;
import org.example.back.entity.enums.CertificationStatus;
import org.example.back.service.inspector.SkillService;
import org.example.back.entity.enums.UserRole;
import org.example.back.service.common.MinioService;
import org.example.back.service.common.UserService;
import org.example.back.mapper.dispatcher.TaskMapper;
import org.example.back.entity.enums.TaskStatus;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 用户控制器
 * 提供用户认证、用户管理等功能
 */
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private MinioService minioService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private SkillService skillService;

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
        return Result.success("注册成功", user);
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

    /**
     * 获取用户统计数据
     */
    @GetMapping("/current/stats")
    public Result<Map<String, Object>> getUserStats(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        Long userId = currentUser.getId();
        Map<String, Object> stats = new HashMap<>();
        
        // 本月完成工单
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        QueryWrapper<org.example.back.entity.Task> monthlyWrapper = new QueryWrapper<>();
        monthlyWrapper.eq("assigned_to", userId);
        monthlyWrapper.eq("status", TaskStatus.COMPLETED);
        monthlyWrapper.ge("completed_at", firstDayOfMonth.atStartOfDay());
        Long monthlyCompleted = taskMapper.selectCount(monthlyWrapper);
        stats.put("monthlyCompleted", monthlyCompleted.intValue());
        
        // 按时完成率
        QueryWrapper<org.example.back.entity.Task> totalMonthWrapper = new QueryWrapper<>();
        totalMonthWrapper.eq("assigned_to", userId);
        totalMonthWrapper.ge("created_at", firstDayOfMonth.atStartOfDay());
        Long totalMonthTasks = taskMapper.selectCount(totalMonthWrapper);
        
        if (totalMonthTasks > 0) {
            // 按时完成 = 在截止日期前完成的
            QueryWrapper<org.example.back.entity.Task> onTimeWrapper = new QueryWrapper<>();
            onTimeWrapper.eq("assigned_to", userId);
            onTimeWrapper.eq("status", TaskStatus.COMPLETED);
            onTimeWrapper.ge("completed_at", firstDayOfMonth.atStartOfDay());
            onTimeWrapper.apply("DATE(completed_at) <= due_date");
            Long onTimeCount = taskMapper.selectCount(onTimeWrapper);
            stats.put("onTimeRate", Math.round(onTimeCount * 100.0 / monthlyCompleted));
        } else {
            stats.put("onTimeRate", 100);
        }
        
        // 累计完成工单
        QueryWrapper<org.example.back.entity.Task> totalWrapper = new QueryWrapper<>();
        totalWrapper.eq("assigned_to", userId);
        totalWrapper.eq("status", TaskStatus.COMPLETED);
        Long totalCompleted = taskMapper.selectCount(totalWrapper);
        stats.put("totalCompleted", totalCompleted.intValue());
        
        return Result.success(stats);
    }

    /**
     * 获取用户技能认证
     */
    @GetMapping("/current/skills")
    public Result<List<Map<String, Object>>> getUserSkills(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        List<SkillCertification> certifications = skillService.getUserSkills(currentUser.getId());
        List<Map<String, Object>> skills = new ArrayList<>();

        for (SkillCertification cert : certifications) {
            Map<String, Object> skill = new HashMap<>();
            skill.put("id", cert.getId());
            skill.put("name", cert.getSkillName());
            skill.put("status", cert.getStatus().getValue());
            skill.put("certificateUrl", cert.getCertificateUrl());
            skill.put("icon", "bi bi-award");

            if (cert.getStatus() == CertificationStatus.CERTIFIED && cert.getReviewedAt() != null) {
                skill.put("certifiedAt", cert.getReviewedAt().toLocalDate().toString());
            } else {
                skill.put("appliedAt", cert.getCreatedAt() != null ? cert.getCreatedAt().toLocalDate().toString() : null);
            }

            if (cert.getStatus() == CertificationStatus.REJECTED) {
                skill.put("rejectReason", cert.getRejectReason());
            }

            skills.add(skill);
        }

        return Result.success(skills);
    }

    /**
     * 获取用户排班信息（调用考勤接口）
     */
    @GetMapping("/current/schedule")
    public Result<List<Map<String, Object>>> getUserSchedule(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        // 返回本周排班，实际应该调用 AttendanceService.getWeeklySchedule
        List<Map<String, Object>> schedule = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        String[] times = {"08:30-17:30", "08:30-17:30", "08:30-17:30", "08:30-17:30", "08:30-17:30", "休息", "休息"};
        String[] labels = {"白班", "白班", "白班", "白班", "白班", "休息", "休息"};
        String[] types = {"day", "day", "day", "day", "day", "rest", "rest"};
        
        for (int i = 0; i < 7; i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("day", days[i]);
            day.put("time", times[i]);
            day.put("label", labels[i]);
            day.put("type", types[i]);
            day.put("date", monday.plusDays(i).toString());
            schedule.add(day);
        }
        
        return Result.success(schedule);
    }

    /**
     * 获取工作量统计
     */
    @GetMapping("/current/work-stats")
    public Result<Map<String, Object>> getWorkStats(
            @RequestParam(defaultValue = "7") int days,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        Map<String, Object> stats = new HashMap<>();
        Long userId = currentUser.getId();
        
        List<String> dates = new ArrayList<>();
        List<Integer> completed = new ArrayList<>();
        List<Integer> pending = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date.getMonthValue() + "/" + date.getDayOfMonth());
            
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
            
            // 查询当天完成的工单数
            QueryWrapper<org.example.back.entity.Task> completedWrapper = new QueryWrapper<>();
            completedWrapper.eq("assigned_to", userId);
            completedWrapper.eq("status", TaskStatus.COMPLETED);
            completedWrapper.ge("completed_at", dayStart);
            completedWrapper.lt("completed_at", dayEnd);
            Long completedCount = taskMapper.selectCount(completedWrapper);
            completed.add(completedCount.intValue());
            
            // 查询当天创建/分配的工单数（巡检次数）
            QueryWrapper<org.example.back.entity.Task> createdWrapper = new QueryWrapper<>();
            createdWrapper.eq("assigned_to", userId);
            createdWrapper.ge("created_at", dayStart);
            createdWrapper.lt("created_at", dayEnd);
            Long createdCount = taskMapper.selectCount(createdWrapper);
            pending.add(createdCount.intValue());
        }
        
        stats.put("dates", dates);
        stats.put("completed", completed);
        stats.put("pending", pending);
        
        return Result.success(stats);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/current")
    public Result<UserDTO> updateUserInfo(@RequestBody Map<String, String> data, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        // 更新用户信息
        if (data.containsKey("phone")) {
            currentUser.setPhone(data.get("phone"));
        }
        if (data.containsKey("email")) {
            currentUser.setEmail(data.get("email"));
        }
        
        userService.updateUser(currentUser);
        
        UserDTO userDTO = userService.getUserById(currentUser.getId());
        return Result.success("更新成功", userDTO);
    }

    /**
     * 上传用户头像
     * 
     * @param file 头像文件
     * @param session HTTP会话
     * @return 上传结果,包含头像访问URL
     */
    @PostMapping("/current/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        
        // 1. 检查登录状态
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        // 2. 验证文件
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }

        // 3. 验证文件类型(只允许图片)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }

        // 4. 验证文件大小(最大15MB)
        if (file.getSize() > 15 * 1024 * 1024) {
            return Result.error("文件大小不能超过15MB");
        }

        try {
            // 5. 删除旧头像(如果存在)
            String oldAvatarUrl = currentUser.getAvatarUrl();
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                minioService.deleteFile(oldAvatarUrl);
            }

            // 6. 上传新头像到MinIO
            String filePath = minioService.uploadFile(file, currentUser.getId());

            // 7. 更新数据库中的头像URL
            currentUser.setAvatarUrl(filePath);
            userService.updateUser(currentUser);

            // 8. 更新Session中的用户信息
            session.setAttribute("user", currentUser);

            // 9. 返回完整的访问URL
            String fullUrl = minioService.getFileUrl(filePath);
            Map<String, String> result = new HashMap<>();
            result.put("avatarUrl", fullUrl);

            return Result.success("头像上传成功", result);

        } catch (Exception e) {
            return Result.error("头像上传失败: " + e.getMessage());
        }
    }

    /**
     * 修改密码
     * 
     * @param data 包含oldPassword和newPassword
     * @param session HTTP会话
     * @return 修改结果
     */
    @PutMapping("/current/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> data, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        String oldPassword = data.get("oldPassword");
        String newPassword = data.get("newPassword");

        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error("请输入原密码");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error("请输入新密码");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            return Result.error("新密码长度必须在6-20之间");
        }

        // 验证原密码（开发环境：明文比较）
        if (!oldPassword.equals(currentUser.getPassword())) {
            return Result.error("原密码错误");
        }

        // 更新密码
        currentUser.setPassword(newPassword);
        userService.updateUser(currentUser);

        // 更新Session
        session.setAttribute("user", currentUser);

        return Result.success("密码修改成功", null);
    }

    /**
     * 获取用户头像URL
     * 
     * @param session HTTP会话
     * @return 头像访问URL
     */
    @GetMapping("/current/avatar")
    public Result<Map<String, String>> getAvatar(HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("未登录");
        }

        String avatarUrl = currentUser.getAvatarUrl();
        String fullUrl = minioService.getFileUrl(avatarUrl);
        
        Map<String, String> result = new HashMap<>();
        result.put("avatarUrl", fullUrl);
        
        return Result.success(result);
    }
}
