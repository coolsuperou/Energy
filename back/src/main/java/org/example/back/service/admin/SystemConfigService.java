package org.example.back.service.admin;

import org.example.back.dto.SystemConfigDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	系统配置管理逻辑，使用内存存储配置参数
 * -- =============================================
 */
@Service
public class SystemConfigService {

    /** 配置存储（使用ConcurrentHashMap保证线程安全） */
    private final ConcurrentHashMap<String, Object> configStore = new ConcurrentHashMap<>();

    // 配置键常量
    private static final String KEY_PEAK_PRICE = "peakPrice";
    private static final String KEY_NORMAL_PRICE = "normalPrice";
    private static final String KEY_VALLEY_PRICE = "valleyPrice";
    private static final String KEY_POWER_OVERLOAD_THRESHOLD = "powerOverloadThreshold";
    private static final String KEY_POWER_WARNING_THRESHOLD = "powerWarningThreshold";
    private static final String KEY_QUOTA_WARNING_THRESHOLD = "quotaWarningThreshold";
    private static final String KEY_PEAK_START_HOUR1 = "peakStartHour1";
    private static final String KEY_PEAK_END_HOUR1 = "peakEndHour1";
    private static final String KEY_PEAK_START_HOUR2 = "peakStartHour2";
    private static final String KEY_PEAK_END_HOUR2 = "peakEndHour2";
    private static final String KEY_NORMAL_START_HOUR = "normalStartHour";
    private static final String KEY_NORMAL_END_HOUR = "normalEndHour";

    /**
     * 构造函数，初始化默认配置
     */
    public SystemConfigService() {
        initDefaultConfig();
    }

    /**
     * 初始化默认配置
     * 根据需求文档：
     * - 峰时(peak): 8-12点, 18-22点 → 1.2元/kWh
     * - 平时(normal): 12-18点 → 0.8元/kWh
     * - 谷时(valley): 22-8点 → 0.4元/kWh
     */
    private void initDefaultConfig() {
        // 电价参数默认值
        configStore.put(KEY_PEAK_PRICE, new BigDecimal("1.20"));
        configStore.put(KEY_NORMAL_PRICE, new BigDecimal("0.80"));
        configStore.put(KEY_VALLEY_PRICE, new BigDecimal("0.40"));
        
        // 预警阈值默认值
        configStore.put(KEY_POWER_OVERLOAD_THRESHOLD, 100);  // 100%超限
        configStore.put(KEY_POWER_WARNING_THRESHOLD, 85);    // 85%预警
        configStore.put(KEY_QUOTA_WARNING_THRESHOLD, 80);    // 80%配额预警
        
        // 时段配置默认值
        configStore.put(KEY_PEAK_START_HOUR1, 8);   // 峰时1: 8-12点
        configStore.put(KEY_PEAK_END_HOUR1, 12);
        configStore.put(KEY_PEAK_START_HOUR2, 18);  // 峰时2: 18-22点
        configStore.put(KEY_PEAK_END_HOUR2, 22);
        configStore.put(KEY_NORMAL_START_HOUR, 12); // 平时: 12-18点
        configStore.put(KEY_NORMAL_END_HOUR, 18);
    }

    /**
     * 获取系统配置
     * 
     * @return 系统配置DTO
     */
    public SystemConfigDTO getConfig() {
        SystemConfigDTO config = new SystemConfigDTO();
        
        // 电价参数
        config.setPeakPrice((BigDecimal) configStore.get(KEY_PEAK_PRICE));
        config.setNormalPrice((BigDecimal) configStore.get(KEY_NORMAL_PRICE));
        config.setValleyPrice((BigDecimal) configStore.get(KEY_VALLEY_PRICE));
        
        // 预警阈值
        config.setPowerOverloadThreshold((Integer) configStore.get(KEY_POWER_OVERLOAD_THRESHOLD));
        config.setPowerWarningThreshold((Integer) configStore.get(KEY_POWER_WARNING_THRESHOLD));
        config.setQuotaWarningThreshold((Integer) configStore.get(KEY_QUOTA_WARNING_THRESHOLD));
        
        // 时段配置
        config.setPeakStartHour1((Integer) configStore.get(KEY_PEAK_START_HOUR1));
        config.setPeakEndHour1((Integer) configStore.get(KEY_PEAK_END_HOUR1));
        config.setPeakStartHour2((Integer) configStore.get(KEY_PEAK_START_HOUR2));
        config.setPeakEndHour2((Integer) configStore.get(KEY_PEAK_END_HOUR2));
        config.setNormalStartHour((Integer) configStore.get(KEY_NORMAL_START_HOUR));
        config.setNormalEndHour((Integer) configStore.get(KEY_NORMAL_END_HOUR));
        
        return config;
    }

