package org.example.back.service.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.entity.SkillCertification;
import org.example.back.entity.User;
import org.example.back.entity.enums.CertificationStatus;
import org.example.back.entity.enums.UserRole;
import org.example.back.entity.enums.UserStatus;
import org.example.back.exception.BusinessException;
import org.example.back.exception.ErrorCode;
import org.example.back.mapper.common.UserMapper;
import org.example.back.mapper.inspector.SkillCertificationMapper;
import org.example.back.service.common.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 经理用户管理服务
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	经理用户管理和技能认证审核逻辑
 * -- =============================================
 */
@Service
public class ManagerUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SkillCertificationMapper skillCertificationMapper;

    @Autowired
    private MinioService minioService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public IPage<User> getUserList(int page, int size, String role, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(role)) {
            try {
                UserRole userRole = UserRole.valueOf(role.toUpperCase());
                wrapper.eq(User::getRole, userRole);
            } catch (IllegalArgumentException e) {
            }
        }
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getName, keyword));
        }
        
        wrapper.orderByDesc(User::getCreatedAt);
        IPage<User> result = userMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(user -> user.setPassword(null));
        return result;
    }

    public User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        user.setPassword(null);
        return user;
    }

    @Transactional
    public User createUser(User user) {
        validateUserParams(user, true);
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
        }
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    @Transactional
    public User updateUser(Long userId, User user) {
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        
        if (StringUtils.hasText(user.getUsername()) && !user.getUsername().equals(existingUser.getUsername())) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, user.getUsername());
            wrapper.ne(User::getId, userId);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BusinessException(ErrorCode.USERNAME_EXISTS);
            }
            existingUser.setUsername(user.getUsername());
        }
        
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
        if (StringUtils.hasText(user.getPassword())) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        existingUser.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(existingUser);
        existingUser.setPassword(null);
        return existingUser;
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (user.getRole() == UserRole.MANAGER) {
            throw new BusinessException("不能删除经理账号");
        }
        userMapper.deleteById(userId);
    }

    @Transactional
    public User updateUserStatus(Long userId, UserStatus status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (user.getRole() == UserRole.MANAGER && status == UserStatus.DISABLED) {
            throw new BusinessException("不能禁用经理账号");
        }
        user.setStatus(status);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    public IPage<Map<String, Object>> getPendingCertifications(int page, int size) {
        Page<SkillCertification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SkillCertification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillCertification::getStatus, CertificationStatus.PENDING);
        wrapper.orderByAsc(SkillCertification::getCreatedAt);
        
        IPage<SkillCertification> certPage = skillCertificationMapper.selectPage(pageParam, wrapper);
        
        Page<Map<String, Object>> resultPage = new Page<>(page, size);
        resultPage.setTotal(certPage.getTotal());
        resultPage.setPages(certPage.getPages());
        
        List<Map<String, Object>> records = certPage.getRecords().stream().map(cert -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", cert.getId());
            item.put("userId", cert.getUserId());
            item.put("skillName", cert.getSkillName());
            item.put("certificateUrl", cert.getCertificateUrl() != null ? minioService.getFileUrl(cert.getCertificateUrl()) : null);
            item.put("status", cert.getStatus());
            item.put("createdAt", cert.getCreatedAt());
            
            User user = userMapper.selectById(cert.getUserId());
            if (user != null) {
                item.put("userName", user.getName());
                item.put("userDepartment", user.getDepartment());
            }
            return item;
        }).toList();
        
        resultPage.setRecords(records);
        return resultPage;
    }

    public IPage<Map<String, Object>> getCertificationList(int page, int size, String status) {
        Page<SkillCertification> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SkillCertification> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(status)) {
            try {
                CertificationStatus certStatus = CertificationStatus.valueOf(status.toUpperCase());
                wrapper.eq(SkillCertification::getStatus, certStatus);
            } catch (IllegalArgumentException e) {
            }
        }
        
        wrapper.orderByDesc(SkillCertification::getCreatedAt);
        IPage<SkillCertification> certPage = skillCertificationMapper.selectPage(pageParam, wrapper);
        
        Page<Map<String, Object>> resultPage = new Page<>(page, size);
        resultPage.setTotal(certPage.getTotal());
        resultPage.setPages(certPage.getPages());
        
        List<Map<String, Object>> records = certPage.getRecords().stream().map(cert -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", cert.getId());
            item.put("userId", cert.getUserId());
            item.put("skillName", cert.getSkillName());
            item.put("certificateUrl", cert.getCertificateUrl() != null ? minioService.getFileUrl(cert.getCertificateUrl()) : null);
            item.put("status", cert.getStatus());
            item.put("rejectReason", cert.getRejectReason());
            item.put("reviewedBy", cert.getReviewedBy());
            item.put("reviewedAt", cert.getReviewedAt());
            item.put("createdAt", cert.getCreatedAt());
            
            User user = userMapper.selectById(cert.getUserId());
            if (user != null) {
                item.put("userName", user.getName());
                item.put("userDepartment", user.getDepartment());
            }
            
            if (cert.getReviewedBy() != null) {
                User reviewer = userMapper.selectById(cert.getReviewedBy());
                if (reviewer != null) {
                    item.put("reviewerName", reviewer.getName());
                }
            }
            return item;
        }).toList();
        
        resultPage.setRecords(records);
        return resultPage;
    }

    @Transactional
    public SkillCertification approveCertification(Long certId, Long reviewerId) {
        SkillCertification cert = skillCertificationMapper.selectById(certId);
        if (cert == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (cert.getStatus() != CertificationStatus.PENDING) {
            throw new BusinessException("该申请已被处理，无法重复审核");
        }
        
        cert.setStatus(CertificationStatus.CERTIFIED);
        cert.setReviewedBy(reviewerId);
        cert.setReviewedAt(LocalDateTime.now());
        cert.setUpdatedAt(LocalDateTime.now());
        skillCertificationMapper.updateById(cert);
        return cert;
    }

    @Transactional
    public SkillCertification rejectCertification(Long certId, Long reviewerId, String rejectReason) {
        SkillCertification cert = skillCertificationMapper.selectById(certId);
        if (cert == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (cert.getStatus() != CertificationStatus.PENDING) {
            throw new BusinessException("该申请已被处理，无法重复审核");
        }
        if (!StringUtils.hasText(rejectReason)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        
        cert.setStatus(CertificationStatus.REJECTED);
        cert.setRejectReason(rejectReason.trim());
        cert.setReviewedBy(reviewerId);
        cert.setReviewedAt(LocalDateTime.now());
        cert.setUpdatedAt(LocalDateTime.now());
        skillCertificationMapper.updateById(cert);
        return cert;
    }

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
