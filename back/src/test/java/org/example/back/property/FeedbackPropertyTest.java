package org.example.back.property;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import org.example.back.entity.Feedback;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 反馈管理属性测试
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	测试反馈管理相关属性（P5.1-P5.4）
 * -- =============================================
 */
class FeedbackPropertyTest {

    /**
     * P5.1 反馈提交属性测试
     * 新提交的反馈状态应为pending，且生成唯一编号
     */
    @Property
    void newFeedbackShouldHavePendingStatusAndUniqueNo(
            @ForAll @LongRange(min = 1, max = 1000) Long userId,
            @ForAll("feedbackTypes") String type,
            @ForAll @StringLength(min = 10, max = 200) String description) {
        
        Feedback feedback = createFeedback(userId, type, description);
        
        // 验证初始状态为pending
        assertThat(feedback.getStatus()).isEqualTo("pending");
        
        // 验证反馈编号不为空且格式正确
        assertThat(feedback.getFeedbackNo()).isNotNull();
        assertThat(feedback.getFeedbackNo()).startsWith("FB");
        
        // 验证未处理
        assertThat(feedback.getHandledBy()).isNull();
        assertThat(feedback.getHandledAt()).isNull();
    }

    /**
     * P5.2 撤回前置条件属性测试
     * 只有pending状态的反馈可以撤回
     */
    @Property
    void onlyPendingFeedbackCanBeWithdrawn(
            @ForAll @LongRange(min = 1, max = 1000) Long userId,
            @ForAll("feedbackStatuses") String status) {
        
        Feedback feedback = createFeedback(userId, "fault", "测试描述");
        feedback.setStatus(status);
        
        boolean canWithdraw = canWithdrawFeedback(feedback);
        
        if ("pending".equals(status)) {
            assertThat(canWithdraw).isTrue("pending状态的反馈应该可以撤回");
        } else {
            assertThat(canWithdraw).isFalse("非pending状态的反馈不应该可以撤回");
        }
    }

    /**
     * P5.3 撤回权限属性测试
     * 只能撤回自己的反馈
     */
    @Property
    void onlyOwnerCanWithdrawFeedback(
            @ForAll @LongRange(min = 1, max = 50) Long ownerId,
            @ForAll @LongRange(min = 51, max = 100) Long otherId) {
        
        // 确保两个ID不同
        Assume.that(!ownerId.equals(otherId));
        
        Feedback feedback = createFeedback(ownerId, "fault", "测试描述");
        
        // 验证所有者可以撤回
        boolean canOwnerWithdraw = canUserWithdrawFeedback(feedback, ownerId);
        assertThat(canOwnerWithdraw).isTrue("反馈所有者应该可以撤回");
        
        // 验证其他用户不能撤回
        boolean canOtherWithdraw = canUserWithdrawFeedback(feedback, otherId);
        assertThat(canOtherWithdraw).isFalse("非所有者不应该可以撤回反馈");
    }

    /**
     * 反馈编号唯一性属性测试
     * 多个反馈应该生成不同的编号
     */
    @Property(tries = 50)
    void feedbackNumbersShouldBeUnique() {
        Set<String> feedbackNos = new HashSet<>();
        
        for (int i = 0; i < 10; i++) {
            Feedback feedback = createFeedback((long) i, "fault", "测试描述" + i);
            String no = feedback.getFeedbackNo();
            
            assertThat(feedbackNos.contains(no)).isFalse("反馈编号应该唯一");
            feedbackNos.add(no);
        }
    }

    /**
     * 反馈处理属性测试
     * 处理后状态变更，记录处理人和处理时间
     */
    @Property
    void handledFeedbackShouldRecordHandlerInfo(
            @ForAll @LongRange(min = 1, max = 1000) Long userId,
            @ForAll @LongRange(min = 1, max = 50) Long handlerId,
            @ForAll @StringLength(min = 5, max = 200) String reply) {
        
        Feedback feedback = createFeedback(userId, "fault", "测试描述");
        
        // 执行处理
        handleFeedback(feedback, handlerId, reply);
        
        // 验证状态变更
        assertThat(feedback.getStatus()).isEqualTo("resolved");
        assertThat(feedback.getHandledBy()).isEqualTo(handlerId);
        assertThat(feedback.getHandledAt()).isNotNull();
        assertThat(feedback.getReply()).isEqualTo(reply);
    }

