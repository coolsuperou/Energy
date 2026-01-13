package org.example.back.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.back.entity.Application;

/**
 * 用电申请 Mapper 接口
 */
@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {
}
