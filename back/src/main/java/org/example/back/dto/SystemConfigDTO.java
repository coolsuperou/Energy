package org.example.back.dto;

import java.math.BigDecimal;

/**
 * 系统配置DTO
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	系统配置数据传输对象，包含电价参数、预警阈值、时段配置
 * -- =============================================
 */
public class SystemConfigDTO {

    // ========== 电价参数配置 ==========
    /** 峰时电价（元/kWh） */
    private BigDecimal peakPrice;
    
    /** 平时电价（元/kWh） */
    private BigDecimal normalPrice;
    
    /** 谷时电价（元/kWh） */
    private BigDecimal valleyPrice;

    // ========== 预警阈值配置 ==========
    /** 功率超限阈值（%），超过此比例触发超限预警 */
    private Integer powerOverloadThreshold;
    
    /** 功率预警阈值（%），超过此比例触发接近限额预警 */
    private Integer powerWarningThreshold;
    
    /** 配额预警阈值（%），已用配额超过此比例触发预警 */
    private Integer quotaWarningThreshold;

    // ========== 时段配置 ==========
    /** 峰时开始时间1（小时，0-23） */
    private Integer peakStartHour1;
    
    /** 峰时结束时间1（小时，0-23） */
    private Integer peakEndHour1;
    
    /** 峰时开始时间2（小时，0-23） */
    private Integer peakStartHour2;
    
    /** 峰时结束时间2（小时，0-23） */
    private Integer peakEndHour2;
    
    /** 平时开始时间（小时，0-23） */
    private Integer normalStartHour;
    
    /** 平时结束时间（小时，0-23） */
    private Integer normalEndHour;

    // Getters and Setters
    public BigDecimal getPeakPrice() {
        return peakPrice;
    }

    public void setPeakPrice(BigDecimal peakPrice) {
        this.peakPrice = peakPrice;
    }

    public BigDecimal getNormalPrice() {
        return normalPrice;
    }

    public void setNormalPrice(BigDecimal normalPrice) {
        this.normalPrice = normalPrice;
    }

    public BigDecimal getValleyPrice() {
        return valleyPrice;
    }

    public void setValleyPrice(BigDecimal valleyPrice) {
        this.valleyPrice = valleyPrice;
    }

    public Integer getPowerOverloadThreshold() {
        return powerOverloadThreshold;
    }

    public void setPowerOverloadThreshold(Integer powerOverloadThreshold) {
        this.powerOverloadThreshold = powerOverloadThreshold;
    }

    public Integer getPowerWarningThreshold() {
        return powerWarningThreshold;
    }

    public void setPowerWarningThreshold(Integer powerWarningThreshold) {
        this.powerWarningThreshold = powerWarningThreshold;
    }

    public Integer getQuotaWarningThreshold() {
        return quotaWarningThreshold;
    }

    public void setQuotaWarningThreshold(Integer quotaWarningThreshold) {
        this.quotaWarningThreshold = quotaWarningThreshold;
    }

    public Integer getPeakStartHour1() {
        return peakStartHour1;
    }

    public void setPeakStartHour1(Integer peakStartHour1) {
        this.peakStartHour1 = peakStartHour1;
    }

    public Integer getPeakEndHour1() {
        return peakEndHour1;
    }

    public void setPeakEndHour1(Integer peakEndHour1) {
        this.peakEndHour1 = peakEndHour1;
    }

    public Integer getPeakStartHour2() {
        return peakStartHour2;
    }

    public void setPeakStartHour2(Integer peakStartHour2) {
        this.peakStartHour2 = peakStartHour2;
    }

    public Integer getPeakEndHour2() {
        return peakEndHour2;
    }

    public void setPeakEndHour2(Integer peakEndHour2) {
        this.peakEndHour2 = peakEndHour2;
    }

    public Integer getNormalStartHour() {
        return normalStartHour;
    }

    public void setNormalStartHour(Integer normalStartHour) {
        this.normalStartHour = normalStartHour;
    }

    public Integer getNormalEndHour() {
        return normalEndHour;
    }

    public void setNormalEndHour(Integer normalEndHour) {
        this.normalEndHour = normalEndHour;
    }
}