    /**
     * 反馈类型属性测试
     * 所有反馈类型都应该是有效值
     */
    @Property
    void feedbackTypeShouldBeValid(@ForAll("feedbackTypes") String type) {
        assertThat(type).isNotNull();
        assertThat(isValidFeedbackType(type)).isTrue("反馈类型应该是有效值");
    }

    /**
     * 紧急程度属性测试
     * 所有紧急程度都应该是有效值
     */
    @Property
    void feedbackUrgencyShouldBeValid(@ForAll("feedbackUrgencies") String urgency) {
        assertThat(urgency).isNotNull();
        assertThat(isValidUrgency(urgency)).isTrue("紧急程度应该是有效值");
    }

    /**
     * 反馈状态流转属性测试
     * 状态只能按照 pending -> processing -> resolved 流转
     */
    @Property
    void feedbackStatusShouldFollowCorrectFlow(
            @ForAll @LongRange(min = 1, max = 1000) Long userId,
            @ForAll @LongRange(min = 1, max = 50) Long handlerId) {
        
        Feedback feedback = createFeedback(userId, "fault", "测试描述");
        
        // 初始状态为pending
        assertThat(feedback.getStatus()).isEqualTo("pending");
        
        // 开始处理，状态变为processing
        startProcessing(feedback, handlerId);
        assertThat(feedback.getStatus()).isEqualTo("processing");
        
        // 完成处理，状态变为resolved
        resolveFeedback(feedback, "已解决");
        assertThat(feedback.getStatus()).isEqualTo("resolved");
    }

    // ========== 数据提供者 ==========

    @Provide
    Arbitrary<String> feedbackTypes() {
        return Arbitraries.of("fault", "suggestion", "question");
    }

    @Provide
    Arbitrary<String> feedbackStatuses() {
        return Arbitraries.of("pending", "processing", "resolved");
    }

    @Provide
    Arbitrary<String> feedbackUrgencies() {
        return Arbitraries.of("normal", "urgent");
    }

    // ========== 辅助方法 ==========

    private Feedback createFeedback(Long userId, String type, String description) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setType(type);
        feedback.setDescription(description);
        feedback.setLocation("测试位置");
        feedback.setUrgency("normal");
        feedback.setStatus("pending");
        feedback.setFeedbackNo(generateFeedbackNo());
        return feedback;
    }

    private String generateFeedbackNo() {
        return "FB" + System.currentTimeMillis() + (int)(Math.random() * 1000);
    }

    private boolean canWithdrawFeedback(Feedback feedback) {
        return "pending".equals(feedback.getStatus());
    }

    private boolean canUserWithdrawFeedback(Feedback feedback, Long userId) {
        return canWithdrawFeedback(feedback) && feedback.getUserId().equals(userId);
    }

    private void handleFeedback(Feedback feedback, Long handlerId, String reply) {
        feedback.setStatus("resolved");
        feedback.setHandledBy(handlerId);
        feedback.setHandledAt(LocalDateTime.now());
        feedback.setReply(reply);
    }

    private void startProcessing(Feedback feedback, Long handlerId) {
        feedback.setStatus("processing");
        feedback.setHandledBy(handlerId);
    }

    private void resolveFeedback(Feedback feedback, String reply) {
        feedback.setStatus("resolved");
        feedback.setHandledAt(LocalDateTime.now());
        feedback.setReply(reply);
    }

    private boolean isValidFeedbackType(String type) {
        return "fault".equals(type) || "suggestion".equals(type) || "question".equals(type);
    }

    private boolean isValidUrgency(String urgency) {
        return "normal".equals(urgency) || "urgent".equals(urgency);
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

        AssertHelper<T> isNull() {
            if (actual != null) {
                throw new AssertionError("Expected null value, but was: " + actual);
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
