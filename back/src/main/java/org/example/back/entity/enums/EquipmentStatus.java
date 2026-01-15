package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 设备状态枚举
 * 定义车间设备的运行状态
 */
public enum EquipmentStatus {
    /** 正常 设备运行正常可以使用 */
    NORMAL("normal", "正常"),
    
    /** 警告 设备有轻微异常需要关注 */
    WARNING("warning", "警告"),
    
    /** 故障 设备出现故障无法正常使用 */
    FAULT("fault", "故障");

    /** 数据库存储值 */
    @EnumValue
    private final String value;
    
    /** 状态中文描述 */
    private final String description;

    EquipmentStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
