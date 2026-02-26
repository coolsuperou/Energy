# 工具制造电能数据平台 - 设计文档

## 1. 架构概述

### 1.1 系统架构

采用前后端分离的B/S架构：

```
┌─────────────────────────────────────────────────┐
│                   浏览器 (Vue 3)                  │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │ Vue Router│ │  Pinia   │ │ Axios (API层)    │ │
│  └──────────┘ └──────────┘ └────────┬─────────┘ │
└─────────────────────────────────────┼───────────┘
                                      │ HTTP/SSE
                                      ▼
┌─────────────────────────────────────────────────┐
│              Vite Dev Server (:3000)              │
│              /api → proxy → :8080                │
└─────────────────────────────────────┼───────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────┐
│           Spring Boot (:8080, /api)              │
│  ┌──────────┐ ┌──────────┐ ┌──────────────────┐ │
│  │Controller │→│ Service  │→│ Mapper(MyBatis+) │ │
│  └──────────┘ └──────────┘ └────────┬─────────┘ │
│  ┌──────────┐ ┌──────────┐          │           │
│  │ MinIO SDK│ │ OkHttp WS│          │           │
│  └──────────┘ └──────────┘          │           │
└─────────────────────────────────────┼───────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                  ▼
              ┌──────────┐    ┌──────────┐      ┌──────────┐
              │  MySQL    │    │  MinIO   │      │ 讯飞星火  │
              │  (:3306)  │    │(:9090/9000)│    │  (WSS)   │
              └──────────┘    └──────────┘      └──────────┘
```

### 1.2 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + Vite | 3.4 / 5.0 |
| UI组件 | Element Plus + Bootstrap 5 + Bootstrap Icons | 2.4 / 5.3 |
| 图表 | ECharts | 5.4 |
| 状态管理 | Pinia | 2.1 |
| 路由 | Vue Router | 4.2 |
| HTTP客户端 | Axios | 1.6 |
| 后端框架 | Spring Boot | 3.2 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.x |
| 对象存储 | MinIO | 8.5.7 |
| AI通信 | OkHttp WebSocket | - |
| 密码加密 | Spring Security Crypto (BCrypt) | - |
| 测试 | JUnit 5 + jqwik 1.8.2 + H2 | - |

### 1.3 角色与路由映射

| 角色 | 路由前缀 | 布局组件 |
|------|----------|----------|
| 车间用户(workshop) | /workshop | WorkshopLayout.vue |
| 能源调度员(dispatcher) | /dispatcher | DispatcherLayout.vue |
| 设备巡检员(inspector) | /inspector | InspectorLayout.vue |
| 经理(manager) | /manager | ManagerLayout.vue |
| 系统管理员(admin) | /admin | AdminLayout.vue |

---

## 2. 数据模型

### 2.1 数据库ER关系

```
users (1) ──── (N) applications     用户提交多个申请
users (1) ──── (N) tasks            用户被分配多个任务
users (1) ──── (N) feedbacks        用户提交多个反馈
users (1) ──── (N) notifications    用户接收多个通知
users (1) ──── (N) comments         用户发表多个评论
users (1) ──── (N) attendance_records 用户有多条考勤
users (1) ──── (N) operation_logs   用户产生多条日志

equipments (1) ── (N) applications  设备关联多个申请
equipments (1) ── (N) energy_data   设备产生多条能耗
equipments (1) ── (N) tasks         设备关联多个任务

applications (1) ── (N) energy_data 申请生成多条能耗
applications (1) ── (N) comments    申请有多条评论
tasks (1) ──────── (N) comments     任务有多条评论
```


### 2.2 数据库表设计

#### users - 用户表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 登录账号 |
| password | VARCHAR(255) | NOT NULL | 密码（BCrypt加密） |
| name | VARCHAR(50) | NOT NULL | 真实姓名 |
| role | ENUM('admin','dispatcher','inspector','manager','workshop') | NOT NULL | 角色 |
| department | VARCHAR(100) | | 部门 |
| phone | VARCHAR(20) | | 联系电话 |
| email | VARCHAR(100) | | 邮箱 |
| avatar_url | VARCHAR(255) | | 头像URL（MinIO路径） |
| status | ENUM('active','disabled') | DEFAULT 'active' | 状态 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

#### equipments - 设备表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 设备ID |
| name | VARCHAR(100) | NOT NULL | 设备名称 |
| workshop_id | BIGINT | NOT NULL | 所属车间ID |
| rated_power | DECIMAL(10,2) | NOT NULL | 额定功率(kW) |
| status | ENUM('normal','warning','fault') | DEFAULT 'normal' | 设备状态 |
| location | VARCHAR(200) | | 设备位置 |
| model | VARCHAR(100) | | 设备型号 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

#### applications - 用电申请表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 申请ID |
| application_no | VARCHAR(20) | UNIQUE, NOT NULL | 申请编号(AP+日期+序号) |
| user_id | BIGINT | FK→users, NOT NULL | 申请人ID |
| workshop_id | BIGINT | NOT NULL | 车间ID |
| equipment_id | BIGINT | FK→equipments, NOT NULL | 设备ID |
| power | DECIMAL(10,2) | NOT NULL | 申请功率(kW) |
| apply_date | DATE | NOT NULL | 用电日期 |
| start_time | TIME | NOT NULL | 开始时间 |
| end_time | TIME | NOT NULL | 结束时间 |
| purpose | VARCHAR(500) | | 用电用途 |
| urgency | ENUM('normal','urgent','critical') | DEFAULT 'normal' | 紧急程度 |
| status | ENUM('pending','approved','rejected','adjusted') | DEFAULT 'pending' | 状态 |
| approved_by | BIGINT | FK→users | 审批人ID |
| approved_at | DATETIME | | 审批时间 |
| comment | VARCHAR(500) | | 审批意见 |
| adjusted_start_time | TIME | | 调整后开始时间 |
| adjusted_end_time | TIME | | 调整后结束时间 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

