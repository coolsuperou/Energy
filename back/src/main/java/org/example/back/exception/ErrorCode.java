package org.example.back.exception;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    // 通用错误
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后重试"),

    // 用户相关 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "账号或密码错误"),
    USER_DISABLED(1003, "账号已被禁用，请联系管理员"),
    USERNAME_EXISTS(1004, "用户名已存在"),
    USER_NO_ROLE(1005, "用户未分配角色，请联系管理员"),

    // 申请相关 2xxx
    APPLICATION_NOT_FOUND(2001, "申请不存在"),
    APPLICATION_ALREADY_PROCESSED(2002, "申请已处理"),
    INVALID_TIME_RANGE(2003, "结束时间必须晚于开始时间"),
    POWER_EXCEED_LIMIT(2004, "申请功率不能超过设备额定功率"),
    QUOTA_INSUFFICIENT(2005, "配额不足"),

    // 设备相关 3xxx
    EQUIPMENT_NOT_FOUND(3001, "设备不存在"),
    EQUIPMENT_FAULT(3002, "设备故障，无法使用"),

    // 任务相关 4xxx
    TASK_NOT_FOUND(4001, "任务不存在"),
    TASK_ALREADY_COMPLETED(4002, "任务已完成"),
    TASK_NOT_ASSIGNED(4003, "任务未分配给当前用户");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
