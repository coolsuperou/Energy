package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum TaskType {
    INSPECTION("inspection", "巡检"),
    REPAIR("repair", "维修"),
    MAINTENANCE("maintenance", "保养");

    @EnumValue
    private final String value;
    private final String description;

    TaskType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
