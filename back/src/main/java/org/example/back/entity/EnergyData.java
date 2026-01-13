package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import org.example.back.entity.enums.TimePeriodType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("energy_data")
public class EnergyData {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private Long workshopId;
    private Long equipmentId;
    private LocalDate recordDate;
    private Integer recordHour;
    private BigDecimal power;
    private BigDecimal energy;
    private TimePeriodType periodType;
    private BigDecimal price;
    private BigDecimal cost;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(exist = false)
    private String applicationNo;
    @TableField(exist = false)
    private String equipmentName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getWorkshopId() { return workshopId; }
    public void setWorkshopId(Long workshopId) { this.workshopId = workshopId; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public Integer getRecordHour() { return recordHour; }
    public void setRecordHour(Integer recordHour) { this.recordHour = recordHour; }
    public BigDecimal getPower() { return power; }
    public void setPower(BigDecimal power) { this.power = power; }
    public BigDecimal getEnergy() { return energy; }
    public void setEnergy(BigDecimal energy) { this.energy = energy; }
    public TimePeriodType getPeriodType() { return periodType; }
    public void setPeriodType(TimePeriodType periodType) { this.periodType = periodType; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getApplicationNo() { return applicationNo; }
    public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
}