#### energy_data - 能耗数据表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 能耗记录ID |
| application_id | BIGINT | FK→applications, NOT NULL | 关联申请ID |
| workshop_id | BIGINT | NOT NULL | 车间ID |
| equipment_id | BIGINT | FK→equipments, NOT NULL | 设备ID |
| record_date | DATE | NOT NULL | 记录日期 |
| record_hour | TINYINT | NOT NULL | 小时(0-23) |
| power | DECIMAL(10,2) | NOT NULL | 功率(kW) |
| energy | DECIMAL(10,2) | NOT NULL | 电量(kWh) |
| period_type | ENUM('peak','normal','valley') | NOT NULL | 时段类型 |
| price | DECIMAL(5,2) | NOT NULL | 电价(元/kWh) |
| cost | DECIMAL(10,2) | NOT NULL | 费用(元) |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

#### tasks - 巡检任务表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 任务ID |
| task_type | ENUM('inspection','repair','maintenance') | NOT NULL | 任务类型 |
| title | VARCHAR(200) | NOT NULL | 任务标题 |
| description | TEXT | | 任务描述 |
| equipment_id | BIGINT | FK→equipments | 关联设备ID |
| assigned_to | BIGINT | FK→users | 分配给（巡检员ID） |
| assigned_by | BIGINT | FK→users, NOT NULL | 分配人（调度员ID） |
| priority | ENUM('low','normal','high','urgent') | DEFAULT 'normal' | 优先级 |
| status | ENUM('pending','in_progress','completed') | DEFAULT 'pending' | 任务状态 |
| due_date | DATE | | 截止日期 |
| completed_at | DATETIME | | 完成时间 |
| report | TEXT | | 完成报告 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

#### notifications - 消息通知表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 通知ID |
| user_id | BIGINT | FK→users, NOT NULL | 接收用户ID |
| type | ENUM('approval','task','alert','system') | NOT NULL | 通知类型 |
| title | VARCHAR(200) | NOT NULL | 通知标题 |
| content | TEXT | | 通知内容 |
| related_id | BIGINT | | 关联业务ID |
| is_read | TINYINT(1) | DEFAULT 0 | 是否已读 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

#### feedbacks - 问题反馈表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 反馈ID |
| feedback_no | VARCHAR(20) | UNIQUE, NOT NULL | 反馈编号(FB+日期+序号) |
| user_id | BIGINT | FK→users, NOT NULL | 提交用户ID |
| type | ENUM('fault','suggestion','question','other') | NOT NULL | 反馈类型 |
| location | VARCHAR(100) | | 相关位置 |
| urgency | ENUM('normal','urgent','critical') | DEFAULT 'normal' | 紧急程度 |
| description | TEXT | NOT NULL | 问题描述 |
| status | ENUM('pending','processing','resolved','withdrawn') | DEFAULT 'pending' | 状态 |
| reply | TEXT | | 处理回复 |
| handled_by | BIGINT | FK→users | 处理人ID |
| handled_at | DATETIME | | 处理时间 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |

#### comments - 评论表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 评论ID |
| related_type | ENUM('application','task') | NOT NULL | 关联类型 |
| related_id | BIGINT | NOT NULL | 关联业务ID |
| user_id | BIGINT | FK→users, NOT NULL | 评论人ID |
| content | TEXT | NOT NULL | 评论内容 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

#### quotas - 配额表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 配额ID |
| workshop_id | BIGINT | NOT NULL | 车间ID |
| year_month | VARCHAR(7) | NOT NULL | 年月(YYYY-MM) |
| total_quota | DECIMAL(12,2) | NOT NULL | 总配额(kWh) |
| used_quota | DECIMAL(12,2) | DEFAULT 0 | 已用配额(kWh) |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| | | UNIQUE(workshop_id, year_month) | 车间+月份唯一 |

#### attendance_records - 考勤排班表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 考勤ID |
| user_id | BIGINT | FK→users, NOT NULL | 用户ID |
| attendance_date | DATE | NOT NULL | 考勤日期 |
| shift_type | ENUM('day','night','rest') | DEFAULT 'day' | 班次类型 |
| scheduled_start_time | TIME | | 计划上班时间 |
| scheduled_end_time | TIME | | 计划下班时间 |
| clock_in_time | TIME | | 实际上班打卡时间 |
| clock_out_time | TIME | | 实际下班打卡时间 |
| status | ENUM('normal','late','early_leave','absent','rest','holiday') | DEFAULT 'normal' | 考勤状态 |
| work_hours | DECIMAL(4,2) | | 工作时长(小时) |
| remark | VARCHAR(200) | | 备注 |
| | | UNIQUE(user_id, attendance_date) | 用户+日期唯一 |

#### operation_logs - 操作日志表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 日志ID |
| user_id | BIGINT | FK→users, NOT NULL | 操作人ID |
| operation_type | VARCHAR(50) | NOT NULL | 操作类型 |
| operation_desc | VARCHAR(500) | | 操作描述 |
| ip_address | VARCHAR(50) | | IP地址 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 操作时间 |


### 2.3 枚举定义

所有枚举类位于 `org.example.back.entity.enums` 包下：

| 枚举类 | 值 | 说明 |
|--------|-----|------|
| UserRole | ADMIN, DISPATCHER, INSPECTOR, MANAGER, WORKSHOP | 用户角色 |
| UserStatus | ACTIVE, DISABLED | 用户状态 |
| ApplicationStatus | PENDING, APPROVED, REJECTED, ADJUSTED | 申请状态 |
| Urgency | NORMAL, URGENT, CRITICAL | 紧急程度 |
| TaskType | INSPECTION, REPAIR, MAINTENANCE | 任务类型 |
| TaskStatus | PENDING, IN_PROGRESS, COMPLETED | 任务状态 |
| Priority | LOW, NORMAL, HIGH, URGENT | 优先级 |
| EquipmentStatus | NORMAL, WARNING, FAULT | 设备状态 |
| TimePeriodType | PEAK, NORMAL, VALLEY | 电价时段 |
| NotificationType | APPROVAL, TASK, ALERT, SYSTEM | 通知类型 |
| ShiftType | DAY, NIGHT, REST | 班次类型 |
| AttendanceStatus | NORMAL, LATE, EARLY_LEAVE, ABSENT, REST, HOLIDAY | 考勤状态 |

