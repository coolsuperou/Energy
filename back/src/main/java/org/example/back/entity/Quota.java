package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@TableName("quotas")
public class Quota {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workshopId;
    @TableField("`year_month`")
    private String yearMonth;
    private BigDecimal totalQuota;
    private BigDecimal usedQuota;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkshopId() { return workshopId; }
    public void setWorkshopId(Long workshopId) { this.workshopId = workshopId; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
    public BigDecimal getTotalQuota() { return totalQuota; }
    public void setTotalQuota(BigDecimal totalQuota) { this.totalQuota = totalQuota; }
    public BigDecimal getUsedQuota() { return usedQuota; }
    public void setUsedQuota(BigDecimal usedQuota) { this.usedQuota = usedQuota; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public BigDecimal getRemainingQuota() {
        return totalQuota.subtract(usedQuota);
    }

    public BigDecimal getUsagePercentage() {
        if (totalQuota.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return usedQuota.multiply(new BigDecimal("100")).divide(totalQuota, 2, RoundingMode.HALF_UP);
    }
}
