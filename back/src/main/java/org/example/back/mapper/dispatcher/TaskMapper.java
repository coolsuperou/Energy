package org.example.back.mapper.dispatcher;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.back.entity.Task;

/**
 * 工单服务类
 * 提供工单的创建、查询、派单、完成等功能
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
