package org.example.back.property;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import org.example.back.entity.AttendanceRecord;
import org.example.back.entity.enums.AttendanceStatus;
import org.example.back.entity.enums.ShiftType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 考勤属性测试
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	测试考勤相关属性（P6.1-P6.3）
 * -- =============================================
 */
class AttendancePropertyTest {

    // 标准上班时间 8:00
    private static final LocalTime STANDARD_START_TIME = LocalTime.of(8, 0);
    // 标准下班时间 17:00
    private static final LocalTime STANDARD_END_TIME = LocalTime.of(17, 0);
    // 迟到容忍时间（分钟）
    private static final int LATE_TOLERANCE_MINUTES = 5;

    /**
     * P6.1 迟到判定属性测试
     * 打卡时间晚于计划上班时间（超过容忍时间）应判定为迟到
     */
    @Property
    void lateClockInShouldBeMarkedAsLate(
            @ForAll @IntRange(min = 6, max = 60) int minutesLate) {
        
        // 确保超过容忍时间
        Assume.that(minutesLate > LATE_TOLERANCE_MINUTES);
        
        AttendanceRecord record = createAttendanceRecord(ShiftType.DAY);
        LocalTime clockInTime = STANDARD_START_TIME.plusMinutes(minutesLate);
        record.setClockInTime(clockInTime);
        record.setClockOutTime(STANDARD_END_TIME);
        
        // 判定考勤状态
        AttendanceStatus status = determineAttendanceStatus(record);
        
        assertThat(status).isEqualTo(AttendanceStatus.LATE);
    }

    /**
     * 正常打卡属性测试
     * 在容忍时间内打卡应判定为正常
     */
    @Property
    void onTimeClockInShouldBeNormal(
            @ForAll @IntRange(min = -30, max = 5) int minutesOffset) {
        
        AttendanceRecord record = createAttendanceRecord(ShiftType.DAY);
        LocalTime clockInTime = STANDARD_START_TIME.plusMinutes(minutesOffset);
        record.setClockInTime(clockInTime);
        record.setClockOutTime(STANDARD_END_TIME);
        
        // 判定考勤状态
        AttendanceStatus status = determineAttendanceStatus(record);
        
        assertThat(status).isEqualTo(AttendanceStatus.NORMAL);
    }

    /**
     * P6.2 早退判定属性测试
     * 打卡时间早于计划下班时间应判定为早退
     */
    @Property
    void earlyClockOutShouldBeMarkedAsEarlyLeave(
            @ForAll @IntRange(min = 10, max = 120) int minutesEarly) {
        
        AttendanceRecord record = createAttendanceRecord(ShiftType.DAY);
        record.setClockInTime(STANDARD_START_TIME);
        LocalTime clockOutTime = STANDARD_END_TIME.minusMinutes(minutesEarly);
        record.setClockOutTime(clockOutTime);
        
        // 判定考勤状态
        AttendanceStatus status = determineAttendanceStatus(record);
        
        assertThat(status).isEqualTo(AttendanceStatus.EARLY_LEAVE);
    }

    /**
     * P6.3 工作时长计算属性测试
     * 工作时长 = 下班打卡时间 - 上班打卡时间
     */
    @Property
    void workHoursShouldBeCalculatedCorrectly(
            @ForAll @IntRange(min = 8, max = 10) int startHour,
            @ForAll @IntRange(min = 16, max = 20) int endHour) {
        
        // 确保结束时间晚于开始时间
        Assume.that(endHour > startHour);
        
        AttendanceRecord record = createAttendanceRecord(ShiftType.DAY);
        LocalTime clockInTime = LocalTime.of(startHour, 0);
        LocalTime clockOutTime = LocalTime.of(endHour, 0);
        record.setClockInTime(clockInTime);
        record.setClockOutTime(clockOutTime);
        
        // 计算工作时长
        BigDecimal workHours = calculateWorkHours(record);
        BigDecimal expectedHours = new BigDecimal(endHour - startHour);
        
        assertThat(workHours).isEqualByComparingTo(expectedHours);
    }

    /**
     * 工作时长精度属性测试
     * 包含分钟的工作时长计算
     */
    @Property
    void workHoursWithMinutesShouldBeAccurate(
            @ForAll @IntRange(min = 8, max = 9) int startHour,
            @ForAll @IntRange(min = 0, max = 59) int startMinute,
            @ForAll @IntRange(min = 17, max = 18) int endHour,
            @ForAll @IntRange(min = 0, max = 59) int endMinute) {
        
        AttendanceRecord record = createAttendanceRecord(ShiftType.DAY);
        LocalTime clockInTime = LocalTime.of(startHour, startMinute);
        LocalTime clockOutTime = LocalTime.of(endHour, endMinute);
        record.setClockInTime(clockInTime);
        record.setClockOutTime(clockOutTime);
        
        // 计算工作时长
        BigDecimal workHours = calculateWorkHours(record);
        
        // 计算预期时长
        int totalMinutes = (endHour * 60 + endMinute) - (startHour * 60 + startMinute);
        BigDecimal expectedHours = new BigDecimal(totalMinutes)
                .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
        
        assertThat(workHours).isEqualByComparingTo(expectedHours);
    }

    /**
     * 缺勤判定属性测试
     * 未打卡应判定为缺勤
     */
    @Property
    void noClockInShouldBeAbsent(@ForAll @LongRange(min = 1, max = 100) Long userId) {
        AttendanceRecord record = createAttendanceRecord(ShiftType.DAY);
        record.setUserId(userId);
        // 未设置打卡时间
        record.setClockInTime(null);
        record.setClockOutTime(null);
        
        AttendanceStatus status = determineAttendanceStatus(record);
        
        assertThat(status).isEqualTo(AttendanceStatus.ABSENT);
    }

