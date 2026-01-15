package org.example.back.controller.dispatcher;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.Task;
import org.example.back.entity.User;
import org.example.back.entity.enums.TaskStatus;
import org.example.back.entity.enums.TaskType;
import org.example.back.service.dispatcher.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工单控制器
 * 提供工单管理相关接口
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /**
     * 分页查询工单列表
     * 调度员查看所有工单
     */
    @GetMapping
    public Result<IPage<Task>> getTaskList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskType type,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        IPage<Task> result = taskService.getTaskList(page, size, status, type);
        return Result.success(result);
    }

    /**
     * 获取工单详情
     */
    @GetMapping("/{id}")
    public Result<Task> getTaskById(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        Task task = taskService.getTaskById(id);
        if (task == null) {
            return Result.error("工单不存在");
        }

        return Result.success(task);
    }

    /**
     * 创建工单
     * 调度员创建新工单
     */
    @PostMapping
    public Result<Task> createTask(@RequestBody Task task, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        Task created = taskService.createTask(task, user.getId());
        return Result.success(created);
    }

    /**
     * 派单
     * 将工单分配给指定的巡检员
     */
    @PostMapping("/{id}/assign")
    public Result<Void> assignTask(
            @PathVariable Long id,
            @RequestParam Long assigneeId,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        taskService.assignTask(id, assigneeId);
        return Result.success(null);
    }

    /**
     * 完成工单
     * 巡检员提交工单报告
     */
    @PostMapping("/{id}/complete")
    public Result<Void> completeTask(
            @PathVariable Long id,
            @RequestParam String report,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        taskService.completeTask(id, report, user.getId());
        return Result.success(null);
    }

    /**
     * 获取我的工单列表
     * 巡检员查看分配给自己的工单
     */
    @GetMapping("/my")
    public Result<List<Task>> getMyTasks(
            @RequestParam(required = false) TaskStatus status,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        List<Task> tasks = taskService.getMyTasks(user.getId(), status);
        return Result.success(tasks);
    }

    /**
     * 获取今日待完成任务
     * 巡检员工作台使用
     */
    @GetMapping("/today")
    public Result<List<Task>> getTodayPendingTasks(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        List<Task> tasks = taskService.getTodayPendingTasks(user.getId());
        return Result.success(tasks);
    }
}
