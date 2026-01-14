package org.example.back.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.back.entity.Feedback;
import org.example.back.mapper.FeedbackMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FeedbackService extends ServiceImpl<FeedbackMapper, Feedback> {

    public String generateFeedbackNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = this.count() + 1;
        return String.format("FB%s%04d", dateStr, count);
    }

    public Feedback submitFeedback(Feedback feedback) {
        feedback.setFeedbackNo(generateFeedbackNo());
        feedback.setStatus("pending");
        feedback.setCreateTime(LocalDateTime.now());
        this.save(feedback);
        return feedback;
    }

    public boolean withdrawFeedback(Long id, Long userId) {
        Feedback feedback = this.getById(id);
        if (feedback == null) {
            throw new RuntimeException("反馈不存在");
        }
        if (!feedback.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此反馈");
        }
        if (!"pending".equals(feedback.getStatus())) {
            throw new RuntimeException("只有待处理状态的反馈可以撤回");
        }
        feedback.setStatus("withdrawn");
        feedback.setUpdateTime(LocalDateTime.now());
        return this.updateById(feedback);
    }

    public boolean handleFeedback(Long id, String reply, String status, Long handlerId) {
        Feedback feedback = this.getById(id);
        if (feedback == null) {
            throw new RuntimeException("反馈不存在");
        }
        feedback.setReply(reply);
        feedback.setStatus(status);
        feedback.setHandledBy(handlerId);
        feedback.setHandledAt(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        return this.updateById(feedback);
    }

    public IPage<Feedback> getUserFeedbacks(Long userId, String status, int page, int size) {
        return baseMapper.selectByUserId(new Page<>(page, size), userId, status);
    }

    public IPage<Feedback> getAllFeedbacks(String status, String type, int page, int size) {
        return baseMapper.selectAllFeedbacks(new Page<>(page, size), status, type);
    }
}
