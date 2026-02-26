package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 巡检记录实体类
 * 每台设备每个检查项的检查结果明细
 * 检查类型固定5种：外观检查、运行状态、温度检测、噪音振动、电气安全
 */
@Data
@TableName("inspection_records")
public class InspectionRecord {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联巡检计划ID */
    private Long planId;

    /** 设备ID */
    private Long equipmentId;

    /** 检查类型 appearance外观 running运行 temperature温度 noise噪音 electrical电气 */
    private String checkType;

    /** 检查结果 normal正常 abnormal异常 fault故障 */
    private String result;

    /** 备注 */
    private String remark;

    /** 是否已转报修 0否 1是 */
    private Boolean repaired;

    /** 检查时间 */
    private LocalDateTime checkedAt;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // ===== 非数据库字段 =====

    /** 设备名称 */
    @TableField(exist = false)
    private String equipmentName;

    /** 设备位置 */
    @TableField(exist = false)
    private String equipmentLocation;

    /** 设备状态 */
    @TableField(exist = false)
    private String equipmentStatus;
}
