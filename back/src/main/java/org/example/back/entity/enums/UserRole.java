package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum UserRole {
    ADMIN("admin", "系统管理员", "/admin/index.html"),
    DISPATCHER("dispatcher", "能源调度员", "/dispatcher/index.html"),
    INSPECTOR("inspector", "设备巡检员", "/inspector/index.html"),
    MANAGER("manager", "能源经理", "/manager/index.html"),
    WORKSHOP("workshop", "车间用户", "/workshop/index.html");

    @EnumValue
    private final String value;
    private final String description;
    private final String homePage;

    UserRole(String value, String description, String homePage) {
        this.value = value;
        this.description = description;
        this.homePage = homePage;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
    public String getHomePage() { return homePage; }
}
