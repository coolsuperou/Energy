package org.example.back.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.back.entity.Equipment;
import org.example.back.mapper.EquipmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipmentService {

    @Autowired
    private EquipmentMapper equipmentMapper;

    public List<Equipment> getByWorkshopId(Long workshopId) {
        LambdaQueryWrapper<Equipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Equipment::getWorkshopId, workshopId);
        return equipmentMapper.selectList(wrapper);
    }

    public Equipment getById(Long id) {
        return equipmentMapper.selectById(id);
    }

    public List<Equipment> getAll() {
        return equipmentMapper.selectList(null);
    }
}
