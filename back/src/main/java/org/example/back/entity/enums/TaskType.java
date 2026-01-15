package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 工单任务类型枚举
 * 定义巡检员需要执行的任务类型
 */
public enum TaskType {
    /** 巡检 日常设备巡检任务 */
    INSPECTION("inspection", "巡检"),
    
    /** 维修 设备故障维修任务 */
    REPAIR("repair", "维修"),
    
    /** 保养 设备预防性维护任务 */
    MAINTENANCE("maintenance", "保养");

    /** 数据库存储值 */
    @EnumValue
    private final String value;
    
    /** 任务类型中文描述 */
    private final String description;

    TaskType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
