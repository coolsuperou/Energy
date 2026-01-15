package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.example.back.entity.enums.EquipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备实体类
 * 车间内的生产设备信息
 * 用于用电申请时选择设备，记录设备的额定功率和状态
 */
@Data
@TableName("equipments")
public class Equipment {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 设备名称 */
    private String name;
    
    /** 所属车间ID */
    private Long workshopId;
    
    /** 额定功率 单位kW */
    private BigDecimal ratedPower;
    
    /** 设备状态 NORMAL正常 MAINTENANCE维护中 FAULT故障 */
    private EquipmentStatus status;
    
    /** 设备位置 */
    private String location;
    
    /** 设备型号 */
    private String model;
    
    /** 创建时间 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
