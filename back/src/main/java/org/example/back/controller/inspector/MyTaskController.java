package org.example.back.controller.inspector;

import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.Task;
import org.example.back.entity.User;
import org.example.back.entity.enums.TaskStatus;
import org.example.back.service.inspector.InspectorTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.example.back.service.common.MinioService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 巡检员我的任务控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	设备巡检员我的任务相关接口
 * -- =============================================
 */
@RestController
@RequestMapping("/inspector/tasks")
public class MyTaskController {

    @Autowired
    private InspectorTaskService inspectorTaskService;

    @Autowired
    private MinioService minioService;

    /**
     * 获取我的任务列表
     * 支持按状态筛选（进行中/已完成）
     * 
     * @param status 任务状态（可选）：IN_PROGRESS进行中、COMPLETED已完成
     * @param session HTTP会话
     * @return 任务列表
     */
    @GetMapping("/my")
    public Result<List<Task>> getMyTasks(
            @RequestParam(required = false) TaskStatus status,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        List<Task> tasks = inspectorTaskService.getMyTasks(user.getId(), status);
        return Result.success(tasks);
    }

    /**
     * 获取今日待完成任务
     * 查询进行中且截止日期在今天或之前的任务
     * 
     * @param session HTTP会话
     * @return 今日待完成任务列表
     */
    @GetMapping("/today")
    public Result<List<Task>> getTodayTasks(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        List<Task> tasks = inspectorTaskService.getTodayTasks(user.getId());
        return Result.success(tasks);
    }

    /**
     * 完成工单
     * 巡检员填写巡检报告完成任务，可选上传图片
     * 
     * @param id 任务ID
     * @param report 巡检报告内容
     * @param images 完成图片（可选，最多5张）
     * @param session HTTP会话
     * @return 操作结果
     */
    @PostMapping("/{id}/complete")
    public Result<Void> completeTask(
            @PathVariable Long id,
            @RequestParam String report,
            @RequestParam(required = false) List<MultipartFile> images,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        // 上传图片到MinIO
        String imageUrls = null;
        if (images != null && !images.isEmpty()) {
            List<String> urls = new ArrayList<>();
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String path = minioService.uploadFile(image, user.getId(), MinioService.FileType.REPORT);
                    String fullUrl = minioService.getFileUrl(path);
                    urls.add(fullUrl);
                }
            }
            if (!urls.isEmpty()) {
                imageUrls = String.join(",", urls);
            }
        }

        inspectorTaskService.completeTask(id, report, imageUrls, user.getId());
        return Result.success(null);
    }

    /**
     * 获取我的任务统计
     * 返回进行中任务数、今日完成数等统计数据
     * 
     * @param session HTTP会话
     * @return 任务统计数据
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getMyTaskStats(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        Map<String, Object> stats = inspectorTaskService.getMyTaskStats(user.getId());
        return Result.success(stats);
    }
}
