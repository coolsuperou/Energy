package org.example.back.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.Feedback;
import org.example.back.entity.User;
import org.example.back.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

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
}
