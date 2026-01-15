package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.example.back.entity.enums.UserRole;
import org.example.back.entity.enums.UserStatus;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 系统所有用户的基础信息
 * 包含五种角色 车间用户、调度员、巡检员、经理、管理员
 */
@Data
@TableName("users")
public class User {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 唯一 */
    private String username;

    /** 登录密码 加密存储 */
    private String password;

    /** 真实姓名 */
    private String name;

    /** 用户角色 WORKSHOP车间用户 DISPATCHER调度员 INSPECTOR巡检员 MANAGER经理 ADMIN管理员 */
    private UserRole role;

    /** 所属部门 */
    private String department;

    /** 联系电话 */
    private String phone;

    /** 电子邮箱 */
    private String email;

    /** 用户状态 ACTIVE启用 DISABLED禁用 */
    private UserStatus status;

    /** 创建时间 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
