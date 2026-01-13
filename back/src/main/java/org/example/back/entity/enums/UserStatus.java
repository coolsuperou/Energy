package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum UserStatus {
    ACTIVE("active", "正常"),
    DISABLED("disabled", "禁用");

    @EnumValue
    private final String value;
    private final String description;

    UserStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
