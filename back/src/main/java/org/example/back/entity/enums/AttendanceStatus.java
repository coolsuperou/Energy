package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 考勤状态枚举
 */
public enum AttendanceStatus {
    NORMAL("normal", "正常"),
    LATE("late", "迟到"),
    EARLY_LEAVE("early_leave", "早退"),
    ABSENT("absent", "缺勤"),
    REST("rest", "休息"),
    HOLIDAY("holiday", "假期");

    @EnumValue
    private final String code;

    @JsonValue
    private final String desc;

    AttendanceStatus(String code, String desc) {
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
