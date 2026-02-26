package org.example.back.mapper.inspector;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.back.entity.InspectionRecord;

import java.util.List;

/**
 * 巡检记录 Mapper 接口
 * 巡检员执行设备检查使用
 */
@Mapper
public interface InspectionRecordMapper extends BaseMapper<InspectionRecord> {

    /**
     * 查询某计划下所有记录，关联设备信息
     */
    @Select("SELECT r.*, e.name AS equipment_name, e.location AS equipment_location, " +
            "e.status AS equipment_status " +
            "FROM inspection_records r " +
            "LEFT JOIN equipments e ON r.equipment_id = e.id " +
            "WHERE r.plan_id = #{planId} " +
            "ORDER BY e.id, r.check_type")
    List<InspectionRecord> selectRecordsWithEquipment(@Param("planId") Long planId);

    /**
     * 统计某计划下已完成检查的设备数（5项全部有结果的设备）
     */
    @Select("SELECT COUNT(DISTINCT equipment_id) FROM inspection_records " +
            "WHERE plan_id = #{planId} AND result IS NOT NULL " +
            "GROUP BY equipment_id HAVING COUNT(*) = 5")
    List<Long> countFullyCheckedEquipments(@Param("planId") Long planId);

    /**
     * 统计某计划下的总设备数
     */
    @Select("SELECT COUNT(DISTINCT equipment_id) FROM inspection_records WHERE plan_id = #{planId}")
    Integer countTotalEquipments(@Param("planId") Long planId);
}
