package org.example.back.util;

import org.example.back.entity.enums.TimePeriodType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class EnergyCalculationResult {

    private BigDecimal totalEnergy;
    private BigDecimal totalCost;
    private BigDecimal totalHours;
    private List<PeriodDetail> periodDetails;

    public EnergyCalculationResult() {
        this.totalEnergy = BigDecimal.ZERO;
        this.totalCost = BigDecimal.ZERO;
        this.totalHours = BigDecimal.ZERO;
        this.periodDetails = new ArrayList<>();
    }

    public static class PeriodDetail {
        private TimePeriodType periodType;
        private BigDecimal hours;
        private BigDecimal energy;
        private BigDecimal price;
        private BigDecimal cost;
        private int startHour;
        private int endHour;

        public PeriodDetail(TimePeriodType periodType, BigDecimal hours, BigDecimal energy, 
                          BigDecimal price, BigDecimal cost, int startHour, int endHour) {
            this.periodType = periodType;
            this.hours = hours;
            this.energy = energy;
            this.price = price;
            this.cost = cost;
            this.startHour = startHour;
            this.endHour = endHour;
        }

        public TimePeriodType getPeriodType() { return periodType; }
        public BigDecimal getHours() { return hours; }
        public BigDecimal getEnergy() { return energy; }
        public BigDecimal getPrice() { return price; }
        public BigDecimal getCost() { return cost; }
        public int getStartHour() { return startHour; }
        public int getEndHour() { return endHour; }
    }

    public void addPeriodDetail(PeriodDetail detail) {
        this.periodDetails.add(detail);
        this.totalEnergy = this.totalEnergy.add(detail.getEnergy());
        this.totalCost = this.totalCost.add(detail.getCost());
        this.totalHours = this.totalHours.add(detail.getHours());
    }

    public BigDecimal getTotalEnergy() { return totalEnergy; }
    public void setTotalEnergy(BigDecimal totalEnergy) { this.totalEnergy = totalEnergy; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public BigDecimal getTotalHours() { return totalHours; }
    public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }
    public List<PeriodDetail> getPeriodDetails() { return periodDetails; }
    public void setPeriodDetails(List<PeriodDetail> periodDetails) { this.periodDetails = periodDetails; }
}
