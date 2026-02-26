package org.example.back.service.energyData;

import lombok.extern.slf4j.Slf4j;
import org.example.back.entity.Application;
import org.example.back.entity.EnergyData;
import org.example.back.entity.enums.TimePeriodType;
import org.example.back.mapper.common.EnergyDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 能耗数据生成器
 * 为已批准的用电申请自动生成能耗数据
 * 
 * @author 每天十点睡
 */
@Slf4j
@Service
public class EnergyDataGenerator {

    @Autowired
    private EnergyDataMapper energyDataMapper;
    
    @Autowired
    private QuotaService quotaService;

    @Transactional
    public List<EnergyData> generateEnergyData(Application application) {
        List<EnergyData> energyDataList = new ArrayList<>();

        LocalTime startTime = application.getAdjustedStartTime() != null 
                ? application.getAdjustedStartTime() 
                : application.getStartTime();
        LocalTime endTime = application.getAdjustedEndTime() != null 
                ? application.getAdjustedEndTime() 
                : application.getEndTime();
        
        BigDecimal power = application.getPower();
        LocalDate applyDate = application.getApplyDate();
        LocalDate today = LocalDate.now();
        int currentHour = LocalTime.now().getHour();

        int startHour = startTime.getHour();
        int startMinute = startTime.getMinute();
        int endHour = endTime.getHour();
        int endMinute = endTime.getMinute();

        // 计算实际截止小时：只生成到当前时间为止
        // 如果申请日期是今天，截止到 min(endHour, currentHour+1)
        // 如果申请日期是过去，正常生成全部
        // 如果申请日期是未来，不生成任何数据
        boolean crossMidnight = endTime.isBefore(startTime) || endTime.equals(startTime);
        
        if (crossMidnight) {
            // 跨天情况：第一段 startHour~24:00，第二段 0:00~endHour
            int firstEndHour = 24;
            int firstEndMinute = 0;
            int secondEndHour = endHour;
            int secondEndMinute = endMinute;
            
            if (applyDate.equals(today)) {
                // 今天：第一段截止到当前小时
                firstEndHour = Math.min(24, currentHour + 1);
                firstEndMinute = 0;
                // 第二段（明天的）不生成
                secondEndHour = 0;
                secondEndMinute = 0;
            } else if (applyDate.isAfter(today)) {
                return energyDataList; // 未来日期不生成
            } else if (applyDate.plusDays(1).equals(today)) {
                // 第一段（昨天的）全部生成
                // 第二段（今天的）截止到当前小时
                secondEndHour = Math.min(endHour, currentHour + 1);
                if (secondEndHour >= endHour) {
                    secondEndHour = endHour;
                    secondEndMinute = endMinute;
                } else {
                    secondEndMinute = 0;
                }
            }
            // 更早的日期：全部生成（firstEndHour/secondEndHour保持原值）
            
            if (firstEndHour > startHour) {
                generateHourlyData(energyDataList, application, applyDate, startHour, startMinute, firstEndHour, firstEndMinute, power);
            }
            if (secondEndHour > 0) {
                generateHourlyData(energyDataList, application, applyDate.plusDays(1), 0, 0, secondEndHour, secondEndMinute, power);
            }
        } else {
            // 不跨天
            int actualEndHour = endHour;
            int actualEndMinute = endMinute;
            
            if (applyDate.equals(today)) {
                actualEndHour = Math.min(endHour, currentHour + 1);
                if (actualEndHour >= endHour) {
                    actualEndHour = endHour;
                    actualEndMinute = endMinute;
                } else {
                    actualEndMinute = 0;
                }
                if (actualEndHour <= startHour) {
                    return energyDataList; // 还没到开始时间
                }
            } else if (applyDate.isAfter(today)) {
                return energyDataList; // 未来日期不生成
            }
            
            generateHourlyData(energyDataList, application, applyDate, startHour, startMinute, actualEndHour, actualEndMinute, power);
        }

        // 按日期汇总能耗，用于更新配额
        Map<LocalDate, BigDecimal> dailyEnergy = new HashMap<>();
        
        for (EnergyData data : energyDataList) {
            energyDataMapper.insert(data);
            dailyEnergy.merge(data.getRecordDate(), data.getEnergy(), BigDecimal::add);
        }
        
        // 更新车间配额
        for (Map.Entry<LocalDate, BigDecimal> entry : dailyEnergy.entrySet()) {
            quotaService.updateUsedQuota(application.getWorkshopId(), entry.getValue(), entry.getKey());
            log.info("更新车间配额: workshopId={}, date={}, energy={}kWh", 
                    application.getWorkshopId(), entry.getKey(), entry.getValue());
        }

        return energyDataList;
    }

