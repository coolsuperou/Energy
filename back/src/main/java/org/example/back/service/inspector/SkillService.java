package org.example.back.service.inspector;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.back.dto.SkillApplyRequest;
import org.example.back.entity.SkillCertification;
import org.example.back.entity.enums.CertificationStatus;
import org.example.back.exception.BusinessException;
import org.example.back.exception.ErrorCode;
import org.example.back.mapper.inspector.SkillCertificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 技能认证服务
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	巡检员技能认证申请和查询逻辑
 * -- =============================================
 */
@Service
public class SkillService {

    @Autowired
    private SkillCertificationMapper skillCertificationMapper;

    /**
     * 提交技能认证申请
     * 
     * @param userId 申请人ID
     * @param request 申请请求
     * @return 创建的技能认证记录
     */
    public SkillCertification applySkill(Long userId, SkillApplyRequest request) {
        // 参数校验
        if (!StringUtils.hasText(request.getSkillName())) {
            throw new BusinessException("技能名称不能为空");
        }

        // 检查是否已存在相同技能的待审核或已认证申请
        LambdaQueryWrapper<SkillCertification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillCertification::getUserId, userId);
        wrapper.eq(SkillCertification::getSkillName, request.getSkillName().trim());
        wrapper.in(SkillCertification::getStatus, CertificationStatus.PENDING, CertificationStatus.CERTIFIED);
        
        SkillCertification existing = skillCertificationMapper.selectOne(wrapper);
        if (existing != null) {
            if (existing.getStatus() == CertificationStatus.CERTIFIED) {
                throw new BusinessException("该技能已认证，无需重复申请");
            } else {
                throw new BusinessException("该技能已有待审核的申请，请等待审核结果");
            }
        }

        // 创建技能认证申请
        SkillCertification certification = new SkillCertification();
        certification.setUserId(userId);
        certification.setSkillName(request.getSkillName().trim());
        certification.setCertificateUrl(request.getCertificateUrl());
        certification.setStatus(CertificationStatus.PENDING);
        certification.setCreatedAt(LocalDateTime.now());
        certification.setUpdatedAt(LocalDateTime.now());

        skillCertificationMapper.insert(certification);

        return certification;
    }

    /**
     * 重新申请被拒绝的技能认证（基于原记录ID）
     *
     * @param userId 申请人ID
     * @param certId 被拒绝的认证记录ID
     * @param certificateUrl 新的证书文件URL（可选）
     * @return 更新后的技能认证记录
     */
    public SkillCertification reapplySkill(Long userId, Long certId, String certificateUrl) {
        SkillCertification cert = skillCertificationMapper.selectById(certId);
        if (cert == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!cert.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此认证记录");
        }
        if (cert.getStatus() != CertificationStatus.REJECTED) {
            throw new BusinessException("只有被拒绝的认证才能重新申请");
        }

        cert.setStatus(CertificationStatus.PENDING);
        if (certificateUrl != null) {
            cert.setCertificateUrl(certificateUrl);
        }
        cert.setRejectReason(null);
        cert.setReviewedBy(null);
        cert.setReviewedAt(null);
        cert.setUpdatedAt(LocalDateTime.now());
        skillCertificationMapper.updateById(cert);
        return cert;
    }

    /**
     * 获取用户的技能认证列表
     * 
     * @param userId 用户ID
     * @return 技能认证列表
     */
    public List<SkillCertification> getUserSkills(Long userId) {
        LambdaQueryWrapper<SkillCertification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillCertification::getUserId, userId);
        wrapper.orderByDesc(SkillCertification::getCreatedAt);
        
        return skillCertificationMapper.selectList(wrapper);
    }

    /**
     * 获取用户已认证的技能列表
     * 
     * @param userId 用户ID
     * @return 已认证的技能列表
     */
    public List<SkillCertification> getCertifiedSkills(Long userId) {
        LambdaQueryWrapper<SkillCertification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SkillCertification::getUserId, userId);
        wrapper.eq(SkillCertification::getStatus, CertificationStatus.CERTIFIED);
        wrapper.orderByDesc(SkillCertification::getReviewedAt);
        
        return skillCertificationMapper.selectList(wrapper);
    }
}