---

## 3. 后端API设计

### 3.1 统一响应格式

```java
public class Result<T> {
    int code;        // 200成功, 401未登录, 500错误
    String message;  // 提示信息
    T data;          // 业务数据
    long timestamp;  // 时间戳
}
```

分页响应使用 MyBatis-Plus 的 `IPage<T>`，包含 records、total、current、size、pages 字段。

### 3.2 认证机制

使用 HttpSession 进行会话管理：
- 登录成功后将 User 对象存入 session
- 每个接口通过 `session.getAttribute("user")` 获取当前用户
- 前端 Axios 配置 `withCredentials: true` 携带 Cookie
- 未登录返回 401 状态码，前端拦截器自动跳转登录页

### 3.3 API端点清单

#### 3.3.1 用户模块 - UserController (`/users`) [controller/common]

| 方法 | 路径 | 说明 | 对应需求 |
|------|------|------|----------|
| POST | /users/login | 用户登录 | 1.1 |
| POST | /users/register | 用户注册 | 1.2 |
| POST | /users/logout | 用户登出 | 1.1 |
| GET | /users/current | 获取当前用户信息 | 1.1 |
| PUT | /users/current | 更新个人信息(phone/email) | 2.6/3.7/4.4/5.8/6.6 |
| POST | /users/current/avatar | 上传头像 | 2.6/3.7/4.4/5.8/6.6 |
| GET | /users/current/avatar | 获取头像URL | 2.6/3.7/4.4/5.8/6.6 |
| GET | /users | 获取用户列表(支持角色筛选) | 5.4/6.2 |
| GET | /users/{id} | 获取用户详情 | 5.4/6.2 |
| GET | /users/current/stats | 获取任务统计(巡检员) | 4.1 |
| GET | /users/current/skills | 获取技能认证列表 | 4.1 |
| GET | /users/current/schedule | 获取排班信息 | 1.3 |
| GET | /users/current/work-stats | 获取工作量统计 | 4.1 |

#### 3.3.2 用电申请模块 (`/applications`) [controller/workshop + controller/dispatcher]

> 车间用户通过 WorkshopApplicationController 提交申请，调度员通过 ApplicationController 审批

| 方法 | 路径 | 说明 | 对应需求 | 所属Controller |
|------|------|------|----------|----------------|
| POST | /applications | 提交用电申请 | 2.2 | workshop/WorkshopApplicationController |
| GET | /applications/my | 获取我的申请列表 | 2.2 | workshop/WorkshopApplicationController |
| GET | /applications/pending | 获取待审批列表(调度员) | 3.2 | dispatcher/ApplicationController |
| GET | /applications | 获取申请列表(按角色返回) | 3.2/5.2 | dispatcher/ApplicationController |
| GET | /applications/{id} | 获取申请详情 | 2.2/3.2 | common（两个Controller共用） |
| PUT | /applications/{id}/approve | 批准申请 | 3.2 | dispatcher/ApplicationController |
| PUT | /applications/{id}/reject | 拒绝申请 | 3.2 | dispatcher/ApplicationController |
| PUT | /applications/{id}/adjust | 调整并批准 | 3.2 | dispatcher/ApplicationController |

#### 3.3.3 能耗数据模块 - EnergyDataController (`/energy-data`) [controller/dispatcher]

> 能耗监控和预警主要由调度员使用，车间用户通过 WorkshopEnergyController 查看自己车间的能耗

| 方法 | 路径 | 说明 | 对应需求 |
|------|------|------|----------|
| GET | /energy-data/today | 获取今日能耗曲线(按小时) | 2.3/3.3 |
| GET | /energy-data/range | 获取日期范围能耗数据 | 2.3/5.2 |
| GET | /energy-data/workshop-stats | 获取各车间能耗统计 | 3.3/5.1/5.2 |
| GET | /energy-data/alerts | 获取实时预警数据 | 3.4 |

#### 3.3.4 工单模块 - TaskController (`/tasks`) [controller/dispatcher]

> 工单创建和派单由调度员操作，巡检员通过 MyTaskController 查看和完成自己的任务

| 方法 | 路径 | 说明 | 对应需求 |
|------|------|------|----------|
| POST | /tasks | 创建工单 | 3.5 |
| GET | /tasks | 分页查询工单列表 | 3.5 |
| GET | /tasks/{id} | 获取工单详情 | 3.5/4.2 |
| POST | /tasks/{id}/assign | 派单给巡检员 | 3.5 |
| POST | /tasks/{id}/complete | 完成工单(提交报告) | 4.2 |
| GET | /tasks/my | 获取我的工单列表(巡检员) | 4.2 |
| GET | /tasks/today | 获取今日待完成任务 | 4.1 |
| GET | /tasks/my/stats | 获取任务统计 | 4.1 |

> 注：`/tasks/my`、`/tasks/today`、`/tasks/my/stats`、`/tasks/{id}/complete` 由 `inspector/MyTaskController` 提供，其余由 `dispatcher/TaskController` 提供

#### 3.3.5 反馈模块 (`/feedback`) [controller/workshop + controller/dispatcher]

> 车间用户通过 FeedbackController 提交/撤回反馈，调度员通过 FeedbackHandleController 处理反馈

