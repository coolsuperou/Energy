package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 通知类型枚举
 * 定义系统消息通知的类型分类
 */
public enum NotificationType {
    /** 审批通知 用电申请审批结果通知 */
    APPROVAL("approval", "审批通知"),
    
    /** 任务通知 工单任务分配和状态变更通知 */
    TASK("task", "任务通知"),
    
    /** 预警通知 能耗异常和设备故障预警通知 */
    ALERT("alert", "预警通知"),
    
    /** 系统通知 系统公告和重要提示通知 */
    SYSTEM("system", "系统通知");

    /** 数据库存储值 */
    @EnumValue
    private final String value;
    
    /** 通知类型中文描述 */
    private final String description;

    NotificationType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
