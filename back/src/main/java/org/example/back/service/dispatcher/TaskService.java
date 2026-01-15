package org.example.back.service.dispatcher;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.entity.Task;
import org.example.back.entity.User;
import org.example.back.entity.enums.TaskStatus;
import org.example.back.entity.enums.TaskType;
import org.example.back.mapper.dispatcher.TaskMapper;
import org.example.back.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单服务类
 * 提供工单的创建、查询、派单、完成等功能
 */
@Service
public class TaskService {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 分页查询工单列表
     * 支持按状态、类型筛选
     */
    public IPage<Task> getTaskList(int page, int size, TaskStatus status, TaskType type) {
        Page<Task> pageParam = new Page<>(page, size);
        QueryWrapper<Task> wrapper = new QueryWrapper<>();
        
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (type != null) {
            wrapper.eq("task_type", type);
        }
        
        wrapper.orderByDesc("created_at");
        
        IPage<Task> result = taskMapper.selectPage(pageParam, wrapper);
        
        // 填充关联信息
        result.getRecords().forEach(this::fillTaskInfo);
        
        return result;
    }

    /**
     * 获取工单详情
     */
    public Task getTaskById(Long id) {
        Task task = taskMapper.selectById(id);
        if (task != null) {
            fillTaskInfo(task);
        }
        return task;
    }

    /**
     * 创建工单
     */
    public Task createTask(Task task, Long creatorId) {
        task.setAssignedBy(creatorId);
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        
        taskMapper.insert(task);
        
        fillTaskInfo(task);
        return task;
    }

    /**
     * 派单
     * 将工单分配给指定的巡检员
     */
    public void assignTask(Long taskId, Long assigneeId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("工单不存在");
        }
        
        task.setAssignedTo(assigneeId);
        task.setStatus(TaskStatus.IN_PROGRESS);
        
        taskMapper.updateById(task);
    }

    /**
     * 完成工单
     * 巡检员提交工单报告
     */
    public void completeTask(Long taskId, String report, Long userId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("工单不存在");
        }
        
        if (!userId.equals(task.getAssignedTo())) {
            throw new RuntimeException("只能完成分配给自己的工单");
        }
        
        task.setStatus(TaskStatus.COMPLETED);
        task.setReport(report);
        task.setCompletedAt(LocalDateTime.now());
        
        taskMapper.updateById(task);
    }

    /**
     * 获取我的工单列表
     * 巡检员查看分配给自己的工单
     */
    public List<Task> getMyTasks(Long userId, TaskStatus status) {
        QueryWrapper<Task> wrapper = new QueryWrapper<>();
        wrapper.eq("assigned_to", userId);
        
        if (status != null) {
            wrapper.eq("status", status);
        }
        
        wrapper.orderByDesc("created_at");
        
        List<Task> tasks = taskMapper.selectList(wrapper);
        tasks.forEach(this::fillTaskInfo);
        
        return tasks;
    }

    /**
     * 获取今日待完成任务
     */
    public List<Task> getTodayPendingTasks(Long userId) {
        QueryWrapper<Task> wrapper = new QueryWrapper<>();
        wrapper.eq("assigned_to", userId);
        wrapper.in("status", TaskStatus.PENDING, TaskStatus.IN_PROGRESS);
        wrapper.le("due_date", LocalDate.now());
        wrapper.orderByAsc("due_date");
        
        List<Task> tasks = taskMapper.selectList(wrapper);
        tasks.forEach(this::fillTaskInfo);
        
        return tasks;
    }

    /**
     * 填充工单关联信息
     * 包括设备名称、处理人姓名、创建人姓名
     */
    private void fillTaskInfo(Task task) {
        // 填充处理人姓名
        if (task.getAssignedTo() != null) {
            User assignee = userMapper.selectById(task.getAssignedTo());
            if (assignee != null) {
                task.setAssigneeName(assignee.getName());
            }
        }
        
        // 填充创建人姓名
        if (task.getAssignedBy() != null) {
            User assigner = userMapper.selectById(task.getAssignedBy());
            if (assigner != null) {
                task.setAssignerName(assigner.getName());
            }
        }
        
        // 设备名称可以通过 equipmentId 查询，这里简化处理
        // 实际项目中可以添加 EquipmentMapper 查询
    }
}
