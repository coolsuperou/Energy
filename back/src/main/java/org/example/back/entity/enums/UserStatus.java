package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 用户状态枚举
 * 定义用户账号的启用和禁用状态
 */
public enum UserStatus {
    /** 正常状态 可以登录使用系统 */
    ACTIVE("active", "正常"),
    
    /** 禁用状态 无法登录系统 */
    DISABLED("disabled", "禁用");

    /** 数据库存储值 */
    @EnumValue
    private final String value;
    
    /** 状态中文描述 */
    private final String description;

    UserStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
