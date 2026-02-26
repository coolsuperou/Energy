package org.example.back.mapper.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.back.entity.EnergyData;

/**
 * 能耗数据 Mapper 接口
 */
@Mapper
public interface EnergyDataMapper extends BaseMapper<EnergyData> {
}
