package org.example.back.service.manager;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.back.entity.EnergyData;
import org.example.back.entity.enums.TimePeriodType;
import org.example.back.mapper.common.EnergyDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * 经理数据分析服务
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	经理数据分析逻辑，提供车间对比、时段分析、成本统计
 * -- =============================================
 */
@Service
public class ManagerAnalysisService {

    @Autowired
    private EnergyDataMapper energyDataMapper;

    // 车间名称映射
    private static final Map<Long, String> WORKSHOP_NAMES = new HashMap<>();
    static {
        WORKSHOP_NAMES.put(1L, "生产一车间");
        WORKSHOP_NAMES.put(2L, "生产二车间");
        WORKSHOP_NAMES.put(3L, "装配车间");
        WORKSHOP_NAMES.put(4L, "仓储车间");
    }

    /**
     * 获取能耗数据汇总分析
     */
    public Map<String, Object> getEnergySummary(LocalDate startDate, LocalDate endDate, Long workshopId) {
        Map<String, Object> result = new HashMap<>();
        
        // 1. 总体汇总
        result.put("overview", getOverview(startDate, endDate, workshopId));
        
        // 2. 车间对比
        result.put("workshopCompare", getWorkshopCompare(startDate, endDate));
        
        // 3. 时段分析
        result.put("periodAnalysis", getPeriodAnalysis(startDate, endDate, workshopId));
        
        // 4. 成本趋势（日粒度）
        result.put("costTrend", getCostTrend(startDate, endDate, workshopId, "daily"));
        
        return result;
    }

    /**
     * 获取总体概览数据
     */
    private Map<String, Object> getOverview(LocalDate startDate, LocalDate endDate, Long workshopId) {
        Map<String, Object> overview = new HashMap<>();
        
        QueryWrapper<EnergyData> wrapper = new QueryWrapper<>();
        wrapper.between("record_date", startDate, endDate);
        if (workshopId != null) {
            wrapper.eq("workshop_id", workshopId);
        }
        wrapper.select(
            "SUM(energy) as total_energy",
            "SUM(cost) as total_cost",
            "AVG(power) as avg_power",
            "MAX(power) as max_power",
            "COUNT(*) as record_count"
        );
        
        Map<String, Object> stats = energyDataMapper.selectMaps(wrapper).stream()
                .findFirst().orElse(new HashMap<>());
        
        overview.put("totalEnergy", toBigDecimal(stats.get("total_energy")));
        overview.put("totalCost", toBigDecimal(stats.get("total_cost")));
        overview.put("avgPower", toBigDecimal(stats.get("avg_power")));
        overview.put("maxPower", toBigDecimal(stats.get("max_power")));
        overview.put("recordCount", toLong(stats.get("record_count")));
        overview.put("startDate", startDate.toString());
        overview.put("endDate", endDate.toString());
        
        return overview;
    }

    /**
     * 获取车间能耗对比数据
     */
    public Map<String, Object> getWorkshopCompare(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> compareList = new ArrayList<>();
        
        QueryWrapper<EnergyData> wrapper = new QueryWrapper<>();
        wrapper.between("record_date", startDate, endDate);
        wrapper.select(
            "workshop_id",
            "SUM(energy) as total_energy",
            "SUM(cost) as total_cost",
            "AVG(power) as avg_power"
        );
        wrapper.groupBy("workshop_id");
        wrapper.orderByDesc("total_energy");
        
        List<Map<String, Object>> stats = energyDataMapper.selectMaps(wrapper);
        
        // 计算最大值用于百分比
        BigDecimal maxEnergy = BigDecimal.ZERO;
        for (Map<String, Object> stat : stats) {
            BigDecimal energy = toBigDecimal(stat.get("total_energy"));
            if (energy.compareTo(maxEnergy) > 0) {
                maxEnergy = energy;
            }
        }
        
        for (Map<String, Object> stat : stats) {
            Map<String, Object> item = new HashMap<>();
            Long wsId = toLong(stat.get("workshop_id"));
            BigDecimal energy = toBigDecimal(stat.get("total_energy"));
            BigDecimal cost = toBigDecimal(stat.get("total_cost"));
            BigDecimal avgPower = toBigDecimal(stat.get("avg_power"));
            
            item.put("workshopId", wsId);
            item.put("workshopName", WORKSHOP_NAMES.getOrDefault(wsId, "车间" + wsId));
            item.put("energy", energy);
            item.put("cost", cost);
            item.put("avgPower", avgPower);
            
            // 计算占比
            if (maxEnergy.compareTo(BigDecimal.ZERO) > 0) {
                item.put("percent", energy.divide(maxEnergy, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP));
            } else {
                item.put("percent", BigDecimal.ZERO);
            }
            
            compareList.add(item);
        }
        
        result.put("workshops", compareList);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        
        return result;
    }

