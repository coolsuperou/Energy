package org.example.back.service.inspector;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.back.entity.*;
import org.example.back.entity.enums.EquipmentStatus;
import org.example.back.mapper.common.EquipmentMapper;
import org.example.back.mapper.dispatcher.InspectionPlanMapper;
import org.example.back.mapper.inspector.InspectionRecordMapper;
import org.example.back.mapper.workshop.FeedbackMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 巡检员巡检执行服务
 * 巡检员使用：查看本周计划、执行检查、异常转报修
 */
@Service
public class InspectionService {

    @Autowired
    private InspectionPlanMapper planMapper;

    @Autowired
    private InspectionRecordMapper recordMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private FeedbackMapper feedbackMapper;

    /** 检查类型中文映射 */
    private static final Map<String, String> CHECK_TYPE_NAMES = new LinkedHashMap<>();
    static {
        CHECK_TYPE_NAMES.put("appearance", "外观检查");
        CHECK_TYPE_NAMES.put("running", "运行状态");
        CHECK_TYPE_NAMES.put("temperature", "温度检测");
        CHECK_TYPE_NAMES.put("noise", "噪音振动");
        CHECK_TYPE_NAMES.put("electrical", "电气安全");
    }

    /**
     * 获取当前巡检员本周的巡检计划
     */
    public List<InspectionPlan> getMyWeekPlans(Long inspectorId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        List<InspectionPlan> plans = planMapper.selectPlansByInspector(inspectorId, weekStart);
        // 填充进度
        for (InspectionPlan plan : plans) {
            Integer total = recordMapper.countTotalEquipments(plan.getId());
            List<Long> checkedList = recordMapper.countFullyCheckedEquipments(plan.getId());
            plan.setTotalCount(total != null ? total : 0);
            plan.setCheckedCount(checkedList != null ? checkedList.size() : 0);
        }
        return plans;
    }

    /**
     * 获取某计划下按设备分组的巡检记录
     */
    public List<Map<String, Object>> getPlanRecordsByEquipment(Long planId) {
        List<InspectionRecord> records = recordMapper.selectRecordsWithEquipment(planId);

        // 按设备分组
        Map<Long, List<InspectionRecord>> grouped = records.stream()
                .collect(Collectors.groupingBy(InspectionRecord::getEquipmentId, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<InspectionRecord>> entry : grouped.entrySet()) {
            List<InspectionRecord> eqRecords = entry.getValue();
            InspectionRecord first = eqRecords.get(0);

            Map<String, Object> eqData = new HashMap<>();
            eqData.put("equipmentId", entry.getKey());
            eqData.put("equipmentName", first.getEquipmentName());
            eqData.put("equipmentLocation", first.getEquipmentLocation());
            eqData.put("equipmentStatus", first.getEquipmentStatus());
            eqData.put("records", eqRecords);

            // 判断该设备是否全部检查完成
            boolean allChecked = eqRecords.stream().allMatch(r -> r.getResult() != null);
            eqData.put("allChecked", allChecked);

            // 判断是否有异常或故障
            boolean hasAbnormal = eqRecords.stream()
                    .anyMatch(r -> "abnormal".equals(r.getResult()) || "fault".equals(r.getResult()));
            eqData.put("hasAbnormal", hasAbnormal);

            // 判断是否已转报修
            boolean isRepaired = eqRecords.stream().anyMatch(r -> Boolean.TRUE.equals(r.getRepaired()));
            eqData.put("repaired", isRepaired);

            result.add(eqData);
        }
        return result;
    }

    /**
     * 更新检查结果
     */
    @Transactional
    public void updateCheckResult(Long recordId, String result, String remark) {
        InspectionRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("检查记录不存在");
        }
        if (Boolean.TRUE.equals(record.getRepaired())) {
            throw new RuntimeException("该设备已转报修，无法修改检查结果");
        }

        // 异常或故障必须填写备注
        if (("abnormal".equals(result) || "fault".equals(result))
                && (remark == null || remark.trim().isEmpty())) {
            throw new RuntimeException("异常或故障结果必须填写备注");
        }

        record.setResult(result);
        record.setRemark(remark);
        record.setCheckedAt(LocalDateTime.now());
        recordMapper.updateById(record);

        // 检查该计划是否全部完成（所有记录都有结果且无异常/故障）
        checkAndCompletePlan(record.getPlanId());
    }

