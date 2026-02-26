package org.example.back.controller.inspector;

import jakarta.servlet.http.HttpSession;
import org.example.back.common.Result;
import org.example.back.dto.SkillApplyRequest;
import org.example.back.entity.SkillCertification;
import org.example.back.entity.User;
import org.example.back.service.common.MinioService;
import org.example.back.service.inspector.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技能认证控制器
 * 
 * -- =============================================
 * -- Author:	每天十点睡
 * -- Create date: 2024
 * -- Description:	巡检员技能认证申请相关接口
 * -- =============================================
 */
@RestController
@RequestMapping("/inspector/skills")
public class SkillController {

    @Autowired
    private SkillService skillService;
    
    @Autowired
    private MinioService minioService;

    /**
     * 上传技能证书文件
     * POST /inspector/skills/upload-certificate
     * 
     * @param file 证书文件（图片或PDF，最大15MB）
     * @param session HTTP会话
     * @return 上传后的文件URL
     */
    @PostMapping("/upload-certificate")
    public Result<Map<String, String>> uploadCertificate(
            @RequestParam("file") MultipartFile file, 
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        String fileUrl = minioService.uploadCertificate(file, user.getId());
        String accessUrl = minioService.getFileUrl(fileUrl);
        
        Map<String, String> result = new HashMap<>();
        result.put("fileUrl", fileUrl);
        result.put("accessUrl", accessUrl);
        
        return Result.success(result);
    }

    /**
     * 提交技能认证申请
     * POST /inspector/skills/apply
     * 
     * @param request 申请请求（skillName技能名称、certificateUrl证书文件URL可选）
     * @param session HTTP会话
     * @return 创建的技能认证记录
     */
    @PostMapping("/apply")
    public Result<SkillCertification> applySkill(@RequestBody SkillApplyRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        SkillCertification certification = skillService.applySkill(user.getId(), request);
        return Result.success(certification);
    }

    /**
     * 获取当前用户的技能列表
     * GET /inspector/skills/my
     * 
     * @param session HTTP会话
     * @return 技能认证列表
     */
    @GetMapping("/my")
    public Result<List<SkillCertification>> getMySkills(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        List<SkillCertification> skills = skillService.getUserSkills(user.getId());
        return Result.success(skills);
    }

    /**
     * 重新申请被拒绝的技能认证
     * POST /inspector/skills/{id}/reapply
     *
     * @param id 被拒绝的认证记录ID
     * @param request 可选的新证书URL
     * @param session HTTP会话
     * @return 更新后的技能认证记录
     */
    @PostMapping("/{id}/reapply")
    public Result<SkillCertification> reapplySkill(
            @PathVariable Long id,
            @RequestBody(required = false) SkillApplyRequest request,
            HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Result.error(401, "请先登录");
        }

        String certificateUrl = (request != null) ? request.getCertificateUrl() : null;
        SkillCertification certification = skillService.reapplySkill(user.getId(), id, certificateUrl);
        return Result.success(certification);
    }
}
