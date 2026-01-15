package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 任务优先级枚举
 * 定义工单任务的优先级别
 */
public enum Priority {
    /** 低优先级 普通任务 */
    LOW("low", "低"),
    
    /** 中优先级 一般重要任务 */
    MEDIUM("medium", "中"),
    
    /** 高优先级 重要紧急任务 */
    HIGH("high", "高");

    /** 数据库存储值 */
    @EnumValue
    private final String value;
    
    /** 优先级中文描述 */
    private final String description;

    Priority(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