    /**
     * 获取时段用电分析（峰/平/谷占比）
     */
    public Map<String, Object> getPeriodAnalysis(LocalDate startDate, LocalDate endDate, Long workshopId) {
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper<EnergyData> wrapper = new QueryWrapper<>();
        wrapper.between("record_date", startDate, endDate);
        if (workshopId != null) {
            wrapper.eq("workshop_id", workshopId);
        }
        wrapper.select(
            "period_type",
            "SUM(energy) as total_energy",
            "SUM(cost) as total_cost"
        );
        wrapper.groupBy("period_type");
        
        List<Map<String, Object>> stats = energyDataMapper.selectMaps(wrapper);
        
        // 初始化各时段数据
        BigDecimal peakEnergy = BigDecimal.ZERO, peakCost = BigDecimal.ZERO;
        BigDecimal normalEnergy = BigDecimal.ZERO, normalCost = BigDecimal.ZERO;
        BigDecimal valleyEnergy = BigDecimal.ZERO, valleyCost = BigDecimal.ZERO;
        
        for (Map<String, Object> stat : stats) {
            Object periodTypeObj = stat.get("period_type");
            String periodType = periodTypeObj != null ? periodTypeObj.toString() : "";
            BigDecimal energy = toBigDecimal(stat.get("total_energy"));
            BigDecimal cost = toBigDecimal(stat.get("total_cost"));
            
            if ("PEAK".equalsIgnoreCase(periodType)) {
                peakEnergy = energy;
                peakCost = cost;
            } else if ("NORMAL".equalsIgnoreCase(periodType)) {
                normalEnergy = energy;
                normalCost = cost;
            } else if ("VALLEY".equalsIgnoreCase(periodType)) {
                valleyEnergy = energy;
                valleyCost = cost;
            }
        }
        
        BigDecimal totalEnergy = peakEnergy.add(normalEnergy).add(valleyEnergy);
        BigDecimal totalCost = peakCost.add(normalCost).add(valleyCost);
        
        // 峰时数据
        Map<String, Object> peak = new HashMap<>();
        peak.put("type", "PEAK");
        peak.put("label", "峰时");
        peak.put("energy", peakEnergy);
        peak.put("cost", peakCost);
        peak.put("energyPercent", calculatePercent(peakEnergy, totalEnergy));
        peak.put("costPercent", calculatePercent(peakCost, totalCost));
        peak.put("price", new BigDecimal("1.2"));
        peak.put("timeRange", "8:00-12:00, 18:00-22:00");
        
        // 平时数据
        Map<String, Object> normal = new HashMap<>();
        normal.put("type", "NORMAL");
        normal.put("label", "平时");
        normal.put("energy", normalEnergy);
        normal.put("cost", normalCost);
        normal.put("energyPercent", calculatePercent(normalEnergy, totalEnergy));
        normal.put("costPercent", calculatePercent(normalCost, totalCost));
        normal.put("price", new BigDecimal("0.8"));
        normal.put("timeRange", "12:00-18:00");
        
        // 谷时数据
        Map<String, Object> valley = new HashMap<>();
        valley.put("type", "VALLEY");
        valley.put("label", "谷时");
        valley.put("energy", valleyEnergy);
        valley.put("cost", valleyCost);
        valley.put("energyPercent", calculatePercent(valleyEnergy, totalEnergy));
        valley.put("costPercent", calculatePercent(valleyCost, totalCost));
        valley.put("price", new BigDecimal("0.4"));
        valley.put("timeRange", "22:00-8:00");
        
        List<Map<String, Object>> periods = new ArrayList<>();
        periods.add(peak);
        periods.add(normal);
        periods.add(valley);
        
        result.put("periods", periods);
        result.put("totalEnergy", totalEnergy);
        result.put("totalCost", totalCost);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        
        return result;
    }

    /**
     * 获取成本趋势数据
     * @param granularity 粒度：daily（日）、weekly（周）、monthly（月）
     */
    public Map<String, Object> getCostTrend(LocalDate startDate, LocalDate endDate, 
                                             Long workshopId, String granularity) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> trendList = new ArrayList<>();
        
