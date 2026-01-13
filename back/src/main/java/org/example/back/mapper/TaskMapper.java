package org.example.back.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.back.entity.Task;

/**
 * 巡检任务 Mapper 接口
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