| 方法 | 路径 | 说明 | 对应需求 | 所属Controller |
|------|------|------|----------|----------------|
| POST | /feedback | 提交反馈 | 2.4 | workshop/FeedbackController |
| GET | /feedback/my | 获取我的反馈列表 | 2.4 | workshop/FeedbackController |
| GET | /feedback/all | 获取所有反馈(调度员) | 3.5 | dispatcher/FeedbackHandleController |
| GET | /feedback/{id} | 获取反馈详情 | 2.4/3.5 | common（两个Controller共用） |
| POST | /feedback/{id}/withdraw | 撤回反馈 | 2.4 | workshop/FeedbackController |
| POST | /feedback/{id}/handle | 处理反馈 | 3.5 | dispatcher/FeedbackHandleController |

#### 3.3.6 通知模块 - NotificationController (`/notification`) [controller/common]

| 方法 | 路径 | 说明 | 对应需求 |
|------|------|------|----------|
| GET | /notification/unread-count | 获取未读消息数量 | 2.5/3.6/4.3/5.7/6.5 |
| GET | /notification/my | 获取我的消息列表 | 2.5/3.6/4.3/5.7/6.5 |
| POST | /notification/{id}/read | 标记单条已读 | 2.5/3.6/4.3/5.7/6.5 |
| POST | /notification/read-all | 标记全部已读 | 2.5/3.6/4.3/5.7/6.5 |

#### 3.3.7 设备模块 - EquipmentController (`/equipments`) [controller/workshop]

| 方法 | 路径 | 说明 | 对应需求 |
|------|------|------|----------|
| GET | /equipments | 获取设备列表(支持车间筛选) | 2.2/4.2 |
| GET | /equipments/{id} | 获取设备详情 | 4.2 |

#### 3.3.8 考勤模块 - AttendanceController (`/attendance`) [controller/common]

| 方法 | 路径 | 说明 | 对应需求 |
|------|------|------|----------|
| POST | /attendance/clock | 打卡(type=in/out) | 1.3 |
| GET | /attendance/today | 获取今日考勤 | 1.3 |
| GET | /attendance/monthly | 获取月度考勤记录 | 1.3 |
| GET | /attendance/stats | 获取月度考勤统计 | 1.3 |
| GET | /attendance/schedule/weekly | 获取本周排班 | 1.3 |

#### 3.3.9 AI分析模块 - AiController (`/ai`) [controller/manager]

| 方法 | 路径 | 说明 | 对应需求 |
|------|------|------|----------|
| GET(SSE) | /ai/chat/stream | 流式AI对话 | 5.3 |
| GET(SSE) | /ai/analyze-energy/stream | 流式能耗分析(快速指令，type=overview/peak-valley/anomaly/saving/forecast) | 5.3 |


### 3.4 待实现API（需求中有但代码未实现）

| 模块 | 端点 | 说明 | 对应需求 | 所属Controller包 |
|------|------|------|----------|------------------|
| 评论 | POST /comments | 发表评论 | 1.4/2.2/3.2 | controller/common/ |
| 评论 | GET /comments?relatedType=&relatedId= | 获取评论列表 | 1.4/2.2/3.2 | controller/common/ |
| 反馈图片 | POST /feedback/{id}/images | 上传反馈图片 | 2.4 | controller/workshop/ |
| 反馈图片 | GET /feedback/{id}/images | 获取反馈图片列表 | 2.4/3.5 | controller/workshop/ |
| 反馈派单 | POST /feedback/{id}/dispatch | 反馈转工单(调度员) | 3.5 | controller/dispatcher/ |
| 用户管理 | POST /users | 添加用户(经理/管理员) | 5.4/6.2 | controller/manager/ + controller/admin/ |
| 用户管理 | PUT /users/{id} | 编辑用户 | 5.4/6.2 | controller/manager/ + controller/admin/ |
| 用户管理 | PUT /users/{id}/status | 启用/禁用用户 | 5.4/6.2 | controller/manager/ + controller/admin/ |
| 排班管理 | POST /attendance/schedule/generate | 自动生成月度排班 | 5.5 | controller/manager/ |
| 排班管理 | GET /attendance/schedule/all | 获取所有人排班(经理) | 5.5 | controller/manager/ |
| 排班管理 | PUT /attendance/schedule/{id} | 调整排班 | 5.5 | controller/manager/ |
| 技能认证 | POST /skills/apply | 提交技能认证申请 | 4.1 | controller/inspector/ |
| 技能认证 | GET /skills/pending | 获取待审核列表(经理) | 5.6 | controller/manager/ |
| 技能认证 | PUT /skills/{id}/review | 审核技能认证 | 5.6 | controller/manager/ |
| 系统配置 | GET /config | 获取系统配置 | 6.3 | controller/admin/ |
| 系统配置 | PUT /config | 更新系统配置 | 6.3 | controller/admin/ |
| 操作日志 | GET /logs | 获取操作日志列表 | 6.4 | controller/admin/ |
| 管理员首页 | GET /admin/dashboard | 管理员首页统计 | 6.1 | controller/admin/ |
| 车间能耗 | GET /energy-data/my-workshop | 获取本车间能耗数据 | 2.3 | controller/workshop/ |
| 经理分析 | GET /energy-data/summary | 获取全局能耗汇总 | 5.1/5.2 | controller/manager/ |
| 考勤管理 | GET /attendance/all | 获取所有人员考勤记录(经理) | 5.7 | controller/manager/ |
| 考勤管理 | GET /attendance/daily-summary | 获取每日考勤汇总统计 | 5.7 | controller/manager/ |
| 考勤管理 | PUT /attendance/{id}/supplement | 补卡操作(经理) | 5.7 | controller/manager/ |
| 考勤管理 | GET /attendance/monthly-report | 获取月度考勤统计报表 | 5.7 | controller/manager/ |

---

## 4. 前端设计

### 4.1 项目结构

