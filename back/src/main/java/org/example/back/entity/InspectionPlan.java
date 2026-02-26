package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 巡检计划实体类
 * 按周为巡检员分配车间的排班记录
 * 一条记录表示某巡检员在某周负责某车间的巡检任务
 */
@Data
@TableName("inspection_plans")
public class InspectionPlan {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 巡检员用户ID */
    private Long inspectorId;

    /** 车间ID */
    private Long workshopId;

    /** 周起始日期（周一） */
    private LocalDate weekStart;

    /** 计划状态 pending待完成 completed已完成 */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ===== 非数据库字段 =====

    /** 巡检员姓名 */
    @TableField(exist = false)
    private String inspectorName;

    /** 车间名称（如"第一车间"） */
    @TableField(exist = false)
    private String workshopName;

    /** 已检查设备数 */
    @TableField(exist = false)
    private Integer checkedCount;

    /** 总设备数 */
    @TableField(exist = false)
    private Integer totalCount;
}