    private void generateHourlyData(List<EnergyData> list, Application application,
            LocalDate date, int startHour, int startMinute, int endHour, int endMinute, BigDecimal power) {

        for (int hour = startHour; hour < endHour || (hour == startHour && endHour == startHour); hour++) {
            if (hour >= 24) break;
            
            BigDecimal hours;
            
            if (hour == startHour && startMinute > 0) {
                hours = new BigDecimal(60 - startMinute).divide(new BigDecimal("60"), 4, RoundingMode.HALF_UP);
            } else if (hour == endHour - 1 && endMinute > 0 && endMinute < 60) {
                hours = BigDecimal.ONE;
            } else if (hour == endHour && endMinute > 0) {
                hours = new BigDecimal(endMinute).divide(new BigDecimal("60"), 4, RoundingMode.HALF_UP);
            } else {
                hours = BigDecimal.ONE;
            }

            if (hours.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal fluctuationRate = EnergyCalculator.generateFluctuationRate();
            BigDecimal actualPower = power.multiply(fluctuationRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal energy = actualPower.multiply(hours).setScale(2, RoundingMode.HALF_UP);
            TimePeriodType periodType = EnergyCalculator.getTimePeriodType(hour);
            BigDecimal price = EnergyCalculator.getPrice(periodType);
            BigDecimal cost = energy.multiply(price).setScale(2, RoundingMode.HALF_UP);

            EnergyData energyData = new EnergyData();
            energyData.setApplicationId(application.getId());
            energyData.setWorkshopId(application.getWorkshopId());
            energyData.setEquipmentId(application.getEquipmentId());
            energyData.setRecordDate(date);
            energyData.setRecordHour(hour);
            energyData.setPower(actualPower);
            energyData.setEnergy(energy);
            energyData.setPeriodType(periodType);
            energyData.setPrice(price);
            energyData.setCost(cost);

            list.add(energyData);
        }

        if (endMinute > 0 && endHour < 24 && endHour > startHour) {
            BigDecimal hours = new BigDecimal(endMinute).divide(new BigDecimal("60"), 4, RoundingMode.HALF_UP);
            BigDecimal fluctuationRate = EnergyCalculator.generateFluctuationRate();
            BigDecimal actualPower = power.multiply(fluctuationRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal energy = actualPower.multiply(hours).setScale(2, RoundingMode.HALF_UP);
            TimePeriodType periodType = EnergyCalculator.getTimePeriodType(endHour);
            BigDecimal price = EnergyCalculator.getPrice(periodType);
            BigDecimal cost = energy.multiply(price).setScale(2, RoundingMode.HALF_UP);

            EnergyData energyData = new EnergyData();
            energyData.setApplicationId(application.getId());
            energyData.setWorkshopId(application.getWorkshopId());
            energyData.setEquipmentId(application.getEquipmentId());
            energyData.setRecordDate(date);
            energyData.setRecordHour(endHour);
            energyData.setPower(actualPower);
            energyData.setEnergy(energy);
            energyData.setPeriodType(periodType);
            energyData.setPrice(price);
            energyData.setCost(cost);

            list.add(energyData);
        }
    }

    /**
     * 为指定申请生成某一小时的能耗数据（供定时任务调用）
     * 会先检查该小时是否已有数据，避免重复生成
     */
    @Transactional
    public EnergyData generateSingleHourData(Application application, LocalDate date, int hour) {
        // 检查是否已存在该小时的数据
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EnergyData> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(EnergyData::getApplicationId, application.getId())
               .eq(EnergyData::getRecordDate, date)
               .eq(EnergyData::getRecordHour, hour);
        if (energyDataMapper.selectCount(wrapper) > 0) {
            return null; // 已存在，跳过
        }

        BigDecimal power = application.getPower();
        BigDecimal fluctuationRate = EnergyCalculator.generateFluctuationRate();
        BigDecimal actualPower = power.multiply(fluctuationRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal energy = actualPower.multiply(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
        TimePeriodType periodType = EnergyCalculator.getTimePeriodType(hour);
        BigDecimal price = EnergyCalculator.getPrice(periodType);
        BigDecimal cost = energy.multiply(price).setScale(2, RoundingMode.HALF_UP);

        EnergyData energyData = new EnergyData();
        energyData.setApplicationId(application.getId());
        energyData.setWorkshopId(application.getWorkshopId());
        energyData.setEquipmentId(application.getEquipmentId());
        energyData.setRecordDate(date);
        energyData.setRecordHour(hour);
        energyData.setPower(actualPower);
        energyData.setEnergy(energy);
        energyData.setPeriodType(periodType);
        energyData.setPrice(price);
        energyData.setCost(cost);

        energyDataMapper.insert(energyData);
        quotaService.updateUsedQuota(application.getWorkshopId(), energy, date);
        log.info("定时生成能耗数据: applicationId={}, date={}, hour={}, energy={}kWh", 
                application.getId(), date, hour, energy);
        return energyData;
    }

    public BigDecimal[] generatePowerCurve(List<Application> applications, LocalDate date) {
        BigDecimal[] powerCurve = new BigDecimal[24];
        
        for (int i = 0; i < 24; i++) {
            powerCurve[i] = BigDecimal.ZERO;
        }

        for (Application app : applications) {
            LocalTime startTime = app.getAdjustedStartTime() != null 
                    ? app.getAdjustedStartTime() 
                    : app.getStartTime();
            LocalTime endTime = app.getAdjustedEndTime() != null 
                    ? app.getAdjustedEndTime() 
                    : app.getEndTime();

            int startHour = startTime.getHour();
            int endHour = endTime.getHour();
            
            if (endTime.isAfter(startTime)) {
                for (int hour = startHour; hour < endHour; hour++) {
                    powerCurve[hour] = powerCurve[hour].add(app.getPower());
                }
                if (endTime.getMinute() > 0) {
                    powerCurve[endHour] = powerCurve[endHour].add(app.getPower());
                }
            }
        }

        return powerCurve;
    }
}