```
front/src/
├── api/                    # API请求模块
│   ├── request.js          # Axios实例配置(baseURL=/api, withCredentials)
│   ├── auth.js             # 登录/注册/登出
│   ├── application.js      # 用电申请
│   ├── energy.js           # 能耗数据
│   ├── task.js             # 工单管理
│   ├── feedback.js         # 问题反馈
│   ├── notification.js     # 消息通知
│   ├── equipment.js        # 设备管理
│   ├── attendance.js       # 考勤打卡
│   ├── user.js             # 用户管理
│   ├── ai.js               # AI分析
│   └── inspection.js       # 巡检相关
├── layouts/                # 布局组件（每角色一个）
│   ├── BaseLayout.vue      # 公共基础布局（侧边栏+顶栏+内容区）
│   ├── WorkshopLayout.vue
│   ├── DispatcherLayout.vue
│   ├── InspectorLayout.vue
│   ├── ManagerLayout.vue
│   └── AdminLayout.vue
├── router/
│   └── index.js            # 路由配置 + 路由守卫
├── stores/
│   └── user.js             # 用户状态管理(Pinia)
├── styles/                 # SCSS样式（按角色分文件）
│   ├── common.scss
│   ├── workshop.scss
│   ├── dispatcher.scss
│   ├── inspector.scss
│   ├── manager.scss
│   └── admin.scss
└── views/                  # 页面视图（按角色分目录）
    ├── Login.vue
    ├── common/
    │   └── Message.vue     # 公共消息中心页面
    ├── workshop/
    │   ├── Index.vue       # 首页概览
    │   ├── Apply.vue       # 用电申请
    │   ├── Energy.vue      # 能耗查看
    │   ├── Feedback.vue    # 故障报修
    │   └── Profile.vue     # 个人中心
    ├── dispatcher/
    │   ├── Index.vue       # 工作台
    │   ├── Approval.vue    # 申请审批
    │   ├── Dispatch.vue    # 电力调度
    │   ├── Alert.vue       # 预警处理
    │   ├── Order.vue       # 工单管理
    │   ├── Tasks.vue       # 任务列表
    │   └── Profile.vue     # 个人中心
    ├── inspector/
    │   ├── Index.vue       # 工作台
    │   ├── Equipment.vue   # 我的任务
    │   ├── Inspection.vue  # 巡检详情
    │   ├── Message.vue     # 消息中心
    │   └── Profile.vue     # 个人中心
    ├── manager/
    │   ├── Index.vue       # 首页概览
    │   ├── Analysis.vue    # 数据分析
    │   ├── AI.vue          # AI分析
    │   └── Profile.vue     # 个人中心
    └── admin/
        ├── Index.vue       # 首页概览
        ├── Users.vue       # 用户管理
        ├── Config.vue      # 系统配置
        ├── Logs.vue        # 操作日志
        └── Profile.vue     # 个人中心
```

### 4.2 路由配置

```
/login                          → Login.vue
/workshop                       → WorkshopLayout
  ├── /                         → Index.vue (首页概览)
  ├── /apply                    → Apply.vue (用电申请)
  ├── /energy                   → Energy.vue (能耗查看)
  ├── /feedback                 → Feedback.vue (故障报修)
  ├── /message                  → common/Message.vue (消息中心)
  └── /profile                  → Profile.vue (个人中心)
/dispatcher                     → DispatcherLayout
  ├── /                         → Index.vue (工作台)
  ├── /approval                 → Approval.vue (申请审批)
  ├── /dispatch                 → Dispatch.vue (电力调度)
  ├── /alert                    → Alert.vue (预警处理)
  ├── /order                    → Order.vue (工单管理)
  ├── /tasks                    → Tasks.vue (任务列表)
  ├── /message                  → common/Message.vue (消息通知)
  └── /profile                  → Profile.vue (个人中心)
/inspector                      → InspectorLayout
  ├── /                         → Index.vue (工作台)
  ├── /equipment                → Equipment.vue (我的任务)
  ├── /inspection               → Inspection.vue (巡检详情)
  ├── /message                  → Message.vue (消息中心)
  └── /profile                  → Profile.vue (个人中心)
/manager                        → ManagerLayout
  ├── /                         → Index.vue (首页概览)
  ├── /analysis                 → Analysis.vue (数据分析)
  ├── /ai                       → AI.vue (AI分析)
  ├── /users (待新增)            → Users.vue (用户管理，含技能认证审核)
  ├── /schedule (待新增)         → Schedule.vue (排班考勤管理)
  ├── /message                  → common/Message.vue (消息中心)
  └── /profile                  → Profile.vue (个人中心)
/admin                          → AdminLayout
  ├── /                         → Index.vue (首页概览)
  ├── /users                    → Users.vue (用户管理)
  ├── /config                   → Config.vue (系统配置)
  ├── /logs                     → Logs.vue (操作日志)
  ├── /message (待新增)          → common/Message.vue (消息中心)
  └── /profile                  → Profile.vue (个人中心)
```

### 4.3 状态管理

使用 Pinia 管理全局状态，当前仅有 `user` store：

```javascript
// stores/user.js
{
  user: Object | null,     // 当前登录用户信息
  isLoggedIn: Boolean,     // 是否已登录
  homePage: String,        // 角色对应首页路径
  doLogin(username, password),
  doLogout(),
  fetchCurrentUser()
}
```

### 4.4 前端请求配置

```javascript
// api/request.js
axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true  // 携带Session Cookie
})
// 响应拦截器：code !== 200 时 ElMessage.error，401 时跳转登录页
```

---

## 5. 关键业务逻辑设计

### 5.1 用电申请审批流程

```
1. 车间用户提交申请 → POST /applications
   - 生成申请编号: APP + yyyyMMdd + HHmmss + 序号
   - 初始状态: PENDING
   
2. 调度员审批 → PUT /applications/{id}/approve|reject|adjust
   - approve: status → APPROVED, 触发能耗数据生成
   - reject: status → REJECTED, 记录拒绝原因
   - adjust: status → ADJUSTED, 修改时段后触发能耗数据生成
   - 审批后发送通知给申请人

3. 能耗数据生成（审批通过后自动触发）
   - 按申请时段的每个小时生成一条 energy_data 记录
   - 根据小时判断时段类型:
     - 峰时(peak): 8-12点, 18-22点 → 1.2元/kWh
     - 平时(normal): 12-18点 → 0.8元/kWh
     - 谷时(valley): 22-8点 → 0.4元/kWh
   - 功率 = 申请功率 × (0.85~1.05随机波动)
   - 电量 = 功率 × 1小时
   - 费用 = 电量 × 电价
```

