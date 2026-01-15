package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
/**
 * 车间配额实体类
 * 管理每个车间每月的用电配额
 * 记录总配额、已用配额，并提供剩余配额和使用率的计算方法
 */
@Data
@TableName("quotas")
public class Quota {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 车间ID */
    private Long workshopId;
    
    /** 年月 格式YYYY-MM */
    @TableField("`year_month`")
    private String yearMonth;
    
    /** 总配额 单位kWh */
    private BigDecimal totalQuota;
    
    /** 已用配额 单位kWh */
    private BigDecimal usedQuota;
    
    /** 创建时间 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /** 更新时间 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 计算剩余配额
     * @return 剩余配额 单位kWh
     */
    public BigDecimal getRemainingQuota() {
        return totalQuota.subtract(usedQuota);
    }

    /**
     * 计算配额使用率
     * @return 使用率百分比 保留2位小数
     */
    public BigDecimal getUsagePercentage() {
        if (totalQuota.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return usedQuota.multiply(new BigDecimal("100")).divide(totalQuota, 2, RoundingMode.HALF_UP);
    }
}
