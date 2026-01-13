package org.example.back.service;

import org.example.back.entity.Application;
import org.example.back.entity.EnergyData;
import org.example.back.entity.enums.TimePeriodType;
import org.example.back.mapper.EnergyDataMapper;
import org.example.back.util.EnergyCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EnergyDataGenerator {

    @Autowired
    private EnergyDataMapper energyDataMapper;

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

        int startHour = startTime.getHour();
        int endHour = endTime.getHour();
        int startMinute = startTime.getMinute();
        int endMinute = endTime.getMinute();

        boolean crossMidnight = endTime.isBefore(startTime) || endTime.equals(startTime);
        
        if (crossMidnight) {
            generateHourlyData(energyDataList, application, applyDate, startHour, startMinute, 24, 0, power);
            generateHourlyData(energyDataList, application, applyDate.plusDays(1), 0, 0, endHour, endMinute, power);
        } else {
            generateHourlyData(energyDataList, application, applyDate, startHour, startMinute, endHour, endMinute, power);
        }

        for (EnergyData data : energyDataList) {
            energyDataMapper.insert(data);
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