### 5.2 工单派单流程

```
1. 调度员创建工单 → POST /tasks
   - 状态: PENDING
   
2. 调度员派单 → POST /tasks/{id}/assign?assigneeId=xxx
   - 状态: PENDING → IN_PROGRESS（直接跳过，无需巡检员接单）
   - 发送任务通知给巡检员

3. 巡检员完成 → POST /tasks/{id}/complete?report=xxx
   - 状态: IN_PROGRESS → COMPLETED
   - 记录完成时间
```

### 5.3 反馈处理与派单流程

```
1. 车间用户提交反馈 → POST /feedback
   - 生成反馈编号: FB + yyyyMMdd + 序号
   - 支持上传图片附件（MinIO存储）
   - 状态: PENDING

2a. 调度员直接回复 → POST /feedback/{id}/handle
    - 状态: PENDING → RESOLVED
    
2b. 调度员派单给巡检员 → POST /feedback/{id}/dispatch（待实现）
    - 自动创建维修任务(task_type=REPAIR)
    - 任务关联反馈ID
    - 反馈状态: PENDING → PROCESSING
    - 巡检员完成维修后，反馈状态 → RESOLVED
```

### 5.4 预警检测逻辑

```
基于能耗数据实时分析:
1. 功率超限预警: 车间当前功率 > 车间限额 → CRITICAL
2. 功率接近限额: 车间当前功率 > 限额×85% → WARNING
3. 异常波动检测: 平均功率低但峰值高 → WARNING
```

### 5.5 AI分析流程

```
1. 经理点击快速操作指令 → GET /ai/analyze-energy/stream?type=xxx
   - 支持5种快速指令类型：
     a. overview    — 能耗总览分析（各车间今日/本月用电量、费用、整体评估）
     b. peak-valley — 峰谷时段分析（峰/平/谷用电占比、错峰建议）
     c. anomaly     — 异常用电检测（功率异常波动、超限、定位问题车间）
     d. saving      — 节能优化建议（基于历史数据的降本增效建议）
     e. forecast    — 用电趋势预测（近期数据预测、未来预警）
   - 后端根据type获取对应维度的能耗统计数据
   - 构建针对性分析提示词（包含实际数据）
   - 通过 OkHttp WebSocket 连接讯飞星火
   - 流式返回分析结果（SSE）

2. 经理自由对话 → GET /ai/chat/stream?message=xxx
   - 直接将用户消息发送给AI
   - 流式返回回复（SSE）
```

### 5.6 考勤与排班逻辑

```
1. 自动排班（按月生成）:
   - 工作日: 默认白班(08:30-17:30)
   - 周末: 默认休息
   - 巡检员: 白班/夜班轮换
   - 经理不参与排班

2. 打卡判定:
   - 上班打卡时间 > 计划上班时间 → 迟到(LATE)
   - 下班打卡时间 < 计划下班时间 → 早退(EARLY_LEAVE)
   - 当日无打卡记录 → 缺勤(ABSENT)
   - 工作时长 = 下班时间 - 上班时间
```

### 5.7 文件存储设计

```
MinIO配置:
  endpoint: http://127.0.0.1:9090
  bucket: avatars (自动创建，公开读取)
  
文件命名: {userId}_{timestamp}.{ext}
访问方式: 预签名URL（有效期7天）
文件限制: 最大15MB，仅限图片格式

使用场景:
  - 用户头像上传/更换/删除
  - 反馈图片上传（待实现多图支持）
  - 技能认证证书上传（待实现）
```

---

## 6. 后端分层架构

### 6.1 包结构

