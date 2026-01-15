package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 紧急程度枚举
 * 定义用电申请的紧急程度，影响审批优先级
 */
public enum Urgency {
    /** 普通 正常的用电需求 优先级3 */
    NORMAL("normal", "普通", 3),
    
    /** 紧急 需要尽快审批的用电需求 优先级2 */
    URGENT("urgent", "紧急", 2),
    
    /** 非常紧急 最高优先级的用电需求 优先级1 */
    CRITICAL("critical", "非常紧急", 1);

    /** 数据库存储值 */
    @EnumValue
    private final String value;
    
    /** 紧急程度中文描述 */
    private final String description;
    
    /** 优先级数值 数值越小优先级越高 */
    private final int priority;

    Urgency(String value, String description, int priority) {
        this.value = value;
        this.description = description;
        this.priority = priority;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
    public int getPriority() { return priority; }
}
