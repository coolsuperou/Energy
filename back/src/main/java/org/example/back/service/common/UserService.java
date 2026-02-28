package org.example.back.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.back.dto.LoginRequest;
import org.example.back.dto.LoginResponse;
import org.example.back.dto.RegisterRequest;
import org.example.back.dto.UserDTO;
import org.example.back.entity.User;
import org.example.back.entity.enums.UserRole;
import org.example.back.entity.enums.UserStatus;
import org.example.back.exception.BusinessException;
import org.example.back.exception.ErrorCode;
import org.example.back.mapper.common.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

    @Autowired
    private MinioService minioService;

    public LoginResponse login(LoginRequest request) {
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));

        if (user == null) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 开发环境：明文密码比较
        if (!request.getPassword().equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        if (user.getRole() == null) {
            throw new BusinessException(ErrorCode.USER_NO_ROLE);
        }

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRole(),
                user.getDepartment(),
                user.getRole().getHomePage()
        );
    }

    @Transactional
    public UserDTO register(RegisterRequest request) {
        long count = this.count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        // 开发环境：明文存储密码
        user.setPassword(request.getPassword());
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setDepartment(request.getDepartment());
        user.setStatus(UserStatus.ACTIVE);
        // 设置用户选择的角色
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
        } else {
            user.setRole(null);
        }

        this.save(user);

        return UserDTO.fromEntity(user);
    }

    public UserDTO getUserById(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserDTO dto = UserDTO.fromEntity(user);
        // 转换头像URL为完整地址
        if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().isEmpty()) {
            dto.setAvatarUrl(minioService.getFileUrl(dto.getAvatarUrl()));
        }
        return dto;
    }

    public User getUserEntityById(Long userId) {
        return this.getById(userId);
    }

    public User getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    @Transactional
    public void updateUserRole(Long userId, UserRole role) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setRole(role);
        this.updateById(user);
    }

    @Transactional
    public void updateUserStatus(Long userId, UserStatus status) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(status);
        this.updateById(user);
    }

    @Transactional
    public void updateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        this.updateById(user);
    }

    /**
     * 获取用户列表
     * 支持按角色筛选，用于派单等场景
     * 
     * @param role 用户角色，为空则查询所有用户
     * @return 用户DTO列表
     */
    public List<UserDTO> getUserList(UserRole role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        // 只查询启用状态的用户
        wrapper.eq(User::getStatus, UserStatus.ACTIVE);
        
        // 如果指定了角色，则按角色筛选
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        
        // 按姓名排序
        wrapper.orderByAsc(User::getName);
        
        List<User> users = this.list(wrapper);
        return users.stream()
                .map(user -> {
                    UserDTO dto = UserDTO.fromEntity(user);
                    // 转换头像URL为完整地址
                    if (dto.getAvatarUrl() != null && !dto.getAvatarUrl().isEmpty()) {
                        dto.setAvatarUrl(minioService.getFileUrl(dto.getAvatarUrl()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }
}
