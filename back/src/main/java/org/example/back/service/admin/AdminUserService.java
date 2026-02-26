package org.example.back.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.entity.User;
import org.example.back.entity.enums.UserRole;
import org.example.back.entity.enums.UserStatus;
import org.example.back.exception.BusinessException;
import org.example.back.exception.ErrorCode;
import org.example.back.mapper.common.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 系统管理员用户管理服务
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	系统管理员用户CRUD和启用/禁用逻辑
 * -- =============================================
 */
@Service
public class AdminUserService {

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 分页获取用户列表
     * 
     * @param page 当前页
     * @param size 每页大小
     * @param role 角色筛选（可选）
     * @param keyword 关键词搜索（可选，搜索用户名或姓名）
     * @return 分页用户列表
     */
    public IPage<User> getUserList(int page, int size, String role, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        // 角色筛选
        if (StringUtils.hasText(role)) {
            try {
                UserRole userRole = UserRole.valueOf(role.toUpperCase());
                wrapper.eq(User::getRole, userRole);
            } catch (IllegalArgumentException e) {
                // 忽略无效的角色参数
            }
        }
        
        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getName, keyword));
        }
        
        wrapper.orderByDesc(User::getCreatedAt);
        
        IPage<User> result = userMapper.selectPage(pageParam, wrapper);
        
        // 清除密码字段
        result.getRecords().forEach(user -> user.setPassword(null));
        
        return result;
    }

    /**
     * 获取用户详情
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 创建用户
     * 
     * @param user 用户信息
     * @return 创建的用户
     */
    @Transactional
    public User createUser(User user) {
        // 参数校验
        validateUserParams(user, true);
        
        // 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // 设置默认状态
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
        }
        
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        userMapper.insert(user);
        
        user.setPassword(null);
        return user;
    }

    /**
     * 更新用户信息
     * 
     * @param userId 用户ID
     * @param user 更新的用户信息
     * @return 更新后的用户
     */
    @Transactional
    public User updateUser(Long userId, User user) {
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        
        // 如果修改了用户名，检查是否重复
        if (StringUtils.hasText(user.getUsername()) && !user.getUsername().equals(existingUser.getUsername())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, user.getUsername());
            wrapper.ne(User::getId, userId);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.USERNAME_EXISTS);
            }
            existingUser.setUsername(user.getUsername());
        }
        
        // 更新其他字段
        if (StringUtils.hasText(user.getName())) {
            existingUser.setName(user.getName());
        }
        if (user.getRole() != null) {
            existingUser.setRole(user.getRole());
        }
        if (StringUtils.hasText(user.getDepartment())) {
            existingUser.setDepartment(user.getDepartment());
        }
        if (user.getPhone() != null) {
            existingUser.setPhone(user.getPhone());
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getStatus() != null) {
            existingUser.setStatus(user.getStatus());
        }
        
        // 如果提供了新密码，则更新密码
        if (StringUtils.hasText(user.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        existingUser.setUpdatedAt(LocalDateTime.now());
        
        userMapper.updateById(existingUser);
        
        existingUser.setPassword(null);
        return existingUser;
    }

    /**
     * 删除用户
     * 
     * @param userId 用户ID
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        
        // 不允许删除管理员角色（防止误删自己）
        if (user.getRole() == UserRole.ADMIN) {
            throw new BusinessException("不能删除管理员账号");
        }
        
        userMapper.deleteById(userId);
    }

    /**
     * 启用/禁用用户
     * 
     * @param userId 用户ID
     * @param status 目标状态
     * @return 更新后的用户
     */
    @Transactional
    public User updateUserStatus(Long userId, UserStatus status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        
        // 不允许禁用管理员角色
        if (user.getRole() == UserRole.ADMIN && status == UserStatus.DISABLED) {
            throw new BusinessException("不能禁用管理员账号");
        }
        
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        
        userMapper.updateById(user);
        
        user.setPassword(null);
        return user;
    }

    /**
     * 重置用户密码
     * 
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 更新后的用户
     */
    @Transactional
    public User resetPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        
        if (!StringUtils.hasText(newPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        
        userMapper.updateById(user);
        
        user.setPassword(null);
        return user;
    }

    /**
     * 校验用户参数
     */
    private void validateUserParams(User user, boolean isCreate) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new BusinessException("用户名不能为空");
        }
        if (isCreate && !StringUtils.hasText(user.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        if (!StringUtils.hasText(user.getName())) {
            throw new BusinessException("姓名不能为空");
        }
        if (user.getRole() == null) {
            throw new BusinessException("角色不能为空");
        }
    }
}
