package org.example.back.mapper.admin;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.back.entity.OperationLog;

/**
 * 操作日志 Mapper 接口
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
