package org.example.back.controller.common;

import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.Comment;
import org.example.back.entity.User;
import org.example.back.service.common.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评论控制器
 * 提供评论的发表和查询接口
 * 支持在用电申请（application）和巡检任务（task）上发表评论
 */
@RestController
@RequestMapping("/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 发表评论
     * 
     * @param requestBody 请求体，包含 relatedType、relatedId、content
     * @param session HTTP会话
     * @return 保存后的评论对象
     */
    @PostMapping
    public Result<Comment> addComment(@RequestBody Map<String, Object> requestBody, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        String relatedType = (String) requestBody.get("relatedType");
        Object relatedIdObj = requestBody.get("relatedId");
        String content = (String) requestBody.get("content");

        // 参数校验
        if (relatedType == null || relatedType.isEmpty()) {
            return Result.error("关联类型不能为空");
        }
        if (!"application".equals(relatedType) && !"task".equals(relatedType)) {
            return Result.error("关联类型无效，只支持 application 或 task");
        }
        if (relatedIdObj == null) {
            return Result.error("关联ID不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            return Result.error("评论内容不能为空");
        }

        Long relatedId;
        if (relatedIdObj instanceof Number) {
            relatedId = ((Number) relatedIdObj).longValue();
        } else {
            return Result.error("关联ID格式错误");
        }

        Comment comment = commentService.addComment(relatedType, relatedId, user.getId(), content.trim());
        return Result.success("评论发表成功", comment);
    }

    /**
     * 获取评论列表
     * 
     * @param relatedType 关联业务类型 application/task
     * @param relatedId 关联业务ID
     * @param session HTTP会话
     * @return 评论列表
     */
    @GetMapping
    public Result<List<Comment>> getCommentList(
            @RequestParam String relatedType,
            @RequestParam Long relatedId,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        // 参数校验
        if (relatedType == null || relatedType.isEmpty()) {
            return Result.error("关联类型不能为空");
        }
        if (!"application".equals(relatedType) && !"task".equals(relatedType)) {
            return Result.error("关联类型无效，只支持 application 或 task");
        }
        if (relatedId == null) {
            return Result.error("关联ID不能为空");
        }

        List<Comment> comments = commentService.getCommentList(relatedType, relatedId);
        return Result.success(comments);
    }
}
