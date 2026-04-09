package org.example.back.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.back.entity.enums.EquipmentStatus;

import java.math.BigDecimal;

/**
 * 创建设备 / 更新设备请求体
 */
@Data
public class EquipmentRequest {

    @NotBlank(message = "设备名称不能为空")
    private String name;

    @NotNull(message = "所属车间不能为空")
    private Long workshopId;

    @NotNull(message = "额定功率不能为空")
    private BigDecimal ratedPower;

    /** 不传则默认正常 */
    private EquipmentStatus status;

    private String location;

    private String model;
}
