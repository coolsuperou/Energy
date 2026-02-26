package org.example.back.controller.workshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.Application;
import org.example.back.entity.EnergyData;
import org.example.back.entity.User;
import org.example.back.entity.enums.TimePeriodType;
import org.example.back.mapper.common.EnergyDataMapper;
import org.example.back.mapper.workshop.ApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 车间能耗查看控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	车间用户查看本车间能耗数据接口
 * -- =============================================
 */
@RestController
@RequestMapping("/energy-data")
public class WorkshopEnergyController {

    @Autowired
    private EnergyDataMapper energyDataMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    /**
     * 获取本车间能耗数据
     * 支持日期范围查询，返回按小时的能耗记录
     * 
     * @param startDate 开始日期（可选，默认今天）
     * @param endDate 结束日期（可选，默认今天）
     * @param session HTTP会话
     * @return 能耗数据列表和费用统计
     */
    @GetMapping("/my-workshop")
    public Result<Map<String, Object>> getMyWorkshopEnergyData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        // 获取用户所属车间ID
        Long workshopId = getUserWorkshopId(user);
        if (workshopId == null) {
            return Result.error("未找到您所属的车间信息");
        }

        // 默认查询今天的数据
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // 查询能耗数据
        LambdaQueryWrapper<EnergyData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyData::getWorkshopId, workshopId);
        wrapper.between(EnergyData::getRecordDate, startDate, endDate);
        wrapper.orderByAsc(EnergyData::getRecordDate, EnergyData::getRecordHour);

        List<EnergyData> energyDataList = energyDataMapper.selectList(wrapper);

        // 计算费用统计（按峰/平/谷分类汇总）
        Map<String, Object> costStats = calculateCostStats(energyDataList);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("energyData", energyDataList);
        result.put("costStats", costStats);
        result.put("workshopId", workshopId);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        return Result.success(result);
    }

    /**
     * 获取用户所属车间ID
     * 优先通过用户部门名称解析车间ID，其次通过申请记录获取
     */
    private Long getUserWorkshopId(User user) {
        // 优先从部门名称解析车间ID（如"第一车间" → 1）
        Long workshopId = parseWorkshopIdFromDepartment(user.getDepartment());
        if (workshopId != null) {
            return workshopId;
        }
        // 兜底：从申请记录获取
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Application::getUserId, user.getId());
        wrapper.last("LIMIT 1");
        Application application = applicationMapper.selectOne(wrapper);
        return application != null ? application.getWorkshopId() : null;
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
     * 计算费用统计（按峰/平/谷分类汇总）
     */
    private Map<String, Object> calculateCostStats(List<EnergyData> energyDataList) {
        Map<String, Object> stats = new HashMap<>();

        // 按时段类型分组统计
        Map<TimePeriodType, List<EnergyData>> groupedByPeriod = energyDataList.stream()
                .collect(Collectors.groupingBy(EnergyData::getPeriodType));

        // 峰时统计
        BigDecimal peakEnergy = BigDecimal.ZERO;
        BigDecimal peakCost = BigDecimal.ZERO;
        if (groupedByPeriod.containsKey(TimePeriodType.PEAK)) {
            for (EnergyData data : groupedByPeriod.get(TimePeriodType.PEAK)) {
                if (data.getEnergy() != null) {
                    peakEnergy = peakEnergy.add(data.getEnergy());
                }
                if (data.getCost() != null) {
                    peakCost = peakCost.add(data.getCost());
                }
            }
        }
        stats.put("peakEnergy", peakEnergy);
        stats.put("peakCost", peakCost);

        // 平时统计
        BigDecimal normalEnergy = BigDecimal.ZERO;
        BigDecimal normalCost = BigDecimal.ZERO;
        if (groupedByPeriod.containsKey(TimePeriodType.NORMAL)) {
            for (EnergyData data : groupedByPeriod.get(TimePeriodType.NORMAL)) {
                if (data.getEnergy() != null) {
                    normalEnergy = normalEnergy.add(data.getEnergy());
                }
                if (data.getCost() != null) {
                    normalCost = normalCost.add(data.getCost());
                }
            }
        }
        stats.put("normalEnergy", normalEnergy);
        stats.put("normalCost", normalCost);

        // 谷时统计
        BigDecimal valleyEnergy = BigDecimal.ZERO;
        BigDecimal valleyCost = BigDecimal.ZERO;
        if (groupedByPeriod.containsKey(TimePeriodType.VALLEY)) {
            for (EnergyData data : groupedByPeriod.get(TimePeriodType.VALLEY)) {
                if (data.getEnergy() != null) {
                    valleyEnergy = valleyEnergy.add(data.getEnergy());
                }
                if (data.getCost() != null) {
                    valleyCost = valleyCost.add(data.getCost());
                }
            }
        }
        stats.put("valleyEnergy", valleyEnergy);
        stats.put("valleyCost", valleyCost);

        // 总计
        BigDecimal totalEnergy = peakEnergy.add(normalEnergy).add(valleyEnergy);
        BigDecimal totalCost = peakCost.add(normalCost).add(valleyCost);
        stats.put("totalEnergy", totalEnergy);
        stats.put("totalCost", totalCost);

        return stats;
    }
}
