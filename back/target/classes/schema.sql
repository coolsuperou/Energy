-- 工具制造电能数据平台 - 数据库表结构
-- Database Schema for Electric Energy Management Platform

-- 创建数据库
CREATE DATABASE IF NOT EXISTS electric_energy_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE electric_energy_platform;

-- 1. 用户表 (users)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    role ENUM('admin', 'dispatcher', 'inspector', 'manager', 'workshop') NOT NULL COMMENT '角色',
    department VARCHAR(100) COMMENT '部门',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    avatar_url VARCHAR(255) COMMENT '头像URL',
    status ENUM('active', 'disabled') DEFAULT 'active' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 设备表 (equipments)
CREATE TABLE IF NOT EXISTS equipments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '设备ID',
    name VARCHAR(100) NOT NULL COMMENT '设备名称',
    workshop_id BIGINT NOT NULL COMMENT '所属车间ID',
    rated_power DECIMAL(10,2) NOT NULL COMMENT '额定功率(kW)',
    status ENUM('normal', 'warning', 'fault') DEFAULT 'normal' COMMENT '设备状态',
    location VARCHAR(200) COMMENT '设备位置',
    model VARCHAR(100) COMMENT '设备型号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_workshop (workshop_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备表';

-- 3. 用电申请表 (applications)
CREATE TABLE IF NOT EXISTS applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    application_no VARCHAR(20) NOT NULL UNIQUE COMMENT '申请编号',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    workshop_id BIGINT NOT NULL COMMENT '车间ID',
    equipment_id BIGINT NOT NULL COMMENT '设备ID',
    power DECIMAL(10,2) NOT NULL COMMENT '申请功率(kW)',
    apply_date DATE NOT NULL COMMENT '用电日期',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    purpose VARCHAR(500) COMMENT '用电用途',
    urgency ENUM('normal', 'urgent', 'critical') DEFAULT 'normal' COMMENT '紧急程度',
    status ENUM('pending', 'approved', 'rejected', 'adjusted') DEFAULT 'pending' COMMENT '申请状态',
    approved_by BIGINT COMMENT '审批人ID',
    approved_at DATETIME COMMENT '审批时间',
    comment VARCHAR(500) COMMENT '审批意见',
    adjusted_start_time TIME COMMENT '调整后开始时间',
    adjusted_end_time TIME COMMENT '调整后结束时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (equipment_id) REFERENCES equipments(id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_application_no (application_no),
    INDEX idx_user (user_id),
    INDEX idx_workshop (workshop_id),
    INDEX idx_status (status),
    INDEX idx_apply_date (apply_date),
    INDEX idx_urgency (urgency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用电申请表';

-- 4. 能耗数据表 (energy_data)
CREATE TABLE IF NOT EXISTS energy_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '能耗记录ID',
    application_id BIGINT NOT NULL COMMENT '关联申请ID',
    workshop_id BIGINT NOT NULL COMMENT '车间ID',
    equipment_id BIGINT NOT NULL COMMENT '设备ID',
    record_date DATE NOT NULL COMMENT '记录日期',
    record_hour TINYINT NOT NULL COMMENT '小时(0-23)',
    power DECIMAL(10,2) NOT NULL COMMENT '功率(kW)',
    energy DECIMAL(10,2) NOT NULL COMMENT '电量(kWh)',
    period_type ENUM('peak', 'normal', 'valley') NOT NULL COMMENT '时段类型',
    price DECIMAL(5,2) NOT NULL COMMENT '电价(元/kWh)',
    cost DECIMAL(10,2) NOT NULL COMMENT '费用(元)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    FOREIGN KEY (equipment_id) REFERENCES equipments(id) ON DELETE CASCADE,
    INDEX idx_workshop_date (workshop_id, record_date),
    INDEX idx_application (application_id),
    INDEX idx_record_date (record_date),
    INDEX idx_period_type (period_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='能耗数据表';

-- 5. 巡检任务表 (tasks)
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_type ENUM('inspection', 'repair', 'maintenance') NOT NULL COMMENT '任务类型',
    title VARCHAR(200) NOT NULL COMMENT '任务标题',
    description TEXT COMMENT '任务描述',
    equipment_id BIGINT COMMENT '关联设备ID',
    assigned_to BIGINT COMMENT '分配给（巡检员ID）',
    assigned_by BIGINT NOT NULL COMMENT '分配人（调度员ID）',
    priority ENUM('low', 'normal', 'high', 'urgent') DEFAULT 'normal' COMMENT '优先级',
    status ENUM('pending', 'assigned', 'in_progress', 'completed') DEFAULT 'pending' COMMENT '任务状态',
    due_date DATE COMMENT '截止日期',
    completed_at DATETIME COMMENT '完成时间',
    report TEXT COMMENT '完成报告',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (equipment_id) REFERENCES equipments(id) ON DELETE SET NULL,
    INDEX idx_assigned_to (assigned_to),
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡检任务表';

-- 6. 消息通知表 (notifications)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '通知ID',
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    type ENUM('approval', 'task', 'alert', 'system') NOT NULL COMMENT '通知类型',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    related_id BIGINT COMMENT '关联业务ID',
    is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_type (type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息通知表';

-- 7. 配额表 (quotas)
CREATE TABLE IF NOT EXISTS quotas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配额ID',
    workshop_id BIGINT NOT NULL COMMENT '车间ID',
    `year_month` VARCHAR(7) NOT NULL COMMENT '年月 YYYY-MM',
    total_quota DECIMAL(12,2) NOT NULL COMMENT '总配额(kWh)',
    used_quota DECIMAL(12,2) DEFAULT 0 COMMENT '已用配额(kWh)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_workshop_month (workshop_id, `year_month`),
    INDEX idx_year_month (`year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配额表';

-- 8. 评论表 (comments) - 用于申请和工单的业务沟通
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评论ID',
    related_type ENUM('application', 'task') NOT NULL COMMENT '关联类型',
    related_id BIGINT NOT NULL COMMENT '关联业务ID',
    user_id BIGINT NOT NULL COMMENT '评论人ID',
    content TEXT NOT NULL COMMENT '评论内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_related (related_type, related_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 9. 问题反馈表 (feedbacks)
CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '反馈ID',
    feedback_no VARCHAR(20) NOT NULL UNIQUE COMMENT '反馈编号',
    user_id BIGINT NOT NULL COMMENT '提交用户ID',
    type ENUM('fault', 'suggestion', 'question', 'other') NOT NULL COMMENT '反馈类型',
    location VARCHAR(100) COMMENT '相关位置',
    urgency ENUM('normal', 'urgent', 'critical') DEFAULT 'normal' COMMENT '紧急程度',
    description TEXT NOT NULL COMMENT '问题描述',
    status ENUM('pending', 'processing', 'resolved', 'withdrawn') DEFAULT 'pending' COMMENT '状态',
    reply TEXT COMMENT '处理回复',
    handled_by BIGINT COMMENT '处理人ID',
    handled_at DATETIME COMMENT '处理时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (handled_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_feedback_no (feedback_no),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='问题反馈表';

-- 10. 操作日志表 (operation_logs)
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT NOT NULL COMMENT '操作人ID',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    operation_desc VARCHAR(500) COMMENT '操作描述',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 11. 考勤排班表 (attendance_records)
CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考勤ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    attendance_date DATE NOT NULL COMMENT '考勤日期',
    
    -- 排班信息
    shift_type ENUM('day', 'night', 'rest') DEFAULT 'day' COMMENT '班次类型：白班/夜班/休息',
    scheduled_start_time TIME COMMENT '计划上班时间',
    scheduled_end_time TIME COMMENT '计划下班时间',
    
    -- 打卡信息
    clock_in_time TIME COMMENT '实际上班打卡时间',
    clock_out_time TIME COMMENT '实际下班打卡时间',
    
    -- 考勤状态
    status ENUM('normal', 'late', 'early_leave', 'absent', 'rest', 'holiday') DEFAULT 'normal' COMMENT '考勤状态',
    work_hours DECIMAL(4,2) COMMENT '工作时长(小时)',
    remark VARCHAR(200) COMMENT '备注',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_date (user_id, attendance_date),
    INDEX idx_user_date (user_id, attendance_date),
    INDEX idx_status (status),
    INDEX idx_shift_type (shift_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤排班表';

USE electric_energy_platform;

-- 修改 assigned_to 字段允许 NULL
ALTER TABLE tasks MODIFY COLUMN assigned_to BIGINT COMMENT '分配给（巡检员ID）';

-- 修改 status 枚举，添加 assigned 状态
ALTER TABLE tasks MODIFY COLUMN status ENUM('pending', 'assigned', 'in_progress', 'completed') DEFAULT 'pending' COMMENT '任务状态';

-- 修改 priority 枚举，添加 urgent，将 medium 改为 normal
ALTER TABLE tasks MODIFY COLUMN priority ENUM('low', 'normal', 'high', 'urgent') DEFAULT 'normal' COMMENT '优先级';

-- 修改外键约束
ALTER TABLE tasks DROP FOREIGN KEY tasks_ibfk_1;
ALTER TABLE tasks ADD CONSTRAINT tasks_ibfk_1 FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL;