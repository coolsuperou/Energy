package org.example.back.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.back.entity.Feedback;

@Mapper
public interface FeedbackMapper extends BaseMapper<Feedback> {

    @Select("<script>" +
            "SELECT f.*, u.name as user_name, h.name as handler_name " +
            "FROM feedbacks f " +
            "LEFT JOIN users u ON f.user_id = u.id " +
            "LEFT JOIN users h ON f.handled_by = h.id " +
            "WHERE f.user_id = #{userId} " +
            "<if test='status != null and status != \"\"'> AND f.status = #{status} </if>" +
            "ORDER BY f.create_time DESC" +
            "</script>")
    IPage<Feedback> selectByUserId(Page<Feedback> page, @Param("userId") Long userId, @Param("status") String status);

    @Select("<script>" +
            "SELECT f.*, u.name as user_name, h.name as handler_name " +
            "FROM feedbacks f " +
            "LEFT JOIN users u ON f.user_id = u.id " +
            "LEFT JOIN users h ON f.handled_by = h.id " +
            "<where>" +
            "<if test='status != null and status != \"\"'> AND f.status = #{status} </if>" +
            "<if test='type != null and type != \"\"'> AND f.type = #{type} </if>" +
            "</where>" +
            "ORDER BY f.create_time DESC" +
            "</script>")
    IPage<Feedback> selectAllFeedbacks(Page<Feedback> page, @Param("status") String status, @Param("type") String type);
}
