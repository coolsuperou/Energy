package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import java.math.BigDecimal;

/**
 * 时段类型枚举
 * 定义电价时段类型及对应电价
 * 峰时8-11点和18-21点 平时11-18点和21-23点 谷时23-8点
 */
public enum TimePeriodType {
    /** 峰时 用电高峰时段 电价1.20元/kWh */
    PEAK("peak", "峰时", new BigDecimal("1.20")),
    
    /** 平时 用电平常时段 电价0.80元/kWh */
    NORMAL("normal", "平时", new BigDecimal("0.80")),
    
    /** 谷时 用电低谷时段 电价0.40元/kWh */
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
