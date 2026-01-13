package org.example.back.controller;

import org.example.back.common.Result;
import org.example.back.entity.Equipment;
import org.example.back.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/equipments")
public class EquipmentController {

    @Autowired
    private EquipmentService equipmentService;

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

    @GetMapping("/{id}")
    public Result<Equipment> getById(@PathVariable Long id) {
        Equipment equipment = equipmentService.getById(id);
        if (equipment == null) {
            return Result.error(404, "设备不存在");
        }
        return Result.success(equipment);
    }
}
