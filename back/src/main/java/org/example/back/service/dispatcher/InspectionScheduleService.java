package org.example.back.service.dispatcher;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.back.entity.Equipment;
import org.example.back.entity.InspectionPlan;
import org.example.back.entity.InspectionRecord;
import org.example.back.entity.User;
import org.example.back.mapper.common.EquipmentMapper;
import org.example.back.mapper.common.UserMapper;
import org.example.back.mapper.dispatcher.InspectionPlanMapper;
import org.example.back.mapper.inspector.InspectionRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 巡检排班管理服务
 * 调度员使用：管理每周巡检计划的创建、删除、自动排班
 */
@Service
public class InspectionScheduleService {

    @Autowired
    private InspectionPlanMapper planMapper;

    @Autowired
    private InspectionRecordMapper recordMapper;

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Autowired
    private UserMapper userMapper;

    /** 固定5种检查类型 */
    private static final String[] CHECK_TYPES = {
        "appearance", "running", "temperature", "noise", "electrical"
    };

    /**
     * 获取某周的排班计划列表（含进度信息）
     */
    public List<InspectionPlan> getWeekPlans(LocalDate weekStart) {
        List<InspectionPlan> plans = planMapper.selectPlansWithNames(weekStart);
        // 填充进度信息
        for (InspectionPlan plan : plans) {
            Integer total = recordMapper.countTotalEquipments(plan.getId());
            List<Long> checkedList = recordMapper.countFullyCheckedEquipments(plan.getId());
            plan.setTotalCount(total != null ? total : 0);
            plan.setCheckedCount(checkedList != null ? checkedList.size() : 0);
        }
        return plans;
    }

    /**
     * 手动添加排班计划
     * 同时为车间内每台设备自动生成5条检查记录
     */
    @Transactional
    public InspectionPlan addPlan(Long inspectorId, Long workshopId, LocalDate weekStart) {
        // 检查是否已存在
        LambdaQueryWrapper<InspectionPlan> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(InspectionPlan::getInspectorId, inspectorId)
                    .eq(InspectionPlan::getWorkshopId, workshopId)
                    .eq(InspectionPlan::getWeekStart, weekStart);
        if (planMapper.selectCount(checkWrapper) > 0) {
            throw new RuntimeException("该巡检员在本周已分配了该车间");
        }

        // 创建计划
        InspectionPlan plan = new InspectionPlan();
        plan.setInspectorId(inspectorId);
        plan.setWorkshopId(workshopId);
        plan.setWeekStart(weekStart);
        plan.setStatus("pending");

        // 查询车间设备
        LambdaQueryWrapper<Equipment> eqWrapper = new LambdaQueryWrapper<>();
        eqWrapper.eq(Equipment::getWorkshopId, workshopId);
        List<Equipment> equipments = equipmentMapper.selectList(eqWrapper);

        if (equipments.isEmpty()) {
            plan.setRemark("该车间暂无设备");
        }

        planMapper.insert(plan);

        // 为每台设备生成5条检查记录
        generateRecords(plan.getId(), equipments);

        return plan;
    }

    /**
     * 删除排班计划（级联删除关联的检查记录）
     */
    @Transactional
    public void deletePlan(Long planId) {
        InspectionPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("排班记录不存在");
        }
        // 删除关联的检查记录
        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InspectionRecord::getPlanId, planId);
        recordMapper.delete(wrapper);
        // 删除计划
        planMapper.deleteById(planId);
    }

    /**
     * 自动排班：将所有车间均匀分配给所有在职巡检员
     */
    @Transactional
    public List<InspectionPlan> autoSchedule(LocalDate weekStart) {
        // 获取所有在职巡检员
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getRole, "inspector").eq(User::getStatus, "active");
        List<User> inspectors = userMapper.selectList(userWrapper);
        if (inspectors.isEmpty()) {
            throw new RuntimeException("当前无可用巡检员，无法自动排班");
        }

        // 获取所有车间ID（从设备表中获取不重复的workshop_id）
        List<Object> workshopIds = equipmentMapper.selectObjs(
            new LambdaQueryWrapper<Equipment>()
                .select(Equipment::getWorkshopId)
                .groupBy(Equipment::getWorkshopId)
        );
        if (workshopIds.isEmpty()) {
            throw new RuntimeException("当前无车间数据，无法自动排班");
        }

        // 先删除当前周已有的排班
        LambdaQueryWrapper<InspectionPlan> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(InspectionPlan::getWeekStart, weekStart);
        List<InspectionPlan> existPlans = planMapper.selectList(existWrapper);
        for (InspectionPlan ep : existPlans) {
            deletePlan(ep.getId());
        }

        // 均匀分配：轮询方式
        List<InspectionPlan> result = new ArrayList<>();
        int inspectorCount = inspectors.size();
        for (int i = 0; i < workshopIds.size(); i++) {
            Long workshopId = ((Number) workshopIds.get(i)).longValue();
            Long inspectorId = inspectors.get(i % inspectorCount).getId();
            InspectionPlan plan = addPlan(inspectorId, workshopId, weekStart);
            result.add(plan);
        }

        return result;
    }

    /**
     * 获取某计划的巡检记录详情
     */
    public List<InspectionRecord> getPlanRecords(Long planId) {
        return recordMapper.selectRecordsWithEquipment(planId);
    }

    /**
     * 获取所有在职巡检员列表
     */
    public List<User> getInspectors() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "inspector").eq(User::getStatus, "active");
        wrapper.select(User::getId, User::getName, User::getDepartment);
        return userMapper.selectList(wrapper);
    }

    /**
     * 获取所有车间列表（从设备表提取不重复的workshop_id）
     */
    public List<java.util.Map<String, Object>> getWorkshops() {
        List<Object> workshopIds = equipmentMapper.selectObjs(
            new LambdaQueryWrapper<Equipment>()
                .select(Equipment::getWorkshopId)
                .groupBy(Equipment::getWorkshopId)
                .orderByAsc(Equipment::getWorkshopId)
        );
        List<java.util.Map<String, Object>> result = new ArrayList<>();
        for (Object id : workshopIds) {
            Long wid = ((Number) id).longValue();
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", wid);
            map.put("name", "第" + wid + "车间");
            result.add(map);
        }
        return result;
    }

    /**
     * 为计划生成检查记录（每台设备5条）
     */
    private void generateRecords(Long planId, List<Equipment> equipments) {
        for (Equipment eq : equipments) {
            for (String checkType : CHECK_TYPES) {
                InspectionRecord record = new InspectionRecord();
                record.setPlanId(planId);
                record.setEquipmentId(eq.getId());
                record.setCheckType(checkType);
                record.setRepaired(false);
                recordMapper.insert(record);
            }
        }
    }
}
