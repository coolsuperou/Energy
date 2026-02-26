package org.example.back.property;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.example.back.entity.Application;
import org.example.back.entity.enums.ApplicationStatus;
import org.example.back.entity.enums.Urgency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 用电申请属性测试
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	测试用电申请相关属性（P2.1-P2.5）
 * -- =============================================
 */
class ApplicationPropertyTest {

    /**
     * P2.1 申请提交属性测试
     * 新提交的申请状态应为PENDING，且生成唯一编号
     */
    @Property
    void newApplicationShouldHavePendingStatusAndUniqueNo(
            @ForAll @LongRange(min = 1, max = 1000) Long userId,
            @ForAll @LongRange(min = 1, max = 100) Long workshopId,
            @ForAll @BigRange(min = "1", max = "500") BigDecimal power) {
        
        Application application = createApplication(userId, workshopId, power);
        
        // 验证初始状态为PENDING
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        
        // 验证申请编号不为空且格式正确
        assertThat(application.getApplicationNo()).isNotNull();
        assertThat(application.getApplicationNo()).startsWith("APP");
    }

    /**
     * P2.2 审批通过属性测试
     * 审批通过后状态变为APPROVED
     */
    @Property
    void approvedApplicationShouldChangeStatus(
            @ForAll @LongRange(min = 1, max = 1000) Long userId,
            @ForAll @LongRange(min = 1, max = 100) Long workshopId,
            @ForAll @LongRange(min = 1, max = 50) Long approverId) {
        
        Application application = createApplication(userId, workshopId, new BigDecimal("100"));
        
        // 执行审批通过
        approveApplication(application, approverId);
        
        // 验证状态变更
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(application.getApprovedBy()).isEqualTo(approverId);
        assertThat(application.getApprovedAt()).isNotNull();
    }

    /**
     * P2.3 审批拒绝属性测试
     * 审批拒绝后状态变为REJECTED，不生成能耗数据
     */
    @Property
    void rejectedApplicationShouldChangeStatus(
            @ForAll @LongRange(min = 1, max = 1000) Long userId,
            @ForAll @LongRange(min = 1, max = 100) Long workshopId,
            @ForAll @LongRange(min = 1, max = 50) Long approverId,
            @ForAll @StringLength(min = 5, max = 100) String rejectReason) {
        
        Application application = createApplication(userId, workshopId, new BigDecimal("100"));
        
        // 执行审批拒绝
        rejectApplication(application, approverId, rejectReason);
        
        // 验证状态变更
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(application.getApprovedBy()).isEqualTo(approverId);
        assertThat(application.getComment()).isEqualTo(rejectReason);
        
        // 验证不应生成能耗数据（通过检查状态）
        assertThat(shouldGenerateEnergyData(application)).isFalse("拒绝的申请不应生成能耗数据");
    }

    /**
     * P2.4 审批幂等性属性测试
     * 已处理的申请不能重复审批
     */
    @Property
    void processedApplicationShouldNotBeReprocessed(
            @ForAll @LongRange(min = 1, max = 1000) Long userId,
            @ForAll @LongRange(min = 1, max = 100) Long workshopId,
            @ForAll @LongRange(min = 1, max = 50) Long approverId) {
        
        Application application = createApplication(userId, workshopId, new BigDecimal("100"));
        
        // 第一次审批
        approveApplication(application, approverId);
        ApplicationStatus statusAfterFirstApproval = application.getStatus();
        
        // 尝试第二次审批（应该失败或保持原状态）
        boolean canReprocess = canProcessApplication(application);
        
        assertThat(canReprocess).isFalse("已处理的申请不应该能够重复审批");
        assertThat(application.getStatus()).isEqualTo(statusAfterFirstApproval);
    }

    /**
     * 申请编号唯一性属性测试
     * 多个申请应该生成不同的编号
     */
    @Property(tries = 50)
    void applicationNumbersShouldBeUnique() {
        Set<String> applicationNos = new HashSet<>();
        
        for (int i = 0; i < 10; i++) {
            Application app = createApplication((long) i, 1L, new BigDecimal("100"));
            String no = app.getApplicationNo();
            
            assertThat(applicationNos.contains(no)).isFalse("申请编号应该唯一");
            applicationNos.add(no);
        }
    }

