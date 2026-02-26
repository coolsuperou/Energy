package org.example.back.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.back.entity.Comment;
import org.example.back.entity.User;
import org.example.back.mapper.common.CommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 评论服务类
 * 提供评论的发表和查询功能
 */
@Service
public class CommentService extends ServiceImpl<CommentMapper, Comment> {

    @Autowired
    private UserService userService;

    /**
     * 发表评论
     * 
     * @param relatedType 关联业务类型 application/task
     * @param relatedId 关联业务ID
     * @param userId 评论人用户ID
     * @param content 评论内容
     * @return 保存后的评论对象（包含评论人姓名）
     */
    public Comment addComment(String relatedType, Long relatedId, Long userId, String content) {
        Comment comment = new Comment();
        comment.setRelatedType(relatedType);
        comment.setRelatedId(relatedId);
        comment.setUserId(userId);
        comment.setContent(content);
        this.save(comment);
        
        // 填充评论人姓名
        User user = userService.getById(userId);
        if (user != null) {
            comment.setUserName(user.getName());
        }
        
        return comment;
    }

    /**
     * 获取评论列表
     * 按创建时间倒序排列，并填充评论人姓名
     * 
     * @param relatedType 关联业务类型 application/task
     * @param relatedId 关联业务ID
     * @return 评论列表
     */
    public List<Comment> getCommentList(String relatedType, Long relatedId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getRelatedType, relatedType)
               .eq(Comment::getRelatedId, relatedId)
               .orderByDesc(Comment::getCreatedAt);
        
        List<Comment> comments = this.list(wrapper);
        
        // 填充评论人姓名
        for (Comment comment : comments) {
            User user = userService.getById(comment.getUserId());
            if (user != null) {
                comment.setUserName(user.getName());
            }
        }
        
        return comments;
    }
}
