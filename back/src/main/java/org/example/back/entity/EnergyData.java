package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.example.back.entity.enums.TimePeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 能耗数据实体类
 * 根据已批准的用电申请自动生成的电量数据
 * 记录每小时的功率、电量、电价和费用，用于能耗分析和统计
 */
@Data
@TableName("energy_data")
public class EnergyData {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 关联的用电申请ID */
    private Long applicationId;
    
    /** 车间ID */
    private Long workshopId;
    
    /** 设备ID */
    private Long equipmentId;
    
    /** 记录日期 */
    private LocalDate recordDate;
    
    /** 记录小时 0-23 */
    private Integer recordHour;
    
    /** 功率 单位kW */
    private BigDecimal power;
    
    /** 电量 单位kWh 功率乘以时长 */
    private BigDecimal energy;
    
    /** 时段类型 PEAK峰时 NORMAL平时 VALLEY谷时 */
    private TimePeriodType periodType;
    
    /** 电价 单位元/kWh 根据时段确定 */
    private BigDecimal price;
    
    /** 费用 单位元 电量乘以电价 */
    private BigDecimal cost;
    
    /** 创建时间 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /** 申请编号 非数据库字段 用于前端显示 */
    @TableField(exist = false)
    private String applicationNo;
    
    /** 设备名称 非数据库字段 用于前端显示 */
    @TableField(exist = false)
    private String equipmentName;
}
