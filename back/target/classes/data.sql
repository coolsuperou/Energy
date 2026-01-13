-- 工具制造电能数据平台 - 测试数据初始化脚本
-- Test Data Initialization Script

USE electric_energy_platform;

-- 清空现有数据（开发测试用）
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE operation_logs;
TRUNCATE TABLE comments;
TRUNCATE TABLE quotas;
TRUNCATE TABLE notifications;
TRUNCATE TABLE tasks;
TRUNCATE TABLE energy_data;
TRUNCATE TABLE applications;
TRUNCATE TABLE equipments;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 1. 插入测试用户数据（5个角色）
-- ============================================
-- 开发环境：密码统一为明文 "123456"

INSERT INTO users (username, password, name, role, department, phone, email, status) VALUES
-- 系统管理员
('admin', '123456', '张管理', 'admin', '信息技术部', '13800000001', 'admin@factory.com', 'active'),

-- 能源调度员
('dispatcher01', '123456', '李调度', 'dispatcher', '能源管理部', '13800000002', 'dispatcher@factory.com', 'active'),
('dispatcher02', '123456', '王调度', 'dispatcher', '能源管理部', '13800000003', 'dispatcher2@factory.com', 'active'),

-- 设备巡检员
('inspector01', '123456', '赵巡检', 'inspector', '设备维护部', '13800000004', 'inspector@factory.com', 'active'),
('inspector02', '123456', '钱巡检', 'inspector', '设备维护部', '13800000005', 'inspector2@factory.com', 'active'),
('inspector03', '123456', '孙巡检', 'inspector', '设备维护部', '13800000006', 'inspector3@factory.com', 'active'),

-- 能源经理
('manager01', '123456', '周经理', 'manager', '能源管理部', '13800000007', 'manager@factory.com', 'active'),

-- 车间用户
('workshop01', '123456', '吴车间', 'workshop', '第一车间', '13800000008', 'workshop1@factory.com', 'active'),
('workshop02', '123456', '郑车间', 'workshop', '第二车间', '13800000009', 'workshop2@factory.com', 'active'),
('workshop03', '123456', '冯车间', 'workshop', '第三车间', '13800000010', 'workshop3@factory.com', 'active'),
('workshop04', '123456', '陈车间', 'workshop', '第四车间', '13800000011', 'workshop4@factory.com', 'active');

-- ============================================
-- 2. 插入设备数据（生产线、机床等）
-- ============================================
-- 第一车间设备（workshop_id = 1）
INSERT INTO equipments (name, workshop_id, rated_power, status, location, model) VALUES
('数控机床-01', 1, 15.50, 'normal', '第一车间A区', 'CNC-2000'),
('数控机床-02', 1, 15.50, 'normal', '第一车间A区', 'CNC-2000'),
('激光切割机-01', 1, 25.00, 'normal', '第一车间B区', 'LC-5000'),
('焊接机器人-01', 1, 12.00, 'normal', '第一车间C区', 'WR-300'),
('冲压机-01', 1, 30.00, 'normal', '第一车间D区', 'PM-800'),

-- 第二车间设备（workshop_id = 2）
('数控机床-03', 2, 15.50, 'normal', '第二车间A区', 'CNC-2000'),
('数控机床-04', 2, 15.50, 'warning', '第二车间A区', 'CNC-2000'),
('激光切割机-02', 2, 25.00, 'normal', '第二车间B区', 'LC-5000'),
('磨床-01', 2, 18.00, 'normal', '第二车间C区', 'GM-600'),
('钻床-01', 2, 8.50, 'normal', '第二车间D区', 'DM-200'),

-- 第三车间设备（workshop_id = 3）
('装配生产线-01', 3, 45.00, 'normal', '第三车间主区', 'AL-1000'),
('喷涂设备-01', 3, 20.00, 'normal', '第三车间喷涂区', 'SP-400'),
('烘干设备-01', 3, 35.00, 'normal', '第三车间烘干区', 'DY-700'),
('检测设备-01', 3, 10.00, 'normal', '第三车间检测区', 'TS-100'),

-- 第四车间设备（workshop_id = 4）
('锻压机-01', 4, 40.00, 'normal', '第四车间A区', 'FP-900'),
('热处理炉-01', 4, 50.00, 'normal', '第四车间B区', 'HT-1200'),
('抛光机-01', 4, 12.00, 'normal', '第四车间C区', 'PM-300'),
('清洗设备-01', 4, 15.00, 'normal', '第四车间D区', 'CL-500');

