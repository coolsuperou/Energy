package org.example.back.service.workshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.back.entity.Application;
import org.example.back.entity.Equipment;
import org.example.back.entity.User;
import org.example.back.entity.enums.ApplicationStatus;
import org.example.back.mapper.workshop.ApplicationMapper;
import org.example.back.mapper.common.EquipmentMapper;
import org.example.back.mapper.common.UserMapper;
import org.example.back.service.energyData.EnergyDataGenerator;
import org.example.back.service.common.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用电申请服务
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	用电申请提交、审批、能耗数据生成
 * -- =============================================
 */
@Service
public class ApplicationService {

    @Autowired
    private ApplicationMapper applicationMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private EquipmentMapper equipmentMapper;
    @Autowired
    private EnergyDataGenerator energyDataGenerator;
    @Autowired(required = false)
    private NotificationService notificationService;
    
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

    /**
     * 批准用电申请
     * 审批通过后自动生成能耗数据并通知申请人
     * 
     * @param id 申请ID
     * @param approverId 审批人ID
     * @param comment 审批意见
     * @return 更新后的申请
     */
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
        
        // 审批通过后自动生成能耗数据
        energyDataGenerator.generateEnergyData(app);
        
        // 发送审批通知给申请人
        sendApprovalNotification(app, "批准");
        
        return app;
    }

    /**
     * 撤回用电申请
     * 仅待审批状态可撤回，且只能撤回自己的申请
     */
    @Transactional
    public void cancel(Long id, Long userId) {
        Application app = applicationMapper.selectById(id);
        if (app == null) throw new RuntimeException("申请不存在");
        if (!app.getUserId().equals(userId)) throw new RuntimeException("只能撤回自己的申请");
        if (app.getStatus() != ApplicationStatus.PENDING) throw new RuntimeException("只能撤回待审批的申请");
        applicationMapper.deleteById(id);
    }

    /**
     * 拒绝用电申请
     * 拒绝后通知申请人
     * 
     * @param id 申请ID
     * @param approverId 审批人ID
     * @param comment 拒绝原因
     * @return 更新后的申请
     */
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
        
        // 发送拒绝通知给申请人
        sendApprovalNotification(app, "拒绝");
        
        return app;
    }

    /**
     * 调整并批准用电申请
     * 修改用电时段后批准，自动生成能耗数据并通知申请人
     * 
     * @param id 申请ID
     * @param approverId 审批人ID
     * @param comment 审批意见
     * @param adjustedStartTime 调整后开始时间
     * @param adjustedEndTime 调整后结束时间
     * @return 更新后的申请
     */
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
        
        // 调整后批准也需要生成能耗数据（使用调整后的时段）
        energyDataGenerator.generateEnergyData(app);
        
        // 发送调整通知给申请人
        sendApprovalNotification(app, "调整后批准");
        
        return app;
    }
    
    /**
     * 发送审批结果通知给申请人
     * 
     * @param app 申请
     * @param action 审批动作（批准/拒绝/调整后批准）
     */
    private void sendApprovalNotification(Application app, String action) {
        if (notificationService == null) return;
        
        String title = "用电申请审批结果";
        String content = String.format("您的用电申请（%s）已被%s。", app.getApplicationNo(), action);
        if (app.getComment() != null && !app.getComment().isEmpty()) {
            content += " 审批意见：" + app.getComment();
        }
        if ("调整后批准".equals(action) && app.getAdjustedStartTime() != null) {
            content += String.format(" 调整后时段：%s-%s", 
                app.getAdjustedStartTime().toString(), 
                app.getAdjustedEndTime().toString());
        }
        
        notificationService.createNotification(app.getUserId(), "approval", title, content, app.getId());
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
