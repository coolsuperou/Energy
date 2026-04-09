package org.example.back.controller.workshop;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.example.back.common.Result;
import org.example.back.dto.EquipmentRequest;
import org.example.back.entity.Equipment;
import org.example.back.entity.User;
import org.example.back.entity.enums.UserRole;
import org.example.back.service.common.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备管理控制器
 * 查询：车间用户仅本车间；巡检员/调度等可查全部或按车间筛选。
 * 增删改：巡检员、调度员、经理、管理员；车间用户不可改。
 */
@RestController
@RequestMapping("/equipments")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;

    /**
     * 获取设备列表
     * 车间用户自动过滤只显示自己车间的设备
     * 其他角色可按车间ID筛选，不传参数则返回所有设备
     */
    @GetMapping
    public Result<List<Equipment>> list(@RequestParam(required = false) Long workshopId, HttpSession session) {
        // 车间用户自动过滤自己车间的设备
        if (workshopId == null) {
            User user = (User) session.getAttribute("user");
            if (user != null && user.getRole() == UserRole.WORKSHOP) {
                workshopId = parseWorkshopIdFromDepartment(user.getDepartment());
            }
        }

        List<Equipment> list;
        if (workshopId != null) {
            list = equipmentService.getByWorkshopId(workshopId);
        } else {
            list = equipmentService.getAll();
        }
        return Result.success(list);
    }

    /**
     * 根据ID获取设备详情
     */
    @GetMapping("/{id}")
    public Result<Equipment> getById(@PathVariable Long id) {
        Equipment equipment = equipmentService.getById(id);
        if (equipment == null) {
            return Result.error(404, "设备不存在");
        }
        return Result.success(equipment);
    }

    /**
     * 新增设备（POST /equipments/create，JSON）。
     */
    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Equipment> create(@Valid @RequestBody EquipmentRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (!canManageEquipment(user)) {
            return Result.error(403, "无权限创建设备");
        }
        Equipment created = equipmentService.create(request);
        return Result.success(created);
    }

    @PutMapping("/{id}")
    public Result<Equipment> update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (!canManageEquipment(user)) {
            return Result.error(403, "无权限修改设备");
        }
        Equipment updated = equipmentService.update(id, request);
        if (updated == null) {
            return Result.error(404, "设备不存在");
        }
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (!canManageEquipment(user)) {
            return Result.error(403, "无权限删除设备");
        }
        if (equipmentService.getById(id) == null) {
            return Result.error(404, "设备不存在");
        }
        try {
            equipmentService.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            return Result.error(409, "该设备仍被用电申请、工单或其他数据引用，无法删除");
        }
        return Result.success(null);
    }

    private static boolean canManageEquipment(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        UserRole r = user.getRole();
        return r == UserRole.INSPECTOR || r == UserRole.ADMIN || r == UserRole.DISPATCHER || r == UserRole.MANAGER;
    }

    /**
     * 从部门名称解析车间ID
     */
    private Long parseWorkshopIdFromDepartment(String department) {
        if (department == null) return null;
        // 通用解析：支持"第一车间"~"第N车间"格式
        String[] cnNumbers = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
        for (int i = 0; i < cnNumbers.length; i++) {
            if (department.contains("第" + cnNumbers[i])) return (long) (i + 1);
        }
        // 尝试匹配"第N车间"中的数字格式（如"第1车间"）
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("第(\\d+)").matcher(department);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }
}