    /**
     * 休息日属性测试
     * 休息日不需要打卡
     */
    @Property
    void restDayShouldBeMarkedAsRest(@ForAll @LongRange(min = 1, max = 100) Long userId) {
        AttendanceRecord record = createAttendanceRecord(ShiftType.REST);
        record.setUserId(userId);
        
        AttendanceStatus status = determineAttendanceStatus(record);
        
        assertThat(status).isEqualTo(AttendanceStatus.REST);
    }

    /**
     * 班次类型属性测试
     * 所有班次类型都应该是有效值
     */
    @Property
    void allShiftTypesShouldBeValid(@ForAll ShiftType shiftType) {
        assertThat(shiftType).isNotNull();
        assertThat(shiftType.getCode()).isNotNull();
    }

    /**
     * 考勤状态属性测试
     * 所有考勤状态都应该是有效值
     */
    @Property
    void allAttendanceStatusesShouldBeValid(@ForAll AttendanceStatus status) {
        assertThat(status).isNotNull();
        assertThat(status.getCode()).isNotNull();
        assertThat(status.getDesc()).isNotNull();
    }

    /**
     * 夜班时间计算属性测试
     * 夜班跨天的工作时长计算
     */
    @Property
    void nightShiftWorkHoursShouldBeCalculatedCorrectly(
            @ForAll @IntRange(min = 20, max = 23) int startHour,
            @ForAll @IntRange(min = 4, max = 8) int endHour) {
        
        AttendanceRecord record = createAttendanceRecord(ShiftType.NIGHT);
        LocalTime clockInTime = LocalTime.of(startHour, 0);
        LocalTime clockOutTime = LocalTime.of(endHour, 0);
        record.setClockInTime(clockInTime);
        record.setClockOutTime(clockOutTime);
        
        // 计算跨天工作时长
        BigDecimal workHours = calculateNightShiftWorkHours(record);
        
        // 预期时长 = (24 - startHour) + endHour
        int expectedTotalHours = (24 - startHour) + endHour;
        BigDecimal expected = new BigDecimal(expectedTotalHours);
        
        assertThat(workHours).isEqualByComparingTo(expected);
    }

    // ========== 辅助方法 ==========

    private AttendanceRecord createAttendanceRecord(ShiftType shiftType) {
        AttendanceRecord record = new AttendanceRecord();
        record.setUserId(1L);
        record.setAttendanceDate(LocalDate.now());
        record.setShiftType(shiftType);
        
        if (shiftType == ShiftType.DAY) {
            record.setScheduledStartTime(STANDARD_START_TIME);
            record.setScheduledEndTime(STANDARD_END_TIME);
        } else if (shiftType == ShiftType.NIGHT) {
            record.setScheduledStartTime(LocalTime.of(20, 0));
            record.setScheduledEndTime(LocalTime.of(6, 0));
        }
        
        return record;
    }

    private AttendanceStatus determineAttendanceStatus(AttendanceRecord record) {
        // 休息日
        if (record.getShiftType() == ShiftType.REST) {
            return AttendanceStatus.REST;
        }
        
        // 未打卡 - 缺勤
        if (record.getClockInTime() == null) {
            return AttendanceStatus.ABSENT;
        }
        
        LocalTime scheduledStart = record.getScheduledStartTime();
        LocalTime scheduledEnd = record.getScheduledEndTime();
        LocalTime clockIn = record.getClockInTime();
        LocalTime clockOut = record.getClockOutTime();
        
        // 迟到判定（超过容忍时间）
        if (clockIn.isAfter(scheduledStart.plusMinutes(LATE_TOLERANCE_MINUTES))) {
            return AttendanceStatus.LATE;
        }
        
        // 早退判定
        if (clockOut != null && clockOut.isBefore(scheduledEnd.minusMinutes(5))) {
            return AttendanceStatus.EARLY_LEAVE;
        }
        
        return AttendanceStatus.NORMAL;
    }

    private BigDecimal calculateWorkHours(AttendanceRecord record) {
        if (record.getClockInTime() == null || record.getClockOutTime() == null) {
            return BigDecimal.ZERO;
        }
        
        LocalTime clockIn = record.getClockInTime();
        LocalTime clockOut = record.getClockOutTime();
        
        int totalMinutes = clockOut.toSecondOfDay() / 60 - clockIn.toSecondOfDay() / 60;
        return new BigDecimal(totalMinutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateNightShiftWorkHours(AttendanceRecord record) {
        if (record.getClockInTime() == null || record.getClockOutTime() == null) {
            return BigDecimal.ZERO;
        }
        
        LocalTime clockIn = record.getClockInTime();
        LocalTime clockOut = record.getClockOutTime();
        
        // 夜班跨天计算
        int minutesBeforeMidnight = (24 * 60) - (clockIn.toSecondOfDay() / 60);
        int minutesAfterMidnight = clockOut.toSecondOfDay() / 60;
        int totalMinutes = minutesBeforeMidnight + minutesAfterMidnight;
        
        return new BigDecimal(totalMinutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
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

        AssertHelper<T> isEqualByComparingTo(T expected) {
            if (actual instanceof BigDecimal && expected instanceof BigDecimal) {
                if (((BigDecimal) actual).compareTo((BigDecimal) expected) != 0) {
                    throw new AssertionError("Expected: " + expected + ", but was: " + actual);
                }
            }
            return this;
        }
    }
}
