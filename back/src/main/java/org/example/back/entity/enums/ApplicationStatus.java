package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ApplicationStatus {
    PENDING("pending", "待审批"),
    APPROVED("approved", "已批准"),
    REJECTED("rejected", "已拒绝"),
    ADJUSTED("adjusted", "已调整");

    @EnumValue
    private final String value;
    private final String description;

    ApplicationStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
