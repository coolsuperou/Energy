package org.example.back.service.inspector;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.back.entity.Equipment;
import org.example.back.entity.Task;
import org.example.back.entity.User;
import org.example.back.entity.enums.TaskStatus;
import org.example.back.mapper.common.EquipmentMapper;
import org.example.back.mapper.common.UserMapper;
import org.example.back.mapper.dispatcher.TaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 巡检员任务服务类
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2026
 * -- Description:	设备巡检员我的任务查询和完成工单逻辑
 * -- =============================================
 */
@Service
public class InspectorTaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    /**
     * 获取我的任务列表
     * 支持按状态筛选
     * 
     * @param userId 用户ID
     * @param status 任务状态（可选）
     * @return 任务列表
     */
    public List<Task> getMyTasks(Long userId, TaskStatus status) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getAssignedTo, userId);
        
        if (status != null) {
            wrapper.eq(Task::getStatus, status);
        }
        
        wrapper.orderByDesc(Task::getCreatedAt);
        
        List<Task> tasks = taskMapper.selectList(wrapper);
        tasks.forEach(this::fillTaskInfo);
        
        return tasks;
    }

    /**
     * 获取今日待完成任务
     * 查询进行中且截止日期在今天或之前的任务
     * 
     * @param userId 用户ID
     * @return 今日待完成任务列表
     */
    public List<Task> getTodayTasks(Long userId) {
        LocalDate today = LocalDate.now();
        
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getAssignedTo, userId);
        wrapper.eq(Task::getStatus, TaskStatus.IN_PROGRESS);
        wrapper.and(w -> w.isNull(Task::getDueDate).or().le(Task::getDueDate, today));
        wrapper.orderByAsc(Task::getDueDate);
        wrapper.orderByDesc(Task::getPriority);
        
        List<Task> tasks = taskMapper.selectList(wrapper);
        tasks.forEach(this::fillTaskInfo);
        
        return tasks;
    }

    /**
     * 完成工单
     * 巡检员填写巡检报告完成任务
     * 
     * @param taskId 任务ID
     * @param report 巡检报告内容
     * @param reportImages 完成图片URL（逗号分隔，可为null）
     * @param userId 当前用户ID
     */
    public void completeTask(Long taskId, String report, String reportImages, Long userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("工单不存在");
        }
        
        // 验证只能完成分配给自己的任务
        if (!userId.equals(task.getAssignedTo())) {
            throw new RuntimeException("只能完成分配给自己的工单");
        }
        
        // 验证任务状态必须是进行中
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new RuntimeException("只能完成进行中的工单");
        }
        
        task.setStatus(TaskStatus.COMPLETED);
        task.setReport(report);
        task.setReportImages(reportImages);
        task.setCompletedAt(LocalDateTime.now());
        
        taskMapper.updateById(task);
    }

    /**
     * 获取我的任务统计
     * 返回进行中任务数、今日完成数、本月完成数等统计数据
     * 
     * @param userId 用户ID
     * @return 任务统计数据
     */
    public Map<String, Object> getMyTaskStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 进行中任务数
        LambdaQueryWrapper<Task> inProgressWrapper = new LambdaQueryWrapper<>();
        inProgressWrapper.eq(Task::getAssignedTo, userId);
        inProgressWrapper.eq(Task::getStatus, TaskStatus.IN_PROGRESS);
        Long inProgressCount = taskMapper.selectCount(inProgressWrapper);
        stats.put("inProgress", inProgressCount);
        
        // 今日完成数
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Task> todayCompletedWrapper = new LambdaQueryWrapper<>();
        todayCompletedWrapper.eq(Task::getAssignedTo, userId);
        todayCompletedWrapper.eq(Task::getStatus, TaskStatus.COMPLETED);
        todayCompletedWrapper.ge(Task::getCompletedAt, today.atStartOfDay());
        todayCompletedWrapper.lt(Task::getCompletedAt, today.plusDays(1).atStartOfDay());
        Long todayCompletedCount = taskMapper.selectCount(todayCompletedWrapper);
        stats.put("todayCompleted", todayCompletedCount);
        
        // 本月完成数
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LambdaQueryWrapper<Task> monthCompletedWrapper = new LambdaQueryWrapper<>();
        monthCompletedWrapper.eq(Task::getAssignedTo, userId);
        monthCompletedWrapper.eq(Task::getStatus, TaskStatus.COMPLETED);
        monthCompletedWrapper.ge(Task::getCompletedAt, firstDayOfMonth.atStartOfDay());
        Long monthCompletedCount = taskMapper.selectCount(monthCompletedWrapper);
        stats.put("monthCompleted", monthCompletedCount);
        
        // 待处理任务数（PENDING状态）
        LambdaQueryWrapper<Task> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Task::getAssignedTo, userId);
        pendingWrapper.eq(Task::getStatus, TaskStatus.PENDING);
        Long pendingCount = taskMapper.selectCount(pendingWrapper);
        stats.put("pending", pendingCount);
        
        return stats;
    }

    /**
     * 填充任务关联信息
     * 包括设备名称、处理人姓名、分配人姓名
     */
    private void fillTaskInfo(Task task) {
        // 填充处理人姓名
        if (task.getAssignedTo() != null) {
            User assignee = userMapper.selectById(task.getAssignedTo());
            if (assignee != null) {
                task.setAssigneeName(assignee.getName());
            }
        }
        
        // 填充分配人姓名
        if (task.getAssignedBy() != null) {
            User assigner = userMapper.selectById(task.getAssignedBy());
            if (assigner != null) {
                task.setAssignerName(assigner.getName());
            }
        }
        
        // 填充设备名称
        if (task.getEquipmentId() != null) {
            Equipment equipment = equipmentMapper.selectById(task.getEquipmentId());
            if (equipment != null) {
                task.setEquipmentName(equipment.getName());
            }
        }
    }
}
