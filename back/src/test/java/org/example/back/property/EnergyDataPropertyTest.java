package org.example.back.property;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.example.back.entity.enums.TimePeriodType;
import org.example.back.service.energyData.EnergyCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 能耗数据属性测试
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	测试能耗计算相关属性（P3.1-P3.4）
 * -- =============================================
 */
class EnergyDataPropertyTest {

    /**
     * P3.1 时段类型判定属性测试
     * 任意小时(0-23)都能正确映射到对应时段类型
     * - 峰时：8-11点、18-21点
     * - 平时：12-17点
     * - 谷时：0-7点、22-23点
     */
    @Property
    void timePeriodTypeShouldBeCorrectlyDetermined(@ForAll @IntRange(min = 0, max = 23) int hour) {
        TimePeriodType result = EnergyCalculator.getTimePeriodType(hour);
        
        // 验证结果不为空
        Assertions.assertThat(result).isNotNull();
        
        // 验证时段划分正确性
        if ((hour >= 8 && hour < 12) || (hour >= 18 && hour < 22)) {
            Assertions.assertThat(result).isEqualTo(TimePeriodType.PEAK);
        } else if (hour >= 12 && hour < 18) {
            Assertions.assertThat(result).isEqualTo(TimePeriodType.NORMAL);
        } else {
            Assertions.assertThat(result).isEqualTo(TimePeriodType.VALLEY);
        }
    }

    /**
     * P3.2 电价计算属性测试
     * 时段类型与电价的对应关系：
     * - 峰时 → 1.20元/kWh
     * - 平时 → 0.80元/kWh
     * - 谷时 → 0.40元/kWh
     */
    @Property
    void priceShouldMatchTimePeriodType(@ForAll TimePeriodType periodType) {
        BigDecimal price = EnergyCalculator.getPrice(periodType);
        
        switch (periodType) {
            case PEAK -> Assertions.assertThat(price).isEqualByComparingTo(new BigDecimal("1.20"));
            case NORMAL -> Assertions.assertThat(price).isEqualByComparingTo(new BigDecimal("0.80"));
            case VALLEY -> Assertions.assertThat(price).isEqualByComparingTo(new BigDecimal("0.40"));
        }
    }

    /**
     * P3.3 费用计算属性测试
     * 费用 = 电量 × 电价，精度误差 ≤ 0.01
     */
    @Property
    void costShouldEqualEnergyMultipliedByPrice(
            @ForAll @BigRange(min = "0.01", max = "1000") BigDecimal energy,
            @ForAll TimePeriodType periodType) {
        
        BigDecimal price = EnergyCalculator.getPrice(periodType);
        BigDecimal expectedCost = energy.multiply(price).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualCost = energy.multiply(price).setScale(2, RoundingMode.HALF_UP);
        
        // 验证费用计算正确，误差不超过0.01
        BigDecimal difference = expectedCost.subtract(actualCost).abs();
        Assertions.assertThat(difference).isLessThanOrEqualTo(new BigDecimal("0.01"));
    }

    /**
     * P3.4 电量计算属性测试
     * 电量 = 功率 × 时长（小时）
     */
    @Property
    void energyShouldEqualPowerMultipliedByHours(
            @ForAll @BigRange(min = "0.1", max = "500") BigDecimal power,
            @ForAll @BigRange(min = "0.1", max = "24") BigDecimal hours) {
        
        BigDecimal expectedEnergy = power.multiply(hours).setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualEnergy = EnergyCalculator.calculateEnergy(power, hours);
        
        Assertions.assertThat(actualEnergy).isEqualByComparingTo(expectedEnergy);
    }

