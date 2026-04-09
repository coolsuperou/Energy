package org.example.back.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.back.dto.EquipmentRequest;
import org.example.back.entity.Equipment;
import org.example.back.entity.enums.EquipmentStatus;
import org.example.back.mapper.common.EquipmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(rollbackFor = Exception.class)
    public Equipment create(EquipmentRequest req) {
        Equipment e = new Equipment();
        e.setName(req.getName().trim());
        e.setWorkshopId(req.getWorkshopId());
        e.setRatedPower(req.getRatedPower());
        e.setStatus(req.getStatus() != null ? req.getStatus() : EquipmentStatus.NORMAL);
        e.setLocation(trimToNull(req.getLocation()));
        e.setModel(trimToNull(req.getModel()));
        equipmentMapper.insert(e);
        return e;
    }

    @Transactional(rollbackFor = Exception.class)
    public Equipment update(Long id, EquipmentRequest req) {
        Equipment existing = equipmentMapper.selectById(id);
        if (existing == null) {
            return null;
        }
        existing.setName(req.getName().trim());
        existing.setWorkshopId(req.getWorkshopId());
        existing.setRatedPower(req.getRatedPower());
        existing.setStatus(req.getStatus() != null ? req.getStatus() : EquipmentStatus.NORMAL);
        existing.setLocation(trimToNull(req.getLocation()));
        existing.setModel(trimToNull(req.getModel()));
        equipmentMapper.updateById(existing);
        return existing;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        return equipmentMapper.deleteById(id) > 0;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
