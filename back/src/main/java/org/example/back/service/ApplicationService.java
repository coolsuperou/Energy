package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.entity.Application;
import org.example.back.entity.Equipment;
import org.example.back.entity.User;
import org.example.back.entity.enums.ApplicationStatus;
import org.example.back.mapper.ApplicationMapper;
import org.example.back.mapper.EquipmentMapper;
import org.example.back.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EquipmentMapper equipmentMapper;
    
    private static final AtomicInteger sequence = new AtomicInteger(0);

    public String generateApplicationNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int seq = sequence.incrementAndGet() % 10000;
        return String.format("AP%s%04d", dateStr, seq);
    }

    @Transactional
    public Application submitApplication(Application application) {
        application.setApplicationNo(generateApplicationNo());
        application.setStatus(ApplicationStatus.PENDING);
        application.setCreatedAt(LocalDateTime.now());
        applicationMapper.insert(application);
        return application;
    }

    public Application getById(Long id) {
        Application app = applicationMapper.selectById(id);
        if (app != null) {
            fillRelatedInfo(app);
        }
        return app;
    }

    public IPage<Application> getUserApplications(Long userId, Integer page, Integer size, String status) {
        Page<Application> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Application::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Application::getStatus, ApplicationStatus.valueOf(status.toUpperCase()));
        }
        wrapper.orderByDesc(Application::getCreatedAt);
        IPage<Application> result = applicationMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(this::fillRelatedInfo);
        return result;
    }

    public IPage<Application> getPendingApplications(Integer page, Integer size, Long workshopId) {
        Page<Application> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Application::getStatus, ApplicationStatus.PENDING);
        if (workshopId != null) {
            wrapper.eq(Application::getWorkshopId, workshopId);
        }
        wrapper.orderByAsc(Application::getUrgency).orderByAsc(Application::getCreatedAt);
        IPage<Application> result = applicationMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(this::fillRelatedInfo);
        return result;
    }

    public IPage<Application> getAllApplications(Integer page, Integer size, String status, Long workshopId) {
        Page<Application> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Application::getStatus, ApplicationStatus.valueOf(status.toUpperCase()));
        }
        if (workshopId != null) {
            wrapper.eq(Application::getWorkshopId, workshopId);
        }
        wrapper.orderByDesc(Application::getCreatedAt);
        IPage<Application> result = applicationMapper.selectPage(pageParam, wrapper);
        result.getRecords().forEach(this::fillRelatedInfo);
        return result;
    }

    @Transactional
    public Application approve(Long id, Long approverId, String comment) {
        Application app = applicationMapper.selectById(id);
        if (app == null) throw new RuntimeException("申请不存在");
        if (app.getStatus() != ApplicationStatus.PENDING) throw new RuntimeException("只能审批待处理的申请");
        app.setStatus(ApplicationStatus.APPROVED);
        app.setApprovedBy(approverId);
        app.setApprovedAt(LocalDateTime.now());
        app.setComment(comment);
        applicationMapper.updateById(app);
        return app;
    }

    @Transactional
    public Application reject(Long id, Long approverId, String comment) {
        Application app = applicationMapper.selectById(id);
        if (app == null) throw new RuntimeException("申请不存在");
        if (app.getStatus() != ApplicationStatus.PENDING) throw new RuntimeException("只能审批待处理的申请");
        app.setStatus(ApplicationStatus.REJECTED);
        app.setApprovedBy(approverId);
        app.setApprovedAt(LocalDateTime.now());
        app.setComment(comment);
        applicationMapper.updateById(app);
        return app;
    }

    @Transactional
    public Application adjust(Long id, Long approverId, String comment, LocalTime adjustedStartTime, LocalTime adjustedEndTime) {
        Application app = applicationMapper.selectById(id);
        if (app == null) throw new RuntimeException("申请不存在");
        if (app.getStatus() != ApplicationStatus.PENDING) throw new RuntimeException("只能审批待处理的申请");
        app.setStatus(ApplicationStatus.ADJUSTED);
        app.setApprovedBy(approverId);
        app.setApprovedAt(LocalDateTime.now());
        app.setComment(comment);
        app.setAdjustedStartTime(adjustedStartTime);
        app.setAdjustedEndTime(adjustedEndTime);
        applicationMapper.updateById(app);
        return app;
    }

    private void fillRelatedInfo(Application app) {
        if (app.getUserId() != null) {
            User user = userMapper.selectById(app.getUserId());
            if (user != null) app.setUserName(user.getName());
        }
        if (app.getEquipmentId() != null) {
            Equipment equipment = equipmentMapper.selectById(app.getEquipmentId());
            if (equipment != null) app.setEquipmentName(equipment.getName());
        }
        if (app.getApprovedBy() != null) {
            User approver = userMapper.selectById(app.getApprovedBy());
            if (approver != null) app.setApproverName(approver.getName());
        }
    }
}
