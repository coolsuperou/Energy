package org.example.back.service.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.back.entity.Application;
import org.example.back.entity.EnergyData;
import org.example.back.entity.User;
import org.example.back.entity.enums.UserRole;
import org.example.back.mapper.workshop.ApplicationMapper;
import org.example.back.mapper.EnergyDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EnergyDataService {

    @Autowired
    private EnergyDataMapper energyDataMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    /**
     * 获取今日能耗曲线数据（按小时）
     * 车间用户只能查看自己车间的数据
     */
    public List<EnergyData> getTodayHourlyData(User currentUser, Long workshopId) {
        QueryWrapper<EnergyData> wrapper = new QueryWrapper<>();
        wrapper.eq("record_date", LocalDate.now());
        
        // 权限控制：车间用户只能查看自己车间的数据
        Long targetWorkshopId = getAccessibleWorkshopId(currentUser, workshopId);
        if (targetWorkshopId != null) {
            wrapper.eq("workshop_id", targetWorkshopId);
        }
        
        wrapper.orderByAsc("record_hour");
        return energyDataMapper.selectList(wrapper);
    }

    /**
     * 获取指定日期范围的能耗数据
     */
    public List<EnergyData> getEnergyDataByDateRange(User currentUser, Long workshopId, 
                                                      LocalDate startDate, LocalDate endDate) {
        QueryWrapper<EnergyData> wrapper = new QueryWrapper<>();
        wrapper.between("record_date", startDate, endDate);
        
        // 权限控制
        Long targetWorkshopId = getAccessibleWorkshopId(currentUser, workshopId);
        if (targetWorkshopId != null) {
            wrapper.eq("workshop_id", targetWorkshopId);
        }
        
        wrapper.orderByAsc("record_date", "record_hour");
        return energyDataMapper.selectList(wrapper);
    }

    /**
     * 获取各车间能耗统计
     */
    public Map<String, Object> getWorkshopEnergyStats(User currentUser, LocalDate date) {
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper<EnergyData> wrapper = new QueryWrapper<>();
        wrapper.eq("record_date", date);
        
        // 权限控制
        Long workshopId = getAccessibleWorkshopId(currentUser, null);
        if (workshopId != null) {
            wrapper.eq("workshop_id", workshopId);
        }
        
        wrapper.select("workshop_id", 
                      "SUM(energy) as total_energy", 
                      "SUM(cost) as total_cost",
                      "AVG(power) as avg_power");
        wrapper.groupBy("workshop_id");
        
        List<Map<String, Object>> stats = energyDataMapper.selectMaps(wrapper);
        result.put("stats", stats);
        result.put("date", date);
        
        return result;
    }

    /**
     * 获取用户可访问的车间ID
     * 车间用户：只能访问自己的车间
     * 其他角色：可以访问指定车间或全部车间
     */
    private Long getAccessibleWorkshopId(User currentUser, Long requestedWorkshopId) {
        if (currentUser.getRole() == UserRole.WORKSHOP) {
            // 车间用户：通过申请记录获取车间ID
            return getUserWorkshopId(currentUser.getId());
        }
        
        // 调度员、经理、管理员：可以查看指定车间或全部
        return requestedWorkshopId;
    }

    /**
     * 通过用户的申请记录获取车间ID
     */
    private Long getUserWorkshopId(Long userId) {
        QueryWrapper<Application> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.last("LIMIT 1");
        
        Application application = applicationMapper.selectOne(wrapper);
        return application != null ? application.getWorkshopId() : null;
    }

    /**
     * 获取实时预警数据
     * 基于当前能耗数据分析生成预警
     */
    public Map<String, Object> getAlerts(User currentUser) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> alerts = new java.util.ArrayList<>();
        
        // 获取今日各车间能耗数据
        QueryWrapper<EnergyData> wrapper = new QueryWrapper<>();
        wrapper.eq("record_date", LocalDate.now());
        wrapper.select("workshop_id", 
                      "MAX(power) as max_power",
                      "AVG(power) as avg_power",
                      "SUM(energy) as total_energy");
        wrapper.groupBy("workshop_id");
        
        List<Map<String, Object>> workshopData = energyDataMapper.selectMaps(wrapper);
        
        // 车间限额配置（实际应从配置表读取）
        Map<Long, Integer> workshopLimits = new HashMap<>();
        workshopLimits.put(1L, 800);  // 生产一车间
        workshopLimits.put(2L, 700);  // 生产二车间
        workshopLimits.put(3L, 600);  // 装配车间
        workshopLimits.put(4L, 500);  // 仓储车间
        
        // 车间名称映射
        Map<Long, String> workshopNames = new HashMap<>();
        workshopNames.put(1L, "生产一车间");
        workshopNames.put(2L, "生产二车间");
        workshopNames.put(3L, "装配车间");
        workshopNames.put(4L, "仓储车间");
        
        int criticalCount = 0;
        int warningCount = 0;
        
        // 分析各车间数据生成预警
        for (Map<String, Object> data : workshopData) {
            Long workshopId = (Long) data.get("workshop_id");
            Double maxPower = data.get("max_power") != null ? 
                ((Number) data.get("max_power")).doubleValue() : 0;
            Double avgPower = data.get("avg_power") != null ? 
                ((Number) data.get("avg_power")).doubleValue() : 0;
            
            Integer limit = workshopLimits.getOrDefault(workshopId, 1000);
            String workshopName = workshopNames.getOrDefault(workshopId, "车间" + workshopId);
            
            // 功率超限预警（严重）
            if (maxPower > limit) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("id", "alert-" + workshopId + "-over");
                alert.put("type", "power_over");
                alert.put("level", "critical");
                alert.put("title", workshopName + "功率超限");
                alert.put("description", String.format("当前功率 %.0fkW，超出限额 %dkW", maxPower, limit));
                alert.put("location", workshopName + " - 主配电室");
                alert.put("workshopId", workshopId);
                alert.put("current", maxPower.intValue());
                alert.put("limit", limit);
                alerts.add(alert);
                criticalCount++;
            }
            // 接近限额预警（一般）
            else if (maxPower > limit * 0.85) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("id", "alert-" + workshopId + "-warn");
                alert.put("type", "power_over");
                alert.put("level", "warning");
                alert.put("title", workshopName + "功率接近限额");
                alert.put("description", String.format("当前功率 %.0fkW，已达限额的 %.0f%%", 
                    maxPower, (maxPower / limit) * 100));
                alert.put("location", workshopName + " - 主配电室");
                alert.put("workshopId", workshopId);
                alert.put("current", maxPower.intValue());
                alert.put("limit", limit);
                alerts.add(alert);
                warningCount++;
            }
            
            // 功率波动预警
            if (avgPower > 0 && (maxPower - avgPower) / avgPower > 0.3) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("id", "alert-" + workshopId + "-fluctuation");
                alert.put("type", "power_fluctuation");
                alert.put("level", "warning");
                alert.put("title", workshopName + "用电异常波动");
                alert.put("description", String.format("功率波动幅度 %.0f%%，可能存在设备异常", 
                    ((maxPower - avgPower) / avgPower) * 100));
                alert.put("location", workshopName + " - 配电室");
                alert.put("workshopId", workshopId);
                alerts.add(alert);
                warningCount++;
            }
        }
        
        // 统计信息
        Map<String, Object> stats = new HashMap<>();
        stats.put("critical", criticalCount);
        stats.put("warning", warningCount);
        stats.put("handled", 15);  // 今日已处理数量（可从数据库统计）
        stats.put("handleRate", criticalCount + warningCount > 0 ? 
            (15 * 100 / (15 + criticalCount + warningCount)) : 100);
        
        result.put("alerts", alerts);
        result.put("stats", stats);
        
        return result;
    }
}
