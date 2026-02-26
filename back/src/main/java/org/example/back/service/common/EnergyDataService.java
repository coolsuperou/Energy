package org.example.back.service.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.back.entity.Application;
import org.example.back.entity.EnergyData;
import org.example.back.entity.User;
import org.example.back.entity.enums.UserRole;
import org.example.back.mapper.workshop.ApplicationMapper;
import org.example.back.mapper.common.EnergyDataMapper;
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
            // 车间用户：优先从部门名称解析车间ID
            Long workshopId = parseWorkshopIdFromDepartment(currentUser.getDepartment());
            if (workshopId != null) {
                return workshopId;
            }
            // 兜底：通过申请记录获取车间ID
            return getUserWorkshopId(currentUser.getId());
        }
        
        // 调度员、经理、管理员：可以查看指定车间或全部
        return requestedWorkshopId;
    }

    /**
     * 从部门名称解析车间ID
     * 例如："第一车间" → 1, "第二车间" → 2
     */
    private Long parseWorkshopIdFromDepartment(String department) {
        if (department == null) return null;
        Map<String, Long> mapping = Map.of(
            "第一车间", 1L, "第二车间", 2L,
            "第三车间", 3L, "第四车间", 4L
        );
        return mapping.get(department);
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
}
