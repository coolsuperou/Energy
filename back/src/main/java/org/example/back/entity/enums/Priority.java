package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum Priority {
    LOW("low", "低"),
    MEDIUM("medium", "中"),
    HIGH("high", "高");

    @EnumValue
    private final String value;
    private final String description;

    Priority(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