        QueryWrapper<EnergyData> wrapper = new QueryWrapper<>();
        wrapper.between("record_date", startDate, endDate);
        if (workshopId != null) {
            wrapper.eq("workshop_id", workshopId);
        }
        wrapper.select(
            "record_date",
            "SUM(energy) as total_energy",
            "SUM(cost) as total_cost"
        );
        wrapper.groupBy("record_date");
        wrapper.orderByAsc("record_date");
        
        List<Map<String, Object>> dailyStats = energyDataMapper.selectMaps(wrapper);
        
        // 转换为Map便于查找
        Map<LocalDate, Map<String, Object>> dataMap = new HashMap<>();
        for (Map<String, Object> stat : dailyStats) {
            Object dateObj = stat.get("record_date");
            LocalDate date = dateObj instanceof LocalDate ? (LocalDate) dateObj : 
                    LocalDate.parse(dateObj.toString());
            dataMap.put(date, stat);
        }
        
        if ("daily".equals(granularity)) {
            // 日粒度：填充每一天
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                Map<String, Object> item = new HashMap<>();
                item.put("date", date.toString());
                item.put("label", date.getMonthValue() + "-" + date.getDayOfMonth());
                
                Map<String, Object> dayData = dataMap.get(date);
                if (dayData != null) {
                    item.put("energy", toBigDecimal(dayData.get("total_energy")));
                    item.put("cost", toBigDecimal(dayData.get("total_cost")));
                } else {
                    item.put("energy", BigDecimal.ZERO);
                    item.put("cost", BigDecimal.ZERO);
                }
                trendList.add(item);
            }
        } else if ("weekly".equals(granularity)) {
            // 周粒度：按周聚合
            Map<String, Map<String, Object>> weeklyMap = new LinkedHashMap<>();
            WeekFields weekFields = WeekFields.ISO;
            
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                int year = date.getYear();
                int week = date.get(weekFields.weekOfWeekBasedYear());
                String weekKey = year + "-W" + String.format("%02d", week);
                
                Map<String, Object> dayData = dataMap.get(date);
                BigDecimal energy = dayData != null ? toBigDecimal(dayData.get("total_energy")) : BigDecimal.ZERO;
                BigDecimal cost = dayData != null ? toBigDecimal(dayData.get("total_cost")) : BigDecimal.ZERO;
                
                weeklyMap.computeIfAbsent(weekKey, k -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("date", weekKey);
                    m.put("label", "第" + week + "周");
                    m.put("energy", BigDecimal.ZERO);
                    m.put("cost", BigDecimal.ZERO);
                    return m;
                });
                
                Map<String, Object> weekData = weeklyMap.get(weekKey);
                weekData.put("energy", ((BigDecimal) weekData.get("energy")).add(energy));
                weekData.put("cost", ((BigDecimal) weekData.get("cost")).add(cost));
            }
            trendList.addAll(weeklyMap.values());
        } else if ("monthly".equals(granularity)) {
            // 月粒度：按月聚合
            Map<String, Map<String, Object>> monthlyMap = new LinkedHashMap<>();
            
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                String monthKey = date.getYear() + "-" + String.format("%02d", date.getMonthValue());
                
                Map<String, Object> dayData = dataMap.get(date);
                BigDecimal energy = dayData != null ? toBigDecimal(dayData.get("total_energy")) : BigDecimal.ZERO;
                BigDecimal cost = dayData != null ? toBigDecimal(dayData.get("total_cost")) : BigDecimal.ZERO;
                
                final int monthValue = date.getMonthValue();
                monthlyMap.computeIfAbsent(monthKey, k -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("date", k);
                    m.put("label", monthValue + "月");
                    m.put("energy", BigDecimal.ZERO);
                    m.put("cost", BigDecimal.ZERO);
                    return m;
                });
                
                Map<String, Object> monthData = monthlyMap.get(monthKey);
                monthData.put("energy", ((BigDecimal) monthData.get("energy")).add(energy));
                monthData.put("cost", ((BigDecimal) monthData.get("cost")).add(cost));
            }
            trendList.addAll(monthlyMap.values());
        }
        
        result.put("trend", trendList);
        result.put("granularity", granularity);
        result.put("startDate", startDate.toString());
        result.put("endDate", endDate.toString());
        
        return result;
    }

    /**
     * 计算百分比
     */
    private BigDecimal calculatePercent(BigDecimal value, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 安全转换为BigDecimal
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 安全转换为Long
     */
    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
