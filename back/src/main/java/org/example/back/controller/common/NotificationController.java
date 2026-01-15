package org.example.back.controller.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.Notification;
import org.example.back.entity.User;
import org.example.back.service.common.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Integer>> getUnreadCount(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        int count = notificationService.getUnreadCount(user.getId());
        Map<String, Integer> data = new HashMap<>();
        data.put("count", count);
        return Result.success(data);
    }

    /**
     * 获取我的消息列表
     */
    @GetMapping("/my")
    public Result<IPage<Notification>> getMyNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        IPage<Notification> result = notificationService.getUserNotifications(user.getId(), type, page, size);
        return Result.success(result);
    }

    /**
     * 标记单条消息已读
     */
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        notificationService.markAsRead(id, user.getId());
        return Result.success(null);
    }

    /**
     * 标记所有消息已读
     */
    @PostMapping("/read-all")
    public Result<Void> markAllRead(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        notificationService.markAllAsRead(user.getId());
        return Result.success(null);
    }
}