    /**
     * 更新系统配置
     * 
     * @param config 新的配置参数
     * @return 更新后的配置
     */
    public SystemConfigDTO updateConfig(SystemConfigDTO config) {
        // 更新电价参数
        if (config.getPeakPrice() != null) {
            configStore.put(KEY_PEAK_PRICE, config.getPeakPrice());
        }
        if (config.getNormalPrice() != null) {
            configStore.put(KEY_NORMAL_PRICE, config.getNormalPrice());
        }
        if (config.getValleyPrice() != null) {
            configStore.put(KEY_VALLEY_PRICE, config.getValleyPrice());
        }
        
        // 更新预警阈值
        if (config.getPowerOverloadThreshold() != null) {
            configStore.put(KEY_POWER_OVERLOAD_THRESHOLD, config.getPowerOverloadThreshold());
        }
        if (config.getPowerWarningThreshold() != null) {
            configStore.put(KEY_POWER_WARNING_THRESHOLD, config.getPowerWarningThreshold());
        }
        if (config.getQuotaWarningThreshold() != null) {
            configStore.put(KEY_QUOTA_WARNING_THRESHOLD, config.getQuotaWarningThreshold());
        }
        
        // 更新时段配置
        if (config.getPeakStartHour1() != null) {
            configStore.put(KEY_PEAK_START_HOUR1, config.getPeakStartHour1());
        }
        if (config.getPeakEndHour1() != null) {
            configStore.put(KEY_PEAK_END_HOUR1, config.getPeakEndHour1());
        }
        if (config.getPeakStartHour2() != null) {
            configStore.put(KEY_PEAK_START_HOUR2, config.getPeakStartHour2());
        }
        if (config.getPeakEndHour2() != null) {
            configStore.put(KEY_PEAK_END_HOUR2, config.getPeakEndHour2());
        }
        if (config.getNormalStartHour() != null) {
            configStore.put(KEY_NORMAL_START_HOUR, config.getNormalStartHour());
        }
        if (config.getNormalEndHour() != null) {
            configStore.put(KEY_NORMAL_END_HOUR, config.getNormalEndHour());
        }
        
        return getConfig();
    }

    // ========== 便捷方法，供其他服务调用 ==========

    /**
     * 获取峰时电价
     */
    public BigDecimal getPeakPrice() {
        return (BigDecimal) configStore.get(KEY_PEAK_PRICE);
    }

    /**
     * 获取平时电价
     */
    public BigDecimal getNormalPrice() {
        return (BigDecimal) configStore.get(KEY_NORMAL_PRICE);
    }

    /**
     * 获取谷时电价
     */
    public BigDecimal getValleyPrice() {
        return (BigDecimal) configStore.get(KEY_VALLEY_PRICE);
    }

    /**
     * 获取功率超限阈值
     */
    public Integer getPowerOverloadThreshold() {
        return (Integer) configStore.get(KEY_POWER_OVERLOAD_THRESHOLD);
    }

    /**
     * 获取功率预警阈值
     */
    public Integer getPowerWarningThreshold() {
        return (Integer) configStore.get(KEY_POWER_WARNING_THRESHOLD);
    }

    /**
     * 获取配额预警阈值
     */
    public Integer getQuotaWarningThreshold() {
        return (Integer) configStore.get(KEY_QUOTA_WARNING_THRESHOLD);
    }

    /**
     * 判断指定小时是否为峰时
     * 
     * @param hour 小时（0-23）
     * @return 是否为峰时
     */
    public boolean isPeakHour(int hour) {
        int peakStart1 = (Integer) configStore.get(KEY_PEAK_START_HOUR1);
        int peakEnd1 = (Integer) configStore.get(KEY_PEAK_END_HOUR1);
        int peakStart2 = (Integer) configStore.get(KEY_PEAK_START_HOUR2);
        int peakEnd2 = (Integer) configStore.get(KEY_PEAK_END_HOUR2);
        
        return (hour >= peakStart1 && hour < peakEnd1) || (hour >= peakStart2 && hour < peakEnd2);
    }

    /**
     * 判断指定小时是否为平时
     * 
     * @param hour 小时（0-23）
     * @return 是否为平时
     */
    public boolean isNormalHour(int hour) {
        int normalStart = (Integer) configStore.get(KEY_NORMAL_START_HOUR);
        int normalEnd = (Integer) configStore.get(KEY_NORMAL_END_HOUR);
        
        return hour >= normalStart && hour < normalEnd;
    }

    /**
     * 判断指定小时是否为谷时
     * 
     * @param hour 小时（0-23）
     * @return 是否为谷时
     */
    public boolean isValleyHour(int hour) {
        return !isPeakHour(hour) && !isNormalHour(hour);
    }

    /**
     * 根据小时获取对应电价
     * 
     * @param hour 小时（0-23）
     * @return 电价
     */
    public BigDecimal getPriceByHour(int hour) {
        if (isPeakHour(hour)) {
            return getPeakPrice();
        } else if (isNormalHour(hour)) {
            return getNormalPrice();
        } else {
            return getValleyPrice();
        }
    }
}