```
org.example.back/
├── common/                     # 通用类
│   ├── Result.java             # 统一响应
│   └── PageResult.java         # 分页响应
├── config/                     # 配置类
│   ├── CorsConfig.java         # CORS跨域
│   ├── MinioConfig.java        # MinIO配置
│   ├── MyBatisPlusConfig.java  # 分页插件+自动填充
│   └── AiConfig.java           # 讯飞星火配置
├── controller/                 # 控制器（按角色分包）
│   ├── common/                 # 公共接口（所有角色共用）
│   │   ├── UserController      # 用户认证、个人信息、头像
│   │   ├── NotificationController # 消息通知（所有角色）
│   │   ├── AttendanceController # 考勤打卡（所有角色）
│   │   └── CommentController    # 业务评论（待创建）
│   ├── admin/                  # 系统管理员接口
│   │   ├── AdminUserController  # 用户管理(CRUD)
│   │   ├── SystemConfigController # 系统配置
│   │   └── OperationLogController # 操作日志
│   ├── dispatcher/             # 调度员接口
│   │   ├── ApplicationController # 用电申请审批、申请列表
│   │   ├── TaskController       # 工单创建、派单、管理
│   │   ├── EnergyDataController # 能耗监控、预警数据
│   │   └── FeedbackHandleController # 反馈处理、反馈转工单（待创建）
│   ├── inspector/              # 巡检员接口（待创建）
│   │   ├── MyTaskController     # 我的任务、完成工单
│   │   └── SkillController      # 技能认证申请（待创建）
│   ├── manager/                # 经理接口
│   │   ├── AiController         # AI智能分析（流式对话+一键分析）
│   │   ├── ManagerAnalysisController # 数据分析、能耗统计（待创建）
│   │   ├── ManagerUserController # 用户管理+技能认证审核(经理)（待创建）
│   │   └── ScheduleAttendanceController # 排班考勤管理（排班+查看考勤+补卡）（待创建）
│   └── workshop/               # 车间接口
│       ├── WorkshopApplicationController # 提交申请、我的申请
│       ├── WorkshopEnergyController # 能耗查看（待创建）
│       ├── FeedbackController   # 故障报修/问题反馈
│       └── EquipmentController  # 设备查询（车间选设备+巡检员查设备）
├── dto/                        # 数据传输对象
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── RegisterRequest.java
│   ├── UserDTO.java
│   ├── ApplicationRequest.java
│   ├── ApprovalRequest.java
│   └── AttendanceRecordDTO.java
├── entity/                     # 实体类
│   ├── enums/                  # 枚举（12个）
│   ├── User.java
│   ├── Application.java
│   ├── EnergyData.java
│   ├── Task.java
│   ├── Feedback.java
│   ├── Notification.java
│   ├── Comment.java
│   ├── Equipment.java
│   ├── Quota.java
│   ├── AttendanceRecord.java
│   └── OperationLog.java
├── exception/                  # 异常处理
│   ├── BusinessException.java
│   ├── ErrorCode.java
│   └── GlobalExceptionHandler.java
├── mapper/                     # MyBatis-Plus Mapper（按角色分包）
│   ├── common/                 # NotificationMapper, EquipmentMapper, UserMapper, AttendanceMapper, CommentMapper, EnergyDataMapper, QuotaMapper
│   ├── admin/                  # OperationLogMapper
│   ├── dispatcher/             # TaskMapper
│   ├── inspector/              # （复用common或dispatcher中的Mapper）
│   ├── manager/                # ScheduleMapper, CertificationMapper（待创建）
│   └── workshop/               # ApplicationMapper, FeedbackMapper
├── service/                    # 服务层（按角色分包）
│   ├── common/                 # 公共服务（多角色共用）
│   │   ├── UserService          # 用户认证、信息管理
│   │   ├── EnergyDataService    # 能耗数据查询与统计（车间/调度员/经理共用）
│   │   ├── EnergyDataGenerator  # 能耗数据自动生成（审批通过后触发）
│   │   ├── MinioService         # 文件存储（头像/图片上传）
│   │   ├── EquipmentService     # 设备查询（车间选设备+巡检员查设备）
│   │   ├── NotificationService  # 消息通知（所有角色）
│   │   ├── AttendanceService    # 考勤打卡（所有角色）
│   │   └── CommentService       # 业务评论（待创建，申请/任务评论交流）
│   ├── admin/                  # 管理员服务（待创建）
│   │   ├── SystemConfigService  # 系统配置
│   │   └── OperationLogService  # 操作日志
│   ├── dispatcher/             # 调度员服务
│   │   ├── TaskService          # 工单创建、派单、管理
│   │   ├── ApplicationApprovalService # 用电申请审批（待创建）
│   │   ├── EnergyAlertService   # 能耗预警检测（待创建）
│   │   └── FeedbackHandleService # 反馈处理、反馈转工单（待创建）
│   ├── inspector/              # 巡检员服务（待创建）
│   │   ├── InspectorTaskService # 我的任务、完成工单
│   │   └── SkillService         # 技能认证申请
│   ├── manager/                # 经理服务
│   │   ├── AiService            # 讯飞星火AI（流式对话+一键分析）
│   │   ├── ScheduleAttendanceService # 排班考勤管理（排班+考勤查看+补卡）（待创建）
│   │   └── ManagerUserService   # 用户管理+技能认证审核-经理（待创建）
│   └── workshop/               # 车间服务
│       ├── ApplicationService   # 用电申请提交
│       └── FeedbackService      # 故障报修
└── util/                       # 工具类
    ├── EnergyCalculator.java    # 能耗计算工具
    └── EnergyCalculationResult.java
```

### 6.2 现有代码与目标结构差异

#### 6.2.1 包结构差异总览

| 层级 | 现有包 | 需新增包 | 说明 |
|------|--------|----------|------|
| controller | common/, dispatcher/, manager/, workshop/ | admin/, inspector/ | 管理员和巡检员缺少独立controller包 |
| service | common/, dispatcher/, workshop/ | admin/, inspector/, manager/ | 管理员、巡检员、经理缺少独立service包 |
| mapper | common/, dispatcher/ + 根目录散落 | admin/, inspector/, manager/, workshop/ | 根目录的Mapper需归入对应包；新增角色包 |

#### 6.2.2 Mapper迁移计划

根目录散落的Mapper需迁入对应角色包：

| 现有位置 | 目标位置 | 说明 |
|----------|----------|------|
| `mapper/UserMapper` | `mapper/common/UserMapper` | 多角色共用 |
| `mapper/AttendanceMapper` | `mapper/common/AttendanceMapper` | 多角色共用 |
| `mapper/CommentMapper` | `mapper/common/CommentMapper` | 多角色共用 |
| `mapper/EnergyDataMapper` | `mapper/common/EnergyDataMapper` | 多角色共用 |
| `mapper/QuotaMapper` | `mapper/common/QuotaMapper` | 多角色共用 |
| `mapper/OperationLogMapper` | `mapper/admin/OperationLogMapper` | 管理员专用 |

#### 6.2.3 Service迁移与新增计划

| 变更类型 | 类名 | 现有位置 | 目标位置 | 说明 |
|----------|------|----------|----------|------|
| 迁移 | AiService | service/common/ | service/manager/ | AI分析为经理专属功能 |
| 新增 | CommentService | — | service/common/ | 评论交流功能（申请/任务） |
| 新增 | ApplicationApprovalService | — | service/dispatcher/ | 调度员审批申请 |
| 新增 | EnergyAlertService | — | service/dispatcher/ | 能耗预警检测 |
| 新增 | FeedbackHandleService | — | service/dispatcher/ | 反馈处理与转工单 |
| 新增 | InspectorTaskService | — | service/inspector/ | 巡检员我的任务 |
| 新增 | SkillService | — | service/inspector/ | 技能认证申请 |
| 新增 | ManagerUserService | — | service/manager/ | 经理用户管理 |
| 新增 | ScheduleService | — | service/manager/ | 排班管理 |
| 新增 | CertificationService | — | service/manager/ | 技能认证审核 |
| 新增 | SystemConfigService | — | service/admin/ | 系统配置 |
| 新增 | OperationLogService | — | service/admin/ | 操作日志 |

