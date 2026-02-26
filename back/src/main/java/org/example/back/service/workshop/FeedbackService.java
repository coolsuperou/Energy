package org.example.back.service.workshop;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.back.entity.Feedback;
import org.example.back.entity.Task;
import org.example.back.entity.enums.Priority;
import org.example.back.entity.enums.TaskStatus;
import org.example.back.entity.enums.TaskType;
import org.example.back.mapper.workshop.FeedbackMapper;
import org.example.back.mapper.dispatcher.TaskMapper;
import org.example.back.service.common.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FeedbackService extends ServiceImpl<FeedbackMapper, Feedback> {

    @Autowired
    private TaskMapper taskMapper;
    
    @Autowired
    private NotificationService notificationService;

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

    /**
     * 反馈转工单（派单给巡检员）
     * 基于反馈内容自动创建维修任务，关联反馈ID，更新反馈状态为processing
     * 
     * @param feedbackId 反馈ID
     * @param assigneeId 巡检员ID
     * @param dispatcherId 调度员ID（派单人）
     * @return 创建的任务
     */
    @Transactional
    public Task dispatchFeedback(Long feedbackId, Long assigneeId, Long dispatcherId) {
        // 1. 获取反馈信息
        Feedback feedback = this.getById(feedbackId);
        if (feedback == null) {
            throw new RuntimeException("反馈不存在");
        }
        
        // 2. 验证反馈状态（只有待处理状态可以派单）
        if (!"pending".equals(feedback.getStatus())) {
            throw new RuntimeException("只有待处理状态的反馈可以派单");
        }
        
        // 3. 验证反馈类型（只有故障类反馈可以派单）
        if (!"fault".equals(feedback.getType())) {
            throw new RuntimeException("只有故障类反馈可以派单");
        }
        
        // 4. 创建维修任务
        Task task = new Task();
        task.setTaskType(TaskType.REPAIR);
        task.setTitle("【反馈转工单】" + feedback.getLocation() + " - 故障维修");
        task.setDescription("来源反馈编号：" + feedback.getFeedbackNo() + "\n" +
                           "问题位置：" + feedback.getLocation() + "\n" +
                           "问题描述：" + feedback.getDescription());
        task.setFeedbackId(feedbackId);
        task.setEquipmentId(feedback.getEquipmentId());
        task.setAssignedTo(assigneeId);
        task.setAssignedBy(dispatcherId);
        task.setStatus(TaskStatus.IN_PROGRESS); // 派单后直接进入进行中状态
        task.setCreatedAt(LocalDateTime.now());
        
        // 根据反馈紧急程度设置任务优先级
        if ("critical".equals(feedback.getUrgency())) {
            task.setPriority(Priority.HIGH);
            task.setDueDate(LocalDate.now()); // 紧急任务当天截止
        } else if ("urgent".equals(feedback.getUrgency())) {
            task.setPriority(Priority.HIGH);
            task.setDueDate(LocalDate.now().plusDays(1)); // 高优先级次日截止
        } else {
            task.setPriority(Priority.NORMAL);
            task.setDueDate(LocalDate.now().plusDays(3)); // 普通优先级3天内截止
        }
        
        taskMapper.insert(task);
        
        // 5. 更新反馈状态为processing
        feedback.setStatus("processing");
        feedback.setHandledBy(dispatcherId);
        feedback.setHandledAt(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        this.updateById(feedback);
        
        // 6. 发送任务分配通知给巡检员
        notificationService.createNotification(
            assigneeId,
            "task",
            "新维修任务分配",
            "您有一个新的维修任务：" + task.getTitle() + "，请及时处理。",
            task.getId()
        );
        
        return task;
    }
}