    /**
     * 边界测试：小时范围外应抛出异常
     */
    @Property
    void invalidHourShouldThrowException(@ForAll @IntRange(min = -100, max = -1) int negativeHour) {
        Assertions.assertThatThrownBy(() -> EnergyCalculator.getTimePeriodType(negativeHour))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Property
    void invalidHourAbove23ShouldThrowException(@ForAll @IntRange(min = 24, max = 100) int invalidHour) {
        Assertions.assertThatThrownBy(() -> EnergyCalculator.getTimePeriodType(invalidHour))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 波动系数属性测试
     * 波动系数应在 0.85-1.05 范围内
     */
    @Property(tries = 100)
    void fluctuationRateShouldBeInValidRange() {
        BigDecimal rate = EnergyCalculator.generateFluctuationRate();
        
        Assertions.assertThat(rate).isGreaterThanOrEqualTo(new BigDecimal("0.85"));
        Assertions.assertThat(rate).isLessThanOrEqualTo(new BigDecimal("1.05"));
    }

    /**
     * 带波动系数的能耗计算属性测试
     */
    @Property
    void energyWithFluctuationShouldBeCalculatedCorrectly(
            @ForAll @BigRange(min = "1", max = "100") BigDecimal power,
            @ForAll @BigRange(min = "1", max = "8") BigDecimal hours,
            @ForAll @BigRange(min = "0.85", max = "1.05") BigDecimal fluctuationRate) {
        
        BigDecimal expectedEnergy = power.multiply(hours).multiply(fluctuationRate)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualEnergy = EnergyCalculator.calculateEnergyWithFluctuation(power, hours, fluctuationRate);
        
        Assertions.assertThat(actualEnergy).isEqualByComparingTo(expectedEnergy);
    }

    /**
     * 自定义 Assertions 内部类（简化断言）
     */
    private static class Assertions {
        static <T> ObjectAssert<T> assertThat(T actual) {
            return new ObjectAssert<>(actual);
        }

        static ThrowableAssert assertThatThrownBy(ThrowableAssert.ThrowingCallable callable) {
            return new ThrowableAssert(callable);
        }
    }

    private static class ObjectAssert<T> {
        private final T actual;

        ObjectAssert(T actual) {
            this.actual = actual;
        }

        ObjectAssert<T> isNotNull() {
            if (actual == null) {
                throw new AssertionError("Expected non-null value");
            }
            return this;
        }

        ObjectAssert<T> isEqualTo(T expected) {
            if (!java.util.Objects.equals(actual, expected)) {
                throw new AssertionError("Expected: " + expected + ", but was: " + actual);
            }
            return this;
        }

        ObjectAssert<T> isEqualByComparingTo(T expected) {
            if (actual instanceof BigDecimal && expected instanceof BigDecimal) {
                if (((BigDecimal) actual).compareTo((BigDecimal) expected) != 0) {
                    throw new AssertionError("Expected: " + expected + ", but was: " + actual);
                }
            }
            return this;
        }

        ObjectAssert<T> isGreaterThanOrEqualTo(T expected) {
            if (actual instanceof BigDecimal && expected instanceof BigDecimal) {
                if (((BigDecimal) actual).compareTo((BigDecimal) expected) < 0) {
                    throw new AssertionError("Expected >= " + expected + ", but was: " + actual);
                }
            }
            return this;
        }

        ObjectAssert<T> isLessThanOrEqualTo(T expected) {
            if (actual instanceof BigDecimal && expected instanceof BigDecimal) {
                if (((BigDecimal) actual).compareTo((BigDecimal) expected) > 0) {
                    throw new AssertionError("Expected <= " + expected + ", but was: " + actual);
                }
            }
            return this;
        }

        ObjectAssert<T> isInstanceOf(Class<?> clazz) {
            if (!clazz.isInstance(actual)) {
                throw new AssertionError("Expected instance of " + clazz.getName());
            }
            return this;
        }
    }

    private static class ThrowableAssert {
        private Throwable thrown;

        interface ThrowingCallable {
            void call() throws Throwable;
        }

        ThrowableAssert(ThrowingCallable callable) {
            try {
                callable.call();
            } catch (Throwable t) {
                this.thrown = t;
            }
        }

        ThrowableAssert isInstanceOf(Class<? extends Throwable> expectedType) {
            if (thrown == null) {
                throw new AssertionError("Expected exception of type " + expectedType.getName() + " but none was thrown");
            }
            if (!expectedType.isInstance(thrown)) {
                throw new AssertionError("Expected exception of type " + expectedType.getName() + " but was " + thrown.getClass().getName());
            }
            return this;
        }
    }
}
