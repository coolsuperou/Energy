package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 班次类型枚举
 */
public enum ShiftType {
    DAY("day", "白班"),
    NIGHT("night", "夜班"),
    REST("rest", "休息");

    @EnumValue
    private final String code;

    @JsonValue
    private final String desc;

    ShiftType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
