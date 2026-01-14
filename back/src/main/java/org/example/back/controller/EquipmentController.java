package org.example.back.controller;

import org.example.back.common.Result;
import org.example.back.entity.Equipment;
import org.example.back.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
     * 可按车间ID筛选 不传参数则返回所有设备
     * @param workshopId 车间ID 可选参数
     * @return 设备列表
     */
    @GetMapping
    public Result<List<Equipment>> list(@RequestParam(required = false) Long workshopId) {
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
     * @param id 设备ID
     * @return 设备信息
     */
    @GetMapping("/{id}")
    public Result<Equipment> getById(@PathVariable Long id) {
        Equipment equipment = equipmentService.getById(id);
        if (equipment == null) {
            return Result.error(404, "设备不存在");
        }
        return Result.success(equipment);
    }
}
