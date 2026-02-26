package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 工单任务状态枚举
 * 定义工单任务的处理流程状态
 * 
 * 状态流转：
 * - 创建任务 → PENDING（待进行）
 * - 调度员派单 → IN_PROGRESS（进行中，派单后直接进入此状态，无需巡检员接单）
 * - 巡检员完成 → COMPLETED（已完成）
 * 
 * @author 每天十点睡
 */
public enum TaskStatus {
    /** 待进行 任务已创建等待调度员分派 */
    PENDING("pending", "待进行"),
    
    /** 进行中 任务已派单给巡检员，正在执行 */
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
