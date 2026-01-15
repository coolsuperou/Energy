package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 工单任务状态枚举
 * 定义工单任务的处理流程状态
 */
public enum TaskStatus {
    /** 待处理 任务已创建等待巡检员处理 */
    PENDING("pending", "待处理"),
    
    /** 进行中 巡检员正在执行任务 */
    IN_PROGRESS("in_progress", "进行中"),
    
    /** 已完成 巡检员已完成任务并提交报告 */
    COMPLETED("completed", "已完成");

    /** 数据库存储值 */
    @EnumValue
    private final String value;
    
    /** 状态中文描述 */
    private final String description;

    TaskStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
