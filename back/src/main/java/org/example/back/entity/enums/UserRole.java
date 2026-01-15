package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 用户角色枚举
 * 定义系统中的五种用户角色及其首页路径
 */
public enum UserRole {
    /** 系统管理员 负责用户管理和系统配置 */
    ADMIN("admin", "系统管理员", "/admin/index.html"),
    
    /** 能源调度员 负责审批用电申请和电力调度 */
    DISPATCHER("dispatcher", "能源调度员", "/dispatcher/index.html"),
    
    /** 设备巡检员 负责设备巡检和工单处理 */
    INSPECTOR("inspector", "设备巡检员", "/inspector/index.html"),
    
    /** 能源经理 负责数据分析和决策支持 */
    MANAGER("manager", "能源经理", "/manager/index.html"),
    
    /** 车间用户 提交用电申请并查看能耗数据 */
    WORKSHOP("workshop", "车间用户", "/workshop/index.html");

    /** 数据库存储值 */
    @EnumValue
    private final String value;
    
    /** 角色中文描述 */
    private final String description;
    
    /** 角色首页路径 登录后自动跳转 */
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
