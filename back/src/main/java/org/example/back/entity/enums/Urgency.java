package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum Urgency {
    NORMAL("normal", "普通", 3),
    URGENT("urgent", "紧急", 2),
    CRITICAL("critical", "非常紧急", 1);

    @EnumValue
    private final String value;
    private final String description;
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
