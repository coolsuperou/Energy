package org.example.back.dto;

import lombok.Data;

/**
 * 技能认证申请请求DTO
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	巡检员提交技能认证申请的请求参数
 * -- =============================================
 */
@Data
public class SkillApplyRequest {

    /**
     * 技能名称（必填）
     */
    private String skillName;

    /**
     * 证书文件URL（可选，MinIO路径）
     */
    private String certificateUrl;
}
