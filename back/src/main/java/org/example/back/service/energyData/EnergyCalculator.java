package org.example.back.service.energyData;

import org.example.back.entity.enums.TimePeriodType;
import org.example.back.util.EnergyCalculationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

/**
 * 能耗计算工具类
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	能耗计算、时段判定、电价获取
 * -- =============================================
 * 
 * 时段规则（根据需求7.1）：
 * - 峰时(peak)：8:00-12:00, 18:00-22:00 → 1.2元/kWh
 * - 平时(normal)：12:00-18:00 → 0.8元/kWh
 * - 谷时(valley)：22:00-8:00（即22:00-24:00和0:00-8:00）→ 0.4元/kWh
 */
public class EnergyCalculator {

    private EnergyCalculator() {
        // 工具类，禁止实例化
    }

    /**
     * 根据小时判断时段类型
     * 
     * 时段划分：
     * - 峰时：8-11点（8:00-12:00）和 18-21点（18:00-22:00）
     * - 平时：12-17点（12:00-18:00）
     * - 谷时：22-23点和0-7点（22:00-8:00）
     * 
     * @param hour 小时 (0-23)
     * @return 时段类型
     */
    public static TimePeriodType getTimePeriodType(int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("小时必须在 0-23 范围内");
        }

        // 峰时：8-11点 (8:00-12:00) 或 18-21点 (18:00-22:00)
        if ((hour >= 8 && hour < 12) || (hour >= 18 && hour < 22)) {
            return TimePeriodType.PEAK;
        }

        // 平时：12-17点 (12:00-18:00)
        if (hour >= 12 && hour < 18) {
            return TimePeriodType.NORMAL;
        }

        // 谷时：22-23点 或 0-7点 (22:00-8:00)
        return TimePeriodType.VALLEY;
    }

    /**
     * 获取时段电价
     * 
     * @param periodType 时段类型
     * @return 电价 (元/kWh)
     */
    public static BigDecimal getPrice(TimePeriodType periodType) {
        return periodType.getPrice();
    }

    /**
     * 计算跨时段能耗
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param power     功率 (kW)
     * @return 计算结果
     */
    public static EnergyCalculationResult calculateCrossTimePeriod(
            LocalTime startTime, LocalTime endTime, BigDecimal power) {
        
        EnergyCalculationResult result = new EnergyCalculationResult();
        
        int startHour = startTime.getHour();
        int startMinute = startTime.getMinute();
        int endHour = endTime.getHour();
        int endMinute = endTime.getMinute();

        // 处理跨天情况（如 22:00 到 06:00）
        boolean crossMidnight = endTime.isBefore(startTime) || endTime.equals(startTime);
        
        if (crossMidnight) {
            // 跨天：分两段计算
            // 第一段：startTime 到 24:00
            calculateSegment(result, startHour, startMinute, 24, 0, power);
            // 第二段：00:00 到 endTime
            calculateSegment(result, 0, 0, endHour, endMinute, power);
        } else {
            // 不跨天：直接计算
            calculateSegment(result, startHour, startMinute, endHour, endMinute, power);
        }

        return result;
    }

    /**
     * 计算一个时间段内的能耗（按小时拆分）
     */
    private static void calculateSegment(EnergyCalculationResult result, 
            int startHour, int startMinute, int endHour, int endMinute, BigDecimal power) {
        
        // 如果开始和结束在同一小时
        if (startHour == endHour || (endHour == 24 && startMinute == 0)) {
            if (endHour == 24) {
                endHour = 0;
                endMinute = 0;
            }
            if (startHour != endHour || startMinute != endMinute) {
                BigDecimal hours = calculateHours(startMinute, endMinute == 0 && endHour == 0 ? 60 : endMinute);
                if (hours.compareTo(BigDecimal.ZERO) > 0) {
                    addHourDetail(result, startHour, hours, power);
                }
            }
            return;
        }

        // 处理第一个小时（可能不完整）
        if (startMinute > 0) {
            BigDecimal firstHourMinutes = new BigDecimal(60 - startMinute);
            BigDecimal hours = firstHourMinutes.divide(new BigDecimal("60"), 4, RoundingMode.HALF_UP);
            addHourDetail(result, startHour, hours, power);
            startHour++;
        }

        // 处理中间的完整小时
        int lastFullHour = endMinute > 0 ? endHour : endHour;
        for (int hour = startHour; hour < lastFullHour; hour++) {
            int actualHour = hour % 24;
            addHourDetail(result, actualHour, BigDecimal.ONE, power);
        }

        // 处理最后一个小时（可能不完整）
        if (endMinute > 0 && endHour < 24) {
            BigDecimal hours = new BigDecimal(endMinute).divide(new BigDecimal("60"), 4, RoundingMode.HALF_UP);
            addHourDetail(result, endHour, hours, power);
        }
    }

    /**
     * 计算分钟差对应的小时数
     */
    private static BigDecimal calculateHours(int startMinute, int endMinute) {
        int minutes = endMinute - startMinute;
        return new BigDecimal(minutes).divide(new BigDecimal("60"), 4, RoundingMode.HALF_UP);
    }

    /**
     * 添加某小时的能耗明细
     */
    private static void addHourDetail(EnergyCalculationResult result, int hour, 
            BigDecimal hours, BigDecimal power) {
        
        TimePeriodType periodType = getTimePeriodType(hour);
        BigDecimal price = getPrice(periodType);
        BigDecimal energy = power.multiply(hours).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cost = energy.multiply(price).setScale(2, RoundingMode.HALF_UP);

        EnergyCalculationResult.PeriodDetail detail = new EnergyCalculationResult.PeriodDetail(
                periodType, hours, energy, price, cost, hour, hour + 1
        );
        result.addPeriodDetail(detail);
    }

    /**
     * 计算简单能耗（不分时段）
     * 
     * @param power 功率 (kW)
     * @param hours 时长 (小时)
     * @return 能耗 (kWh)
     */
    public static BigDecimal calculateEnergy(BigDecimal power, BigDecimal hours) {
        return power.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算带波动系数的能耗
     * 
     * @param power           功率 (kW)
     * @param hours           时长 (小时)
     * @param fluctuationRate 波动系数 (0.85-1.05)
     * @return 能耗 (kWh)
     */
    public static BigDecimal calculateEnergyWithFluctuation(BigDecimal power, BigDecimal hours, 
            BigDecimal fluctuationRate) {
        return power.multiply(hours).multiply(fluctuationRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 生成随机波动系数 (0.85-1.05)
     * 
     * @return 波动系数
     */
    public static BigDecimal generateFluctuationRate() {
        // 生成 0.85 到 1.05 之间的随机数
        double rate = 0.85 + Math.random() * 0.20;
        return new BigDecimal(rate).setScale(4, RoundingMode.HALF_UP);
    }
}
