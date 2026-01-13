package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import org.example.back.entity.enums.EquipmentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 设备实体
 */
@TableName("equipments")
public class Equipment {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long workshopId;
    private BigDecimal ratedPower;
    private EquipmentStatus status;
    private String location;
    private String model;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getWorkshopId() { return workshopId; }
    public void setWorkshopId(Long workshopId) { this.workshopId = workshopId; }
    public BigDecimal getRatedPower() { return ratedPower; }
    public void setRatedPower(BigDecimal ratedPower) { this.ratedPower = ratedPower; }
    public EquipmentStatus getStatus() { return status; }
    public void setStatus(EquipmentStatus status) { this.status = status; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
