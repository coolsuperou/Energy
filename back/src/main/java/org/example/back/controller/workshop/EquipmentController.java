package org.example.back.controller.workshop;

import org.example.back.common.Result;
import org.example.back.entity.Equipment;
import org.example.back.entity.User;
import org.example.back.entity.enums.UserRole;
import org.example.back.service.common.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * 设备管理控制器
 * 提供设备信息的查询接口
 * 用于车间用电申请时选择设备以及巡检员查看设备状态
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
     * 从部门名称解析车间ID
     */
    private Long parseWorkshopIdFromDepartment(String department) {
        if (department == null) return null;
        if (department.contains("第一")) return 1L;
        if (department.contains("第二")) return 2L;
        if (department.contains("第三")) return 3L;
        return null;
    }
}
