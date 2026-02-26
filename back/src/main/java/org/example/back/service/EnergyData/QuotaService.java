package org.example.back.service.energyData;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.back.entity.Quota;
import org.example.back.mapper.common.QuotaMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 车间配额服务
 * 管理各车间的月度用电配额
 * 
 * @author 每天十点睡
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final QuotaMapper quotaMapper;
    
    /** 默认月度配额（kWh） */
    private static final BigDecimal DEFAULT_MONTHLY_QUOTA = new BigDecimal("50000");
    
    /** 配额预警阈值（85%） */
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("85");
    
    /** 配额严重预警阈值（95%） */
    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("95");

    /**
     * 获取车间当月配额
     * 如果不存在则自动创建
     * 
     * @param workshopId 车间ID
     * @return 配额信息
     */
    public Quota getOrCreateCurrentMonthQuota(Long workshopId) {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return getOrCreateQuota(workshopId, yearMonth);
    }

    /**
     * 获取或创建指定月份的配额
     * 
     * @param workshopId 车间ID
     * @param yearMonth 年月（格式：yyyy-MM）
     * @return 配额信息
     */
    public Quota getOrCreateQuota(Long workshopId, String yearMonth) {
        Quota quota = quotaMapper.selectOne(
            new LambdaQueryWrapper<Quota>()
                .eq(Quota::getWorkshopId, workshopId)
                .eq(Quota::getYearMonth, yearMonth)
        );
        
        if (quota == null) {
            quota = new Quota();
            quota.setWorkshopId(workshopId);
            quota.setYearMonth(yearMonth);
            quota.setTotalQuota(DEFAULT_MONTHLY_QUOTA);
            quota.setUsedQuota(BigDecimal.ZERO);
            quotaMapper.insert(quota);
            log.info("创建车间配额: workshopId={}, yearMonth={}, totalQuota={}", 
                    workshopId, yearMonth, DEFAULT_MONTHLY_QUOTA);
        }
        
        return quota;
    }

    /**
     * 查询车间配额
     * 
     * @param workshopId 车间ID
     * @param yearMonth 年月（格式：yyyy-MM）
     * @return 配额信息，不存在返回null
     */
    public Quota getQuota(Long workshopId, String yearMonth) {
        return quotaMapper.selectOne(
            new LambdaQueryWrapper<Quota>()
                .eq(Quota::getWorkshopId, workshopId)
                .eq(Quota::getYearMonth, yearMonth)
        );
    }

    /**
     * 获取所有车间当月配额列表
     * 
     * @return 配额列表
     */
    public List<Quota> getCurrentMonthQuotas() {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return quotaMapper.selectList(
            new LambdaQueryWrapper<Quota>()
                .eq(Quota::getYearMonth, yearMonth)
                .orderByAsc(Quota::getWorkshopId)
        );
    }

    /**
     * 更新已用配额
     * 
     * @param workshopId 车间ID
     * @param energyUsed 新增用电量（kWh）
     * @param recordDate 记录日期（用于确定年月）
     * @return 更新后的配额信息
     */
    @Transactional
    public Quota updateUsedQuota(Long workshopId, BigDecimal energyUsed, LocalDate recordDate) {
        String yearMonth = recordDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Quota quota = getOrCreateQuota(workshopId, yearMonth);
        
        BigDecimal newUsedQuota = quota.getUsedQuota().add(energyUsed);
        
        quotaMapper.update(null,
            new LambdaUpdateWrapper<Quota>()
                .eq(Quota::getId, quota.getId())
                .set(Quota::getUsedQuota, newUsedQuota)
        );
        
        quota.setUsedQuota(newUsedQuota);
        
        // 检查是否需要发送预警
        checkAndLogQuotaWarning(quota);
        
        log.debug("更新车间配额: workshopId={}, yearMonth={}, energyUsed={}, newUsedQuota={}", 
                workshopId, yearMonth, energyUsed, newUsedQuota);
        
        return quota;
    }

    /**
     * 设置车间月度总配额
     * 
     * @param workshopId 车间ID
     * @param yearMonth 年月
     * @param totalQuota 总配额（kWh）
     * @return 更新后的配额信息
     */
    @Transactional
    public Quota setTotalQuota(Long workshopId, String yearMonth, BigDecimal totalQuota) {
        Quota quota = getOrCreateQuota(workshopId, yearMonth);
        
        quotaMapper.update(null,
            new LambdaUpdateWrapper<Quota>()
                .eq(Quota::getId, quota.getId())
                .set(Quota::getTotalQuota, totalQuota)
        );
        
        quota.setTotalQuota(totalQuota);
        log.info("设置车间配额: workshopId={}, yearMonth={}, totalQuota={}", 
                workshopId, yearMonth, totalQuota);
        
        return quota;
    }

    /**
     * 检查配额使用情况并记录预警日志
     * 
     * @param quota 配额信息
     */
    private void checkAndLogQuotaWarning(Quota quota) {
        BigDecimal usagePercentage = quota.getUsagePercentage();
        
        if (usagePercentage.compareTo(CRITICAL_THRESHOLD) >= 0) {
            log.warn("【配额严重预警】车间ID={}, 年月={}, 已用配额={}kWh, 总配额={}kWh, 使用率={}%", 
                    quota.getWorkshopId(), quota.getYearMonth(), 
                    quota.getUsedQuota(), quota.getTotalQuota(), usagePercentage);
        } else if (usagePercentage.compareTo(WARNING_THRESHOLD) >= 0) {
            log.warn("【配额预警】车间ID={}, 年月={}, 已用配额={}kWh, 总配额={}kWh, 使用率={}%", 
                    quota.getWorkshopId(), quota.getYearMonth(), 
                    quota.getUsedQuota(), quota.getTotalQuota(), usagePercentage);
        }
    }

    /**
     * 检查配额是否超过预警阈值
     * 
     * @param workshopId 车间ID
     * @return true-超过预警阈值
     */
    public boolean isQuotaWarning(Long workshopId) {
        Quota quota = getOrCreateCurrentMonthQuota(workshopId);
        return quota.getUsagePercentage().compareTo(WARNING_THRESHOLD) >= 0;
    }

    /**
     * 检查配额是否超过严重预警阈值
     * 
     * @param workshopId 车间ID
     * @return true-超过严重预警阈值
     */
    public boolean isQuotaCritical(Long workshopId) {
        Quota quota = getOrCreateCurrentMonthQuota(workshopId);
        return quota.getUsagePercentage().compareTo(CRITICAL_THRESHOLD) >= 0;
    }

    /**
     * 获取车间剩余配额
     * 
     * @param workshopId 车间ID
     * @return 剩余配额（kWh）
     */
    public BigDecimal getRemainingQuota(Long workshopId) {
        Quota quota = getOrCreateCurrentMonthQuota(workshopId);
        return quota.getRemainingQuota();
    }
}
