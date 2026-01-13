package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum EquipmentStatus {
    NORMAL("normal", "正常"),
    WARNING("warning", "警告"),
    FAULT("fault", "故障");

    @EnumValue
    private final String value;
    private final String description;

    EquipmentStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
