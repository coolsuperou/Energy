package org.example.back.controller.workshop;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.Feedback;
import org.example.back.entity.User;
import org.example.back.service.workshop.FeedbackService;
import org.example.back.service.common.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

import org.example.back.entity.Task;
import org.example.back.entity.enums.UserRole;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;
    
    @Autowired
    private MinioService minioService;

    /**
     * 提交反馈
     */
    @PostMapping
    public Result<Feedback> submit(@RequestBody Feedback feedback, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        feedback.setUserId(user.getId());
        Feedback saved = feedbackService.submitFeedback(feedback);
        return Result.success(saved);
    }

    /**
     * 获取我的反馈列表
     */
    @GetMapping("/my")
    public Result<IPage<Feedback>> getMyFeedbacks(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        IPage<Feedback> result = feedbackService.getUserFeedbacks(user.getId(), status, page, size);
        return Result.success(result);
    }

    /**
     * 获取所有反馈（管理端）
     */
    @GetMapping("/all")
    public Result<IPage<Feedback>> getAllFeedbacks(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        IPage<Feedback> result = feedbackService.getAllFeedbacks(status, type, page, size);
        return Result.success(result);
    }

    /**
     * 获取反馈详情
     */
    @GetMapping("/{id}")
    public Result<Feedback> getDetail(@PathVariable Long id) {
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return Result.error(404, "反馈不存在");
        }
        return Result.success(feedback);
    }

    /**
     * 撤回反馈
     */
    @PostMapping("/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        feedbackService.withdrawFeedback(id, user.getId());
        return Result.success(null);
    }

    /**
     * 处理反馈（调度员/管理员）
     */
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        String reply = (String) body.get("reply");
        String status = (String) body.get("status");
        feedbackService.handleFeedback(id, reply, status, user.getId());
        return Result.success(null);
    }

    /**
     * 上传反馈图片（支持多图上传）
     * 图片URL存入feedbacks表的images字段，逗号分隔
     */
    @PostMapping("/{id}/images")
    public Result<List<Map<String, String>>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") MultipartFile[] files,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return Result.error(404, "反馈不存在");
        }
        if (!feedback.getUserId().equals(user.getId())) {
            return Result.error(403, "无权为此反馈上传图片");
        }
        
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String path = minioService.uploadFile(file, user.getId(), MinioService.FileType.FEEDBACK);
                String fullUrl = minioService.getFileUrl(path);
                urls.add(fullUrl);
            }
        }
        
        // 合并已有图片
        String existing = feedback.getImages();
        if (existing != null && !existing.isEmpty()) {
            urls.addAll(0, Arrays.asList(existing.split(",")));
        }
        feedback.setImages(String.join(",", urls));
        feedbackService.updateById(feedback);
        
        // 返回兼容旧格式
        List<Map<String, String>> result = urls.stream().map(url -> {
            Map<String, String> m = new HashMap<>();
            m.put("imageUrl", url);
            m.put("accessUrl", url);
            return m;
        }).collect(Collectors.toList());
        
        return Result.success(result);
    }

    /**
     * 获取反馈图片列表
     */
    @GetMapping("/{id}/images")
    public Result<List<Map<String, String>>> getImages(@PathVariable Long id) {
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return Result.error(404, "反馈不存在");
        }
        
        List<Map<String, String>> result = new ArrayList<>();
        String images = feedback.getImages();
        if (images != null && !images.isEmpty()) {
            for (String url : images.split(",")) {
                Map<String, String> m = new HashMap<>();
                m.put("imageUrl", url);
                m.put("accessUrl", url);
                result.add(m);
            }
        }
        return Result.success(result);
    }

    /**
     * 反馈转工单（派单给巡检员）
     * 将故障类反馈转为维修工单，分配给巡检员
     * 基于反馈内容自动创建维修任务（task_type=REPAIR），关联反馈ID
     * 反馈状态更新为processing，派单后巡检员收到任务分配通知
     */
    @PostMapping("/{id}/dispatch")
    public Result<Task> dispatch(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        
        // 验证用户角色（只有调度员可以派单）
        if (user.getRole() != UserRole.DISPATCHER) {
            return Result.error(403, "只有调度员可以进行派单操作");
        }
        
        // 获取巡检员ID
        Object assigneeIdObj = body.get("assigneeId");
        if (assigneeIdObj == null) {
            return Result.error(400, "请选择巡检员");
        }
        
        Long assigneeId;
        if (assigneeIdObj instanceof Integer) {
            assigneeId = ((Integer) assigneeIdObj).longValue();
        } else if (assigneeIdObj instanceof Long) {
            assigneeId = (Long) assigneeIdObj;
        } else {
            assigneeId = Long.parseLong(assigneeIdObj.toString());
        }
        
        Task task = feedbackService.dispatchFeedback(id, assigneeId, user.getId());
        return Result.success(task);
    }
}
