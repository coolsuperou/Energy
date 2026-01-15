package org.example.back.mapper.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.back.entity.Equipment;

/**
 * 设备 Mapper 接口
 */
@Mapper
public interface EquipmentMapper extends BaseMapper<Equipment> {
}
