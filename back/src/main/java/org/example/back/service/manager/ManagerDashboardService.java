package org.example.back.service.manager;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.back.entity.Application;
import org.example.back.entity.EnergyData;
import org.example.back.entity.SkillCertification;
import org.example.back.entity.enums.ApplicationStatus;
import org.example.back.entity.enums.CertificationStatus;
import org.example.back.mapper.common.EnergyDataMapper;
import org.example.back.mapper.inspector.SkillCertificationMapper;
import org.example.back.mapper.workshop.ApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * 经理首页概览服务
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	经理首页概览数据统计逻辑
 * -- =============================================
 */
@Service
public class ManagerDashboardService {

    @Autowired
    private EnergyDataMapper energyDataMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private SkillCertificationMapper skillCertificationMapper;

    // 车间名称映射
    private static final Map<Long, String> WORKSHOP_NAMES = new HashMap<>();
    static {
        WORKSHOP_NAMES.put(1L, "生产一车间");
        WORKSHOP_NAMES.put(2L, "生产二车间");
        WORKSHOP_NAMES.put(3L, "装配车间");
        WORKSHOP_NAMES.put(4L, "仓储车间");
    }

    /**
     * 获取经理首页概览数据
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> result = new HashMap<>();
        
        // 1. 全厂用电指标
        result.put("energyIndicators", getEnergyIndicators());
        
        // 2. 车间用电排名
        result.put("workshopRanking", getWorkshopRanking());
        
        // 3. 待处理事项
        result.put("pendingItems", getPendingItems());
        
        // 4. 用电趋势数据（近7天）
        result.put("energyTrend", getEnergyTrend(7));
        
        return result;
    }

    /**
     * 获取全厂用电指标
     */
    private Map<String, Object> getEnergyIndicators() {
        Map<String, Object> indicators = new HashMap<>();
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        YearMonth lastMonth = currentMonth.minusMonths(1);
        
        // 今日用电量和费用
        QueryWrapper<EnergyData> todayWrapper = new QueryWrapper<>();
        todayWrapper.eq("record_date", today);
        todayWrapper.select("SUM(energy) as total_energy", "SUM(cost) as total_cost");
        Map<String, Object> todayData = energyDataMapper.selectMaps(todayWrapper).stream()
                .filter(Objects::nonNull).findFirst().orElse(new HashMap<>());
        
        BigDecimal todayEnergy = toBigDecimal(todayData.get("total_energy"));
        BigDecimal todayCost = toBigDecimal(todayData.get("total_cost"));
        
        indicators.put("todayEnergy", todayEnergy);
        indicators.put("todayCost", todayCost);
        
        // 本月用电量和费用
        QueryWrapper<EnergyData> monthWrapper = new QueryWrapper<>();
        monthWrapper.between("record_date", currentMonth.atDay(1), today);
        monthWrapper.select("SUM(energy) as total_energy", "SUM(cost) as total_cost");
        Map<String, Object> monthData = energyDataMapper.selectMaps(monthWrapper).stream()
                .filter(Objects::nonNull).findFirst().orElse(new HashMap<>());
        
        BigDecimal monthEnergy = toBigDecimal(monthData.get("total_energy"));
        BigDecimal monthCost = toBigDecimal(monthData.get("total_cost"));
        
        indicators.put("monthEnergy", monthEnergy);
        indicators.put("monthCost", monthCost);
        
        // 上月同期数据（用于计算同比）
        int dayOfMonth = today.getDayOfMonth();
        LocalDate lastMonthSameDay = lastMonth.atDay(Math.min(dayOfMonth, lastMonth.lengthOfMonth()));
        
        QueryWrapper<EnergyData> lastMonthWrapper = new QueryWrapper<>();
        lastMonthWrapper.between("record_date", lastMonth.atDay(1), lastMonthSameDay);
        lastMonthWrapper.select("SUM(energy) as total_energy", "SUM(cost) as total_cost");
        Map<String, Object> lastMonthData = energyDataMapper.selectMaps(lastMonthWrapper).stream()
                .filter(Objects::nonNull).findFirst().orElse(new HashMap<>());
        
        BigDecimal lastMonthEnergy = toBigDecimal(lastMonthData.get("total_energy"));
        BigDecimal lastMonthCost = toBigDecimal(lastMonthData.get("total_cost"));
        
        // 计算同比变化率
        indicators.put("energyChangeRate", calculateChangeRate(monthEnergy, lastMonthEnergy));
        indicators.put("costChangeRate", calculateChangeRate(monthCost, lastMonthCost));
        
        // 昨日数据（用于计算环比）
        LocalDate yesterday = today.minusDays(1);
        QueryWrapper<EnergyData> yesterdayWrapper = new QueryWrapper<>();
        yesterdayWrapper.eq("record_date", yesterday);
        yesterdayWrapper.select("SUM(energy) as total_energy");
        Map<String, Object> yesterdayData = energyDataMapper.selectMaps(yesterdayWrapper).stream()
                .filter(Objects::nonNull).findFirst().orElse(new HashMap<>());
        
        BigDecimal yesterdayEnergy = toBigDecimal(yesterdayData.get("total_energy"));
        indicators.put("dailyChangeRate", calculateChangeRate(todayEnergy, yesterdayEnergy));
        
        return indicators;
    }