-- ============================================
-- 3. 插入车间配额数据
-- ============================================
-- 为每个车间设置当前月份和下个月的配额
INSERT INTO quotas (workshop_id, `year_month`, total_quota, used_quota) VALUES
-- 第一车间
(1, DATE_FORMAT(CURDATE(), '%Y-%m'), 50000.00, 12500.00),
(1, DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), '%Y-%m'), 50000.00, 0.00),

-- 第二车间
(2, DATE_FORMAT(CURDATE(), '%Y-%m'), 45000.00, 10800.00),
(2, DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), '%Y-%m'), 45000.00, 0.00),

-- 第三车间
(3, DATE_FORMAT(CURDATE(), '%Y-%m'), 60000.00, 18000.00),
(3, DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), '%Y-%m'), 60000.00, 0.00),

-- 第四车间
(4, DATE_FORMAT(CURDATE(), '%Y-%m'), 55000.00, 16500.00),
(4, DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), '%Y-%m'), 55000.00, 0.00);

-- ============================================
-- 4. 插入示例用电申请数据
-- ============================================
INSERT INTO applications (application_no, user_id, workshop_id, equipment_id, power, apply_date, start_time, end_time, purpose, urgency, status, approved_by, approved_at, comment) VALUES
-- 第一车间 workshop01 (user_id=8) 的申请记录
('APP202501160001', 8, 1, 1, 80.00, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:00:00', '12:00:00', '生产用电', 'normal', 'pending', NULL, NULL, NULL),
('APP202501150002', 8, 1, 2, 45.00, CURDATE(), '14:00:00', '18:00:00', '设备调试', 'normal', 'approved', 2, NOW(), '同意，注意安全用电'),
('APP202501140003', 8, 1, 3, 75.00, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '18:00:00', '22:00:00', '加班生产', 'urgent', 'approved', 2, DATE_SUB(NOW(), INTERVAL 1 DAY), '批准'),
('APP202501130004', 8, 1, 4, 30.00, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '20:00:00', '23:00:00', '临时用电', 'normal', 'rejected', 2, DATE_SUB(NOW(), INTERVAL 2 DAY), '峰时负载已满，建议调整至谷时'),
('APP202501120005', 8, 1, 5, 155.00, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '08:00:00', '17:00:00', '生产用电', 'critical', 'approved', 2, DATE_SUB(NOW(), INTERVAL 3 DAY), '批准，已协调其他车间错峰'),
('APP202501110006', 8, 1, 1, 60.00, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:00:00', '15:00:00', '订单加工', 'normal', 'approved', 2, DATE_SUB(NOW(), INTERVAL 4 DAY), '批准'),
('APP202501100007', 8, 1, 2, 35.00, DATE_SUB(CURDATE(), INTERVAL 5 DAY), '10:00:00', '14:00:00', '样品试制', 'normal', 'approved', 2, DATE_SUB(NOW(), INTERVAL 5 DAY), '批准'),
('APP202501090008', 8, 1, 3, 90.00, DATE_SUB(CURDATE(), INTERVAL 6 DAY), '07:00:00', '19:00:00', '大批量生产', 'urgent', 'approved', 2, DATE_SUB(NOW(), INTERVAL 6 DAY), '批准，注意用电安全'),
('APP202501080009', 8, 1, 4, 25.00, DATE_SUB(CURDATE(), INTERVAL 7 DAY), '23:00:00', '06:00:00', '夜间生产', 'normal', 'approved', 2, DATE_SUB(NOW(), INTERVAL 7 DAY), '批准，谷时用电'),
('APP202501070010', 8, 1, 5, 50.00, DATE_SUB(CURDATE(), INTERVAL 8 DAY), '08:00:00', '12:00:00', '设备测试', 'normal', 'rejected', 2, DATE_SUB(NOW(), INTERVAL 8 DAY), '当日配额已满，请改期申请'),
('APP202501060011', 8, 1, 1, 40.00, DATE_SUB(CURDATE(), INTERVAL 9 DAY), '13:00:00', '17:00:00', '零件加工', 'normal', 'approved', 2, DATE_SUB(NOW(), INTERVAL 9 DAY), '批准'),
('APP202501050012', 8, 1, 2, 55.00, DATE_SUB(CURDATE(), INTERVAL 10 DAY), '08:00:00', '16:00:00', '生产任务', 'normal', 'approved', 2, DATE_SUB(NOW(), INTERVAL 10 DAY), '批准'),

-- 第二车间 workshop02 (user_id=9) 的申请记录
('APP202501050003', 9, 2, 6, 15.50, CURDATE(), '08:30:00', '18:00:00', '精密零件加工', 'normal', 'approved', 2, NOW(), '批准'),
('APP202501040013', 9, 2, 7, 45.00, DATE_SUB(CURDATE(), INTERVAL 1 DAY), '09:00:00', '17:00:00', '批量生产', 'normal', 'approved', 2, DATE_SUB(NOW(), INTERVAL 1 DAY), '批准'),
('APP202501030014', 9, 2, 8, 25.00, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '14:00:00', '18:00:00', '切割作业', 'urgent', 'pending', NULL, NULL, NULL),

-- 第三车间 workshop03 (user_id=10) 的申请记录
('APP202501050004', 10, 3, 11, 45.00, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '07:00:00', '19:00:00', '装配线生产', 'critical', 'pending', NULL, NULL, NULL),
('APP202501020015', 10, 3, 12, 20.00, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '08:00:00', '12:00:00', '喷涂作业', 'normal', 'approved', 2, DATE_SUB(NOW(), INTERVAL 3 DAY), '批准'),

-- 第四车间 workshop04 (user_id=11) 的申请记录
('APP202501050005', 11, 4, 15, 40.00, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '08:00:00', '16:00:00', '锻压作业', 'normal', 'pending', NULL, NULL, NULL),
('APP202501010016', 11, 4, 16, 50.00, DATE_SUB(CURDATE(), INTERVAL 4 DAY), '06:00:00', '18:00:00', '热处理', 'urgent', 'approved', 2, DATE_SUB(NOW(), INTERVAL 4 DAY), '批准，注意温度控制');

-- ============================================
-- 5. 插入示例巡检任务数据
-- ============================================
INSERT INTO tasks (task_type, title, description, equipment_id, assigned_to, assigned_by, priority, status, due_date) VALUES
('inspection', '数控机床-01 日常巡检', '检查设备运行状态、润滑情况、温度等', 1, 4, 2, 'medium', 'pending', CURDATE()),
('inspection', '激光切割机-02 日常巡检', '检查激光头、冷却系统、气路系统', 8, 5, 2, 'medium', 'pending', CURDATE()),
('maintenance', '数控机床-04 预防性维护', '设备显示警告状态，需要进行预防性维护', 7, 4, 2, 'high', 'in_progress', CURDATE()),
('inspection', '热处理炉-01 安全检查', '检查炉体密封、温控系统、安全装置', 16, 6, 2, 'high', 'pending', CURDATE());

-- ============================================
-- 6. 插入示例通知数据
-- ============================================
INSERT INTO notifications (user_id, type, title, content, related_id, is_read) VALUES
-- 车间用户的通知
(8, 'approval', '用电申请已批准', '您的用电申请 APP202501050001 已被批准', 1, 1),
(8, 'approval', '用电申请已批准', '您的用电申请 APP202501050002 已被批准', 2, 0),
(9, 'approval', '用电申请已批准', '您的用电申请 APP202501050003 已被批准', 3, 0),

-- 巡检员的通知
(4, 'task', '新任务分配', '您有新的巡检任务：数控机床-01 日常巡检', 1, 0),
(5, 'task', '新任务分配', '您有新的巡检任务：激光切割机-02 日常巡检', 2, 0),
(4, 'task', '新任务分配', '您有新的维护任务：数控机床-04 预防性维护', 3, 0),

-- 调度员的通知
(2, 'system', '待审批申请提醒', '当前有 2 个待审批的用电申请', NULL, 0);

-- ============================================
-- 7. 插入示例评论数据
-- ============================================
INSERT INTO comments (related_type, related_id, user_id, content) VALUES
('application', 1, 2, '申请已批准，请按时段用电'),
('application', 1, 8, '收到，谢谢！'),
('task', 3, 4, '设备检查完毕，发现轴承有轻微磨损，建议更换'),
('task', 3, 2, '已安排采购新轴承，请继续监控设备状态');

-- ============================================
-- 数据初始化完成
-- ============================================
-- 查询统计信息
SELECT '用户数据' AS '数据类型', COUNT(*) AS '记录数' FROM users
UNION ALL
SELECT '设备数据', COUNT(*) FROM equipments
UNION ALL
SELECT '用电申请', COUNT(*) FROM applications
UNION ALL
SELECT '巡检任务', COUNT(*) FROM tasks
UNION ALL
SELECT '消息通知', COUNT(*) FROM notifications
UNION ALL
SELECT '车间配额', COUNT(*) FROM quotas
UNION ALL
SELECT '评论数据', COUNT(*) FROM comments;
