package org.example.back.mapper.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.back.entity.Quota;

/**
 * 配额 Mapper 接口
 */
@Mapper
public interface QuotaMapper extends BaseMapper<Quota> {
}
