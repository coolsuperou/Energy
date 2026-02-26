package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.example.back.entity.enums.CertificationStatus;

import java.time.LocalDateTime;

/**
 * 技能认证实体类
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	技能认证申请和状态管理
 * -- =============================================
 */
@Data
@TableName("skill_certifications")
public class SkillCertification {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申请人ID（巡检员）
     */
    private Long userId;

    /**
     * 技能名称
     */
    private String skillName;

    /**
     * 证书文件URL（MinIO路径）
     */
    private String certificateUrl;

    /**
     * 认证状态：pending待审核/certified已认证/rejected已拒绝
     */
    private CertificationStatus status;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 审核人ID（经理）
     */
    private Long reviewedBy;

    /**
     * 审核时间
     */
    private LocalDateTime reviewedAt;

    /**
     * 申请时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
