package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import java.math.BigDecimal;

/**
 * 时段类型枚举
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	电价时段类型及对应电价（根据需求7.1）
 * -- =============================================
 * 
 * 时段划分：
 * - 峰时(peak)：8-12点和18-22点 → 1.2元/kWh
 * - 平时(normal)：12-18点 → 0.8元/kWh
 * - 谷时(valley)：22-8点 → 0.4元/kWh
 */
public enum TimePeriodType {
    /** 峰时 用电高峰时段（8-12点、18-22点）电价1.20元/kWh */
    PEAK("peak", "峰时", new BigDecimal("1.20")),
    
    /** 平时 用电平常时段（12-18点）电价0.80元/kWh */
    NORMAL("normal", "平时", new BigDecimal("0.80")),
    
    /** 谷时 用电低谷时段（22-8点）电价0.40元/kWh */
    VALLEY("valley", "谷时", new BigDecimal("0.40"));

    /** 数据库存储值 */
    @EnumValue
    private final String value;
    
    /** 时段中文描述 */
    private final String description;
    
    /** 时段电价 单位元/kWh */
    private final BigDecimal price;

    TimePeriodType(String value, String description, BigDecimal price) {
        this.value = value;
        this.description = description;
        this.price = price;
    }

    public String getValue() { return value; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
}
