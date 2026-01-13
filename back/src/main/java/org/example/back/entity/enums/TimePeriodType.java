package org.example.back.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import java.math.BigDecimal;

public enum TimePeriodType {
    PEAK("peak", "峰时", new BigDecimal("1.20")),
    NORMAL("normal", "平时", new BigDecimal("0.80")),
    VALLEY("valley", "谷时", new BigDecimal("0.40"));

    @EnumValue
    private final String value;
    private final String description;
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
