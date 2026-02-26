package org.example.back.mapper.dispatcher;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.back.entity.InspectionPlan;

import java.time.LocalDate;
import java.util.List;

/**
 * 巡检计划 Mapper 接口
 * 调度员管理巡检排班使用
 */
@Mapper
public interface InspectionPlanMapper extends BaseMapper<InspectionPlan> {

    /**
     * 查询某周的排班计划，关联巡检员姓名和车间名称
     */
    @Select("SELECT p.*, u.name AS inspector_name, " +
            "CONCAT('第', p.workshop_id, '车间') AS workshop_name " +
            "FROM inspection_plans p " +
            "LEFT JOIN users u ON p.inspector_id = u.id " +
            "WHERE p.week_start = #{weekStart} " +
            "ORDER BY p.inspector_id, p.workshop_id")
    List<InspectionPlan> selectPlansWithNames(@Param("weekStart") LocalDate weekStart);

    /**
     * 查询某巡检员某周的计划
     */
    @Select("SELECT p.*, CONCAT('第', p.workshop_id, '车间') AS workshop_name " +
            "FROM inspection_plans p " +
            "WHERE p.inspector_id = #{inspectorId} AND p.week_start = #{weekStart} " +
            "ORDER BY p.workshop_id")
    List<InspectionPlan> selectPlansByInspector(@Param("inspectorId") Long inspectorId,
                                                @Param("weekStart") LocalDate weekStart);
}