    /**
     * 异常转报修
     * 创建Feedback记录，更新设备状态为fault，标记检查记录已转报修
     */
    @Transactional
    public void convertToRepair(Long recordId, Long inspectorId) {
        InspectionRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("检查记录不存在");
        }
        if (!"abnormal".equals(record.getResult()) && !"fault".equals(record.getResult())) {
            throw new RuntimeException("只有异常或故障的检查项才能转报修");
        }
        if (Boolean.TRUE.equals(record.getRepaired())) {
            throw new RuntimeException("该记录已转报修");
        }

        // 1. 创建Feedback记录
        String checkTypeName = CHECK_TYPE_NAMES.getOrDefault(record.getCheckType(), record.getCheckType());
        Feedback feedback = new Feedback();
        feedback.setFeedbackNo(generateFeedbackNo());
        feedback.setUserId(inspectorId);
        feedback.setType("fault");
        feedback.setEquipmentId(record.getEquipmentId());
        feedback.setUrgency("fault".equals(record.getResult()) ? "urgent" : "normal");
        feedback.setDescription("巡检发现异常 - " + checkTypeName + "：" + (record.getRemark() != null ? record.getRemark() : ""));
        feedback.setStatus("pending");
        feedbackMapper.insert(feedback);

        // 2. 更新设备状态为fault
        Equipment equipment = equipmentMapper.selectById(record.getEquipmentId());
        if (equipment != null) {
            equipment.setStatus(EquipmentStatus.FAULT);
            equipmentMapper.updateById(equipment);
        }

        // 3. 标记该设备所有检查记录为已转报修
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionRecord::getPlanId, record.getPlanId())
               .eq(InspectionRecord::getEquipmentId, record.getEquipmentId());
        List<InspectionRecord> eqRecords = recordMapper.selectList(wrapper);
        for (InspectionRecord r : eqRecords) {
            r.setRepaired(true);
            recordMapper.updateById(r);
        }
    }

    /**
     * 检查计划是否全部完成，自动更新状态
     */
    private void checkAndCompletePlan(Long planId) {
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionRecord::getPlanId, planId);
        List<InspectionRecord> allRecords = recordMapper.selectList(wrapper);

        if (allRecords.isEmpty()) return;

        // 所有记录都有结果
        boolean allHaveResult = allRecords.stream().allMatch(r -> r.getResult() != null);
        // 没有异常或故障（或已转报修的不算）
        boolean noActiveAbnormal = allRecords.stream()
                .noneMatch(r -> !Boolean.TRUE.equals(r.getRepaired())
                        && ("abnormal".equals(r.getResult()) || "fault".equals(r.getResult())));

        if (allHaveResult && noActiveAbnormal) {
            InspectionPlan plan = planMapper.selectById(planId);
            if (plan != null && "pending".equals(plan.getStatus())) {
                plan.setStatus("completed");
                planMapper.updateById(plan);
            }
        }
    }

    /**
     * 恢复报修设备为正常状态
     * 清除repaired标记，恢复设备状态为normal
     */
    @Transactional
    public void restoreFromRepair(Long planId, Long equipmentId) {
        // 1. 清除该设备所有检查记录的repaired标记
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionRecord::getPlanId, planId)
               .eq(InspectionRecord::getEquipmentId, equipmentId);
        List<InspectionRecord> records = recordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            throw new RuntimeException("未找到相关检查记录");
        }
        boolean wasRepaired = records.stream().anyMatch(r -> Boolean.TRUE.equals(r.getRepaired()));
        if (!wasRepaired) {
            throw new RuntimeException("该设备未处于报修状态");
        }
        for (InspectionRecord r : records) {
            r.setRepaired(false);
            recordMapper.updateById(r);
        }

        // 2. 恢复设备状态为normal
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment != null) {
            equipment.setStatus(EquipmentStatus.NORMAL);
            equipmentMapper.updateById(equipment);
        }
    }

    /**
     * 生成反馈编号
     */
    private String generateFeedbackNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "FB" + dateStr;
    }
}