#### 6.2.4 Controller新增计划

| 变更类型 | 类名 | 目标包 | 说明 |
|----------|------|--------|------|
| 新增 | CommentController | controller/common/ | 评论交流 |
| 新增 | AdminUserController | controller/admin/ | 管理员用户管理 |
| 新增 | SystemConfigController | controller/admin/ | 系统配置 |
| 新增 | OperationLogController | controller/admin/ | 操作日志 |
| 新增 | FeedbackHandleController | controller/dispatcher/ | 反馈处理 |
| 新增 | MyTaskController | controller/inspector/ | 我的任务 |
| 新增 | SkillController | controller/inspector/ | 技能认证申请 |
| 新增 | ManagerAnalysisController | controller/manager/ | 数据分析 |
| 新增 | ManagerUserController | controller/manager/ | 经理用户管理 |
| 新增 | ScheduleController | controller/manager/ | 排班管理 |
| 新增 | CertificationController | controller/manager/ | 技能认证审核 |
| 新增 | WorkshopEnergyController | controller/workshop/ | 车间能耗查看 |

#### 6.2.5 现有Controller需迁移/重命名

| 现有类 | 现有包 | 目标包 | 变更说明 |
|--------|--------|--------|----------|
| ApplicationController | controller/common/ | controller/dispatcher/ | 审批功能属于调度员，需拆分 |
| EnergyDataController | controller/common/ | controller/dispatcher/ | 能耗监控/预警属于调度员 |
| EquipmentController | controller/common/ | controller/workshop/ | 设备查询主要由车间和巡检员使用 |

> 注：ApplicationController 涉及车间提交和调度员审批两个角色，需拆分为 `workshop/WorkshopApplicationController`（提交申请）和 `dispatcher/ApplicationController`（审批申请），共用底层 ApplicationService。

---

## 7. 正确性属性

### 7.1 用户认证属性

- P1.1: 使用正确的用户名和密码登录，返回code=200且data包含userId、username、role
- P1.2: 使用错误密码登录，返回错误码且不创建session
- P1.3: 已禁用用户登录，返回USER_DISABLED错误码(1003)
- P1.4: 注册时用户名已存在，返回USERNAME_EXISTS错误码(1004)

### 7.2 用电申请属性

- P2.1: 提交申请后，申请状态为PENDING，且生成唯一申请编号
- P2.2: 批准申请后，状态变为APPROVED，且自动生成对应时段的能耗数据
- P2.3: 拒绝申请后，状态变为REJECTED，不生成能耗数据
- P2.4: 调整申请后，状态变为ADJUSTED，按调整后时段生成能耗数据
- P2.5: 已处理的申请不能重复审批

### 7.3 能耗数据属性

- P3.1: 生成的能耗数据时段类型与小时对应关系正确（8-12/18-22→peak, 12-18→normal, 22-8→valley）
- P3.2: 电价与时段类型对应正确（peak→1.2, normal→0.8, valley→0.4）
- P3.3: 费用 = 电量 × 电价（精度误差≤0.01）
- P3.4: 电量 = 功率 × 1小时

### 7.4 工单管理属性

- P4.1: 创建工单后状态为PENDING
- P4.2: 派单后状态直接变为IN_PROGRESS（不经过ASSIGNED状态）
- P4.3: 完成工单后状态为COMPLETED，且记录完成时间
- P4.4: 只有被分配的巡检员才能完成该工单

### 7.5 反馈管理属性

- P5.1: 提交反馈后状态为PENDING，且生成唯一反馈编号
- P5.2: 只有PENDING状态的反馈可以撤回
- P5.3: 只能撤回自己提交的反馈
- P5.4: 撤回后状态变为WITHDRAWN

### 7.6 考勤属性

- P6.1: 上班打卡时间晚于计划时间 → 状态为LATE
- P6.2: 下班打卡时间早于计划时间 → 状态为EARLY_LEAVE
- P6.3: 工作时长 = 下班时间 - 上班时间（精度到分钟）

---

## 8. 测试策略

### 8.1 测试框架

- 单元测试: JUnit 5
- 属性测试: jqwik 1.8.2
- 测试数据库: H2（内存模式）

### 8.2 属性测试重点

| 属性 | 测试内容 | 框架 |
|------|----------|------|
| P3.1 | 时段类型判定：任意小时(0-23)映射到正确的时段类型 | jqwik |
| P3.2 | 电价计算：任意时段类型映射到正确电价 | jqwik |
| P3.3 | 费用计算：任意电量和电价，费用=电量×电价 | jqwik |
| P2.5 | 审批幂等性：已处理申请重复审批应抛出异常 | jqwik |
| P5.2 | 撤回前置条件：非PENDING状态的反馈撤回应抛出异常 | jqwik |
| P6.1-P6.3 | 考勤状态判定：任意打卡时间和计划时间，状态判定正确 | jqwik |

### 8.3 测试文件位置

```
back/src/test/java/org/example/back/
├── service/
│   ├── EnergyDataServiceTest.java    # 能耗数据生成测试
│   ├── ApplicationServiceTest.java   # 申请审批流程测试
│   ├── AttendanceServiceTest.java    # 考勤判定测试
│   └── FeedbackServiceTest.java      # 反馈管理测试
└── property/
    ├── EnergyDataPropertyTest.java   # 能耗数据属性测试
    ├── AttendancePropertyTest.java   # 考勤属性测试
    └── ApplicationPropertyTest.java  # 申请属性测试
```