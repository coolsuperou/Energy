package org.example.back.controller.workshop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.entity.Application;
import org.example.back.entity.EnergyData;
import org.example.back.entity.User;
import org.example.back.mapper.common.EnergyDataMapper;
import org.example.back.mapper.workshop.ApplicationMapper;
import org.example.back.service.common.NotificationService;
import org.example.back.service.workshop.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 车间用户首页控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	车间用户首页概览相关接口
 * -- =============================================
 */
@RestController
@RequestMapping("/workshop")
public class WorkshopController {

    @Autowired
    private EnergyDataMapper energyDataMapper;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取车间首页概览数据
     * 返回今日用电量、本月用电量、最近申请状态、未读消息数
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        Map<String, Object> dashboard = new HashMap<>();

        // 获取用户所属车间ID（通过部门名称解析）
        Long workshopId = getUserWorkshopId(user);

        // 1. 今日用电量
        BigDecimal todayEnergy = getTodayEnergy(workshopId);
        dashboard.put("todayEnergy", todayEnergy);

        // 2. 本月用电量
        BigDecimal monthEnergy = getMonthEnergy(workshopId);
        dashboard.put("monthEnergy", monthEnergy);

        // 3. 最近申请状态（最近5条）
        List<Application> recentApplications = getRecentApplications(user.getId(), 5);
        dashboard.put("recentApplications", recentApplications);

        // 4. 未读消息数
        int unreadCount = notificationService.getUnreadCount(user.getId());
        dashboard.put("unreadCount", unreadCount);

        // 5. 今日按小时能耗数据（用于功率曲线）
        List<EnergyData> todayHourlyData = getTodayHourlyData(workshopId);
        dashboard.put("todayHourlyData", todayHourlyData);

        return Result.success(dashboard);
    }

    /**
     * 获取用户所属车间ID
     * 优先通过部门名称解析，其次通过申请记录获取
     */
    private Long getUserWorkshopId(User user) {
        Long workshopId = parseWorkshopIdFromDepartment(user.getDepartment());
        if (workshopId != null) {
            return workshopId;
        }
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Application::getUserId, user.getId());
        wrapper.last("LIMIT 1");
        Application application = applicationMapper.selectOne(wrapper);
        return application != null ? application.getWorkshopId() : null;
    }

    /**
     * 从部门名称解析车间ID
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
     * 获取今日用电量
     */
    private BigDecimal getTodayEnergy(Long workshopId) {
        LambdaQueryWrapper<EnergyData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyData::getRecordDate, LocalDate.now());
        if (workshopId != null) {
            wrapper.eq(EnergyData::getWorkshopId, workshopId);
        }
        wrapper.select(EnergyData::getEnergy);
        
        List<EnergyData> dataList = energyDataMapper.selectList(wrapper);
        return dataList.stream()
                .map(EnergyData::getEnergy)
                .filter(e -> e != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取本月用电量
     */
    private BigDecimal getMonthEnergy(Long workshopId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();

        LambdaQueryWrapper<EnergyData> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(EnergyData::getRecordDate, startDate, endDate);
        if (workshopId != null) {
            wrapper.eq(EnergyData::getWorkshopId, workshopId);
        }
        wrapper.select(EnergyData::getEnergy);
        
        List<EnergyData> dataList = energyDataMapper.selectList(wrapper);
        return dataList.stream()
                .map(EnergyData::getEnergy)
                .filter(e -> e != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取最近申请列表
     */
    private List<Application> getRecentApplications(Long userId, int limit) {
        IPage<Application> page = applicationService.getUserApplications(userId, 1, limit, null);
        return page.getRecords();
    }

    /**
     * 获取今日按小时能耗数据（用于功率曲线图）
     */
    private List<EnergyData> getTodayHourlyData(Long workshopId) {
        LambdaQueryWrapper<EnergyData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EnergyData::getRecordDate, LocalDate.now());
        if (workshopId != null) {
            wrapper.eq(EnergyData::getWorkshopId, workshopId);
        }
        wrapper.orderByAsc(EnergyData::getRecordHour);
        return energyDataMapper.selectList(wrapper);
    }
}
