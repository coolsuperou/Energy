package org.example.back.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.entity.OperationLog;
import org.example.back.entity.User;
import org.example.back.mapper.admin.OperationLogMapper;
import org.example.back.mapper.common.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志服务
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	操作日志查询逻辑
 * -- =============================================
 */
@Service
public class OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 分页查询操作日志
     * 
     * @param page 当前页
     * @param size 每页大小
     * @param operationType 操作类型筛选
     * @param userId 用户ID筛选
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 分页结果
     */
    public IPage<OperationLog> getLogList(int page, int size, String operationType, 
            Long userId, String startDate, String endDate) {
        
        Page<OperationLog> pageParam = new Page<>(page, size);
        
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        
        // 操作类型筛选
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(OperationLog::getOperationType, operationType);
        }
        
        // 用户ID筛选
        if (userId != null) {
            wrapper.eq(OperationLog::getUserId, userId);
        }
        
        // 日期范围筛选
        if (StringUtils.hasText(startDate)) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(OperationLog::getCreatedAt, start);
        }
        if (StringUtils.hasText(endDate)) {
            LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            wrapper.le(OperationLog::getCreatedAt, end);
        }
        
        // 按时间倒序排列
        wrapper.orderByDesc(OperationLog::getCreatedAt);
        
        IPage<OperationLog> result = operationLogMapper.selectPage(pageParam, wrapper);
        
        // 填充用户名
        fillUserNames(result.getRecords());
        
        return result;
    }

    /**
     * 填充操作人姓名
     * 
     * @param logs 日志列表
     */
    private void fillUserNames(List<OperationLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        
        // 获取所有用户ID
        List<Long> userIds = logs.stream()
                .map(OperationLog::getUserId)
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询用户
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, String> userNameMap = users.stream()
                .collect(Collectors.toMap(User::getId, User::getName));
        
        // 填充用户名
        logs.forEach(log -> {
            String userName = userNameMap.get(log.getUserId());
            log.setUserName(userName != null ? userName : "未知用户");
        });
    }

    /**
     * 获取所有操作类型
     * 
     * @return 操作类型列表
     */
    public List<String> getOperationTypes() {
        return Arrays.asList(
            "login",      // 登录
            "logout",     // 登出
            "create",     // 创建
            "update",     // 更新
            "delete",     // 删除
            "approve",    // 审批
            "reject",     // 拒绝
            "assign",     // 派单
            "complete"    // 完成
        );
    }
}
