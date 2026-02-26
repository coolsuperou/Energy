package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 技能认证状态枚举
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	技能认证状态：待审核/已认证/已拒绝
 * -- =============================================
 */
public enum CertificationStatus {
    PENDING("pending", "待审核"),
    CERTIFIED("certified", "已认证"),
    REJECTED("rejected", "已拒绝");

    @EnumValue
    private final String value;
    private final String description;

    CertificationStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