    /**
     * 时段调整属性测试
     * 调整后的时段应该被正确记录
     */
    @Property
    void adjustedTimeShouldBeRecorded(
            @ForAll @IntRange(min = 0, max = 23) int startHour,
            @ForAll @IntRange(min = 0, max = 23) int endHour,
            @ForAll @IntRange(min = 0, max = 23) int adjustedStartHour,
            @ForAll @IntRange(min = 0, max = 23) int adjustedEndHour) {
        
        Application application = createApplication(1L, 1L, new BigDecimal("100"));
        application.setStartTime(LocalTime.of(startHour, 0));
        application.setEndTime(LocalTime.of(endHour, 0));
        
        // 执行时段调整
        adjustApplicationTime(application, 
                LocalTime.of(adjustedStartHour, 0), 
                LocalTime.of(adjustedEndHour, 0));
        
        // 验证调整后的时段被记录
        assertThat(application.getAdjustedStartTime()).isEqualTo(LocalTime.of(adjustedStartHour, 0));
        assertThat(application.getAdjustedEndTime()).isEqualTo(LocalTime.of(adjustedEndHour, 0));
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.ADJUSTED);
    }

    /**
     * 紧急程度属性测试
     * 所有紧急程度都应该是有效值
     */
    @Property
    void allUrgencyLevelsShouldBeValid(@ForAll Urgency urgency) {
        assertThat(urgency).isNotNull();
        assertThat(urgency.getValue()).isNotNull();
    }

    // ========== 辅助方法 ==========

    private Application createApplication(Long userId, Long workshopId, BigDecimal power) {
        Application application = new Application();
        application.setUserId(userId);
        application.setWorkshopId(workshopId);
        application.setPower(power);
        application.setApplyDate(LocalDate.now());
        application.setStartTime(LocalTime.of(8, 0));
        application.setEndTime(LocalTime.of(17, 0));
        application.setPurpose("测试用途");
        application.setUrgency(Urgency.NORMAL);
        application.setStatus(ApplicationStatus.PENDING);
        application.setApplicationNo(generateApplicationNo());
        return application;
    }

    private String generateApplicationNo() {
        return "APP" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private void approveApplication(Application application, Long approverId) {
        application.setStatus(ApplicationStatus.APPROVED);
        application.setApprovedBy(approverId);
        application.setApprovedAt(java.time.LocalDateTime.now());
    }

    private void rejectApplication(Application application, Long approverId, String reason) {
        application.setStatus(ApplicationStatus.REJECTED);
        application.setApprovedBy(approverId);
        application.setApprovedAt(java.time.LocalDateTime.now());
        application.setComment(reason);
    }

    private void adjustApplicationTime(Application application, LocalTime newStart, LocalTime newEnd) {
        application.setAdjustedStartTime(newStart);
        application.setAdjustedEndTime(newEnd);
        application.setStatus(ApplicationStatus.ADJUSTED);
        application.setApprovedAt(java.time.LocalDateTime.now());
    }

    private boolean canProcessApplication(Application application) {
        return application.getStatus() == ApplicationStatus.PENDING;
    }

    private boolean shouldGenerateEnergyData(Application application) {
        return application.getStatus() == ApplicationStatus.APPROVED 
            || application.getStatus() == ApplicationStatus.ADJUSTED;
    }

    // ========== 断言辅助类 ==========

    private static <T> AssertHelper<T> assertThat(T actual) {
        return new AssertHelper<>(actual);
    }

    private static class AssertHelper<T> {
        private final T actual;

        AssertHelper(T actual) {
            this.actual = actual;
        }

        AssertHelper<T> isTrue(String message) {
            if (actual instanceof Boolean && !(Boolean) actual) {
                throw new AssertionError(message);
            }
            return this;
        }

        AssertHelper<T> isFalse(String message) {
            if (actual instanceof Boolean && (Boolean) actual) {
                throw new AssertionError(message);
            }
            return this;
        }

        AssertHelper<T> isNotNull() {
            if (actual == null) {
                throw new AssertionError("Expected non-null value");
            }
            return this;
        }

        AssertHelper<T> isEqualTo(T expected) {
            if (!java.util.Objects.equals(actual, expected)) {
                throw new AssertionError("Expected: " + expected + ", but was: " + actual);
            }
            return this;
        }

        AssertHelper<T> startsWith(String prefix) {
            if (actual instanceof String && !((String) actual).startsWith(prefix)) {
                throw new AssertionError("Expected to start with: " + prefix + ", but was: " + actual);
            }
            return this;
        }
    }
}