    /**
     * 获取车间用电排名
     */
    private List<Map<String, Object>> getWorkshopRanking() {
        List<Map<String, Object>> ranking = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        // 今日各车间能耗统计
        QueryWrapper<EnergyData> wrapper = new QueryWrapper<>();
        wrapper.eq("record_date", today);
        wrapper.select("workshop_id", "SUM(energy) as total_energy", "SUM(cost) as total_cost");
        wrapper.groupBy("workshop_id");
        wrapper.orderByDesc("total_energy");
        
        List<Map<String, Object>> stats = energyDataMapper.selectMaps(wrapper);
        
        for (Map<String, Object> stat : stats) {
            if (stat == null) continue;
            Map<String, Object> item = new HashMap<>();
            Long workshopId = toLong(stat.get("workshop_id"));
            item.put("workshopId", workshopId);
            item.put("workshopName", WORKSHOP_NAMES.getOrDefault(workshopId, "车间" + workshopId));
            item.put("energy", toBigDecimal(stat.get("total_energy")));
            item.put("cost", toBigDecimal(stat.get("total_cost")));
            ranking.add(item);
        }
        
        return ranking;
    }

    /**
     * 获取待处理事项
     */
    private Map<String, Object> getPendingItems() {
        Map<String, Object> items = new HashMap<>();
        
        // 待审批申请数
        LambdaQueryWrapper<Application> appWrapper = new LambdaQueryWrapper<>();
        appWrapper.eq(Application::getStatus, ApplicationStatus.PENDING);
        long pendingApplications = applicationMapper.selectCount(appWrapper);
        items.put("pendingApplications", pendingApplications);
        
        // 待审核技能认证数
        LambdaQueryWrapper<SkillCertification> skillWrapper = new LambdaQueryWrapper<>();
        skillWrapper.eq(SkillCertification::getStatus, CertificationStatus.PENDING);
        long pendingCertifications = skillCertificationMapper.selectCount(skillWrapper);
        items.put("pendingCertifications", pendingCertifications);
        
        // 总待处理数
        items.put("total", pendingApplications + pendingCertifications);
        
        return items;
    }

    /**
     * 获取用电趋势数据
     */
    private List<Map<String, Object>> getEnergyTrend(int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);
        
        // 查询日期范围内的能耗数据
        QueryWrapper<EnergyData> wrapper = new QueryWrapper<>();
        wrapper.between("record_date", startDate, today);
        wrapper.select("record_date", "SUM(energy) as total_energy", "SUM(cost) as total_cost");
        wrapper.groupBy("record_date");
        wrapper.orderByAsc("record_date");
        
        List<Map<String, Object>> stats = energyDataMapper.selectMaps(wrapper);
        
        // 转换为Map便于查找
        Map<LocalDate, Map<String, Object>> dataMap = new HashMap<>();
        for (Map<String, Object> stat : stats) {
            if (stat == null) continue;
            Object dateObj = stat.get("record_date");
            LocalDate date = dateObj instanceof LocalDate ? (LocalDate) dateObj : 
                    LocalDate.parse(dateObj.toString());
            dataMap.put(date, stat);
        }
        
        // 填充每一天的数据（包括无数据的日期）
        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            
            Map<String, Object> dayData = dataMap.get(date);
            if (dayData != null) {
                item.put("energy", toBigDecimal(dayData.get("total_energy")));
                item.put("cost", toBigDecimal(dayData.get("total_cost")));
            } else {
                item.put("energy", BigDecimal.ZERO);
                item.put("cost", BigDecimal.ZERO);
            }
            
            trend.add(item);
        }
        
        return trend;
    }

    /**
     * 计算变化率
     */
    private BigDecimal calculateChangeRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
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
