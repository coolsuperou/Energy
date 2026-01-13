package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum NotificationType {
    APPROVAL("approval", "审批通知"),
    TASK("task", "任务通知"),
    ALERT("alert", "预警通知"),
    SYSTEM("system", "系统通知");

    @EnumValue
    private final String value;
    private final String description;

    NotificationType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
