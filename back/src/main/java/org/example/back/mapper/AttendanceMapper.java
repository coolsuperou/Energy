package org.example.back.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.back.entity.AttendanceRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 考勤记录 Mapper 接口
 */
@Mapper
public interface AttendanceMapper extends BaseMapper<AttendanceRecord> {

    /**
     * 获取用户指定月份的考勤记录
     */
    @Select("SELECT * FROM attendance_records WHERE user_id = #{userId} " +
            "AND YEAR(attendance_date) = #{year} AND MONTH(attendance_date) = #{month} " +
            "ORDER BY attendance_date")
    List<AttendanceRecord> getMonthlyAttendance(@Param("userId") Long userId, 
                                                 @Param("year") int year, 
                                                 @Param("month") int month);

    /**
     * 获取用户指定月份的考勤统计
     */
    @Select("SELECT status, COUNT(*) as count FROM attendance_records " +
            "WHERE user_id = #{userId} AND YEAR(attendance_date) = #{year} AND MONTH(attendance_date) = #{month} " +
            "GROUP BY status")
    List<Map<String, Object>> getMonthlyStats(@Param("userId") Long userId, 
                                               @Param("year") int year, 
                                               @Param("month") int month);

    /**
     * 获取用户今日考勤记录
     */
    @Select("SELECT * FROM attendance_records WHERE user_id = #{userId} AND attendance_date = #{date}")
    AttendanceRecord getTodayAttendance(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 获取用户本周排班
     */
    @Select("SELECT * FROM attendance_records WHERE user_id = #{userId} " +
            "AND attendance_date >= #{startDate} AND attendance_date <= #{endDate} " +
            "ORDER BY attendance_date")
    List<AttendanceRecord> getWeeklySchedule(@Param("userId") Long userId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    /**
     * 批量插入或更新排班记录
     */
    @Select("SELECT COUNT(*) FROM attendance_records WHERE user_id = #{userId} AND attendance_date = #{date}")
    int countByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}
