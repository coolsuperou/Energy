package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 用电申请状态枚举
 * 定义用电申请的审批流程状态
 */
public enum ApplicationStatus {
    /** 待审批 申请已提交等待调度员审批 */
    PENDING("pending", "待审批"),
    
    /** 已批准 调度员批准申请可以开始用电 */
    APPROVED("approved", "已批准"),
    
    /** 已拒绝 调度员拒绝申请 */
    REJECTED("rejected", "已拒绝"),
    
    /** 已调整 调度员调整时段后批准 */
    ADJUSTED("adjusted", "已调整");

    /** 数据库存储值 */
    @EnumValue
    private final String value;
    
    /** 状态中文描述 */
    private final String description;

    ApplicationStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
}
