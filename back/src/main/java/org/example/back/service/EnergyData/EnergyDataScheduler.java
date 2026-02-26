package org.example.back.service.energyData;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.back.entity.Application;
import org.example.back.entity.enums.ApplicationStatus;
import org.example.back.mapper.workshop.ApplicationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 能耗数据定时生成器
 * 每分钟检查已批准的申请，为当前小时生成能耗数据
 * 
 * @author 每天十点睡
 */
@Slf4j
@Component
public class EnergyDataScheduler {

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private EnergyDataGenerator energyDataGenerator;

    /**
     * 每分钟执行一次，为正在执行中的已批准申请生成当前小时的能耗数据
     * generateSingleHourData 内部会检查是否已存在，避免重复
     */
    @Scheduled(fixedRate = 60000)
    public void generateCurrentHourEnergyData() {
        LocalDate today = LocalDate.now();
        int currentHour = LocalTime.now().getHour();

        // 查询今天已批准的申请
        LambdaQueryWrapper<Application> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Application::getApplyDate, today)
               .eq(Application::getStatus, ApplicationStatus.APPROVED);
        List<Application> applications = applicationMapper.selectList(wrapper);

        for (Application app : applications) {
            LocalTime startTime = app.getAdjustedStartTime() != null 
                    ? app.getAdjustedStartTime() : app.getStartTime();
            LocalTime endTime = app.getAdjustedEndTime() != null 
                    ? app.getAdjustedEndTime() : app.getEndTime();

            int startHour = startTime.getHour();
            int endHour = endTime.getHour();

            // 判断当前小时是否在申请时段内
            boolean inRange;
            if (endTime.isAfter(startTime)) {
                inRange = currentHour >= startHour && currentHour < endHour;
            } else {
                // 跨天
                inRange = currentHour >= startHour || currentHour < endHour;
            }

            if (inRange) {
                try {
                    energyDataGenerator.generateSingleHourData(app, today, currentHour);
                } catch (Exception e) {
                    log.error("生成能耗数据失败: applicationId={}, hour={}", app.getId(), currentHour, e);
                }
            }
        }
    }
}
