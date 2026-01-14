package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.back.entity.Notification;
import org.example.back.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

@Service
public class NotificationService extends ServiceImpl<NotificationMapper, Notification> {

    public int getUnreadCount(Long userId) {
        return baseMapper.countUnread(userId);
    }

    public IPage<Notification> getUserNotifications(Long userId, String type, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Notification::getType, type);
        }
        wrapper.orderByDesc(Notification::getCreatedAt);
        return this.page(new Page<>(page, size), wrapper);
    }

    public void markAsRead(Long id, Long userId) {
        baseMapper.markRead(id, userId);
    }

    public void markAllAsRead(Long userId) {
        baseMapper.markAllRead(userId);
    }

    public void createNotification(Long userId, String type, String title, String content, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setIsRead(false);
        this.save(notification);
    }
}
