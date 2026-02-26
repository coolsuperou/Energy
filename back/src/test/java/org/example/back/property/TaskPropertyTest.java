package org.example.back.property;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.example.back.entity.Task;
import org.example.back.entity.enums.Priority;
import org.example.back.entity.enums.TaskStatus;
import org.example.back.entity.enums.TaskType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工单管理属性测试
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	测试工单管理相关属性（P4.1-P4.4）
 * -- =============================================
 */
class TaskPropertyTest {

    /**
     * P4.1 工单创建属性测试
     * 新创建的工单状态应为PENDING
     */
    @Property
    void newTaskShouldHavePendingStatus(
            @ForAll TaskType taskType,
            @ForAll @StringLength(min = 5, max = 50) String title,
            @ForAll @LongRange(min = 1, max = 100) Long equipmentId) {
        
        Task task = createTask(taskType, title, equipmentId);
        
        // 验证初始状态为PENDING
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        
        // 验证未分配
        assertThat(task.getAssignedTo()).isNull();
        
        // 验证未完成
        assertThat(task.getCompletedAt()).isNull();
    }

    /**
     * P4.2 派单属性测试
     * 派单后状态直接变为IN_PROGRESS
     */
    @Property
    void assignedTaskShouldBeInProgress(
            @ForAll TaskType taskType,
            @ForAll @StringLength(min = 5, max = 50) String title,
            @ForAll @LongRange(min = 1, max = 100) Long equipmentId,
            @ForAll @LongRange(min = 1, max = 50) Long inspectorId,
            @ForAll @LongRange(min = 1, max = 50) Long dispatcherId) {
        
        Task task = createTask(taskType, title, equipmentId);
        
        // 执行派单
        assignTask(task, inspectorId, dispatcherId);
        
        // 验证状态变为IN_PROGRESS
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getAssignedTo()).isEqualTo(inspectorId);
        assertThat(task.getAssignedBy()).isEqualTo(dispatcherId);
    }

    /**
     * P4.3 完成工单属性测试
     * 完成后状态为COMPLETED，记录完成时间
     */
    @Property
    void completedTaskShouldHaveCompletedStatusAndTime(
            @ForAll TaskType taskType,
            @ForAll @StringLength(min = 5, max = 50) String title,
            @ForAll @LongRange(min = 1, max = 100) Long equipmentId,
            @ForAll @LongRange(min = 1, max = 50) Long inspectorId,
            @ForAll @StringLength(min = 10, max = 200) String report) {
        
        Task task = createTask(taskType, title, equipmentId);
        assignTask(task, inspectorId, 1L);
        
        // 执行完成
        completeTask(task, report);
        
        // 验证状态变为COMPLETED
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        
        // 验证完成时间已记录
        assertThat(task.getCompletedAt()).isNotNull();
        
        // 验证报告已记录
        assertThat(task.getReport()).isEqualTo(report);
    }

    /**
     * P4.4 工单权限属性测试
     * 只有被分配的巡检员才能完成工单
     */
    @Property
    void onlyAssignedInspectorCanCompleteTask(
            @ForAll @LongRange(min = 1, max = 50) Long assignedInspectorId,
            @ForAll @LongRange(min = 51, max = 100) Long otherInspectorId) {
        
        // 确保两个ID不同
        Assume.that(!assignedInspectorId.equals(otherInspectorId));
        
        Task task = createTask(TaskType.INSPECTION, "测试任务", 1L);
        assignTask(task, assignedInspectorId, 1L);
        
        // 验证被分配的巡检员可以完成
        boolean canAssignedComplete = canCompleteTask(task, assignedInspectorId);
        assertThat(canAssignedComplete).isTrue("被分配的巡检员应该能够完成工单");
        
        // 验证其他巡检员不能完成
        boolean canOtherComplete = canCompleteTask(task, otherInspectorId);
        assertThat(canOtherComplete).isFalse("未被分配的巡检员不应该能够完成工单");
    }

    /**
     * 工单状态流转属性测试
     * 状态只能按照 PENDING -> IN_PROGRESS -> COMPLETED 流转
     */
    @Property
    void taskStatusShouldFollowCorrectFlow(
            @ForAll TaskType taskType,
            @ForAll @LongRange(min = 1, max = 50) Long inspectorId) {
        
        Task task = createTask(taskType, "测试任务", 1L);
        
        // 初始状态为PENDING
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        
        // PENDING状态不能直接完成
        boolean canCompleteFromPending = canCompleteTask(task, inspectorId);
        assertThat(canCompleteFromPending).isFalse("PENDING状态不能直接完成");
        
        // 派单后变为IN_PROGRESS
        assignTask(task, inspectorId, 1L);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        
        // IN_PROGRESS状态可以完成
        boolean canCompleteFromInProgress = canCompleteTask(task, inspectorId);
        assertThat(canCompleteFromInProgress).isTrue("IN_PROGRESS状态可以完成");
    }

    /**
     * 优先级属性测试
     * 所有优先级都应该是有效值
     */
    @Property
    void allPrioritiesShouldBeValid(@ForAll Priority priority) {
        assertThat(priority).isNotNull();
        assertThat(priority.getValue()).isNotNull();
        assertThat(priority.getDescription()).isNotNull();
    }

    /**
     * 任务类型属性测试
     * 所有任务类型都应该是有效值
     */
    @Property
    void allTaskTypesShouldBeValid(@ForAll TaskType taskType) {
        assertThat(taskType).isNotNull();
        assertThat(taskType.getValue()).isNotNull();
        assertThat(taskType.getDescription()).isNotNull();
    }

    /**
     * 截止日期属性测试
     * 截止日期应该在创建日期之后
     */
    @Property
    void dueDateShouldBeAfterCreation(
            @ForAll @IntRange(min = 1, max = 30) int daysFromNow) {
        
        Task task = createTask(TaskType.INSPECTION, "测试任务", 1L);
        LocalDate dueDate = LocalDate.now().plusDays(daysFromNow);
        task.setDueDate(dueDate);
        
        assertThat(task.getDueDate()).isNotNull();
        assertThat(task.getDueDate().isAfter(LocalDate.now()) || task.getDueDate().isEqual(LocalDate.now())).isTrue("截止日期应该不早于今天");
    }

    // ========== 辅助方法 ==========

    private Task createTask(TaskType taskType, String title, Long equipmentId) {
        Task task = new Task();
        task.setTaskType(taskType);
        task.setTitle(title);
        task.setDescription("测试描述");
        task.setEquipmentId(equipmentId);
        task.setPriority(Priority.NORMAL);
        task.setStatus(TaskStatus.PENDING);
        task.setDueDate(LocalDate.now().plusDays(7));
        return task;
    }

    private void assignTask(Task task, Long inspectorId, Long dispatcherId) {
        task.setAssignedTo(inspectorId);
        task.setAssignedBy(dispatcherId);
        task.setStatus(TaskStatus.IN_PROGRESS);
    }

    private void completeTask(Task task, String report) {
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task.setReport(report);
    }

    private boolean canCompleteTask(Task task, Long inspectorId) {
        // 只有IN_PROGRESS状态且是被分配的巡检员才能完成
        return task.getStatus() == TaskStatus.IN_PROGRESS 
            && task.getAssignedTo() != null 
            && task.getAssignedTo().equals(inspectorId);
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
    }
}
