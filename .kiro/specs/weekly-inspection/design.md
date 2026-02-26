# 技术设计：每周设备巡检

## 概述

本设计基于需求文档，为电能管理系统新增每周设备巡检功能。涉及后端 2 个新数据库表、2 个实体类、2 个 Mapper、2 个 Service、2 个 Controller，前端 2 个新/改造页面、1 个 API 模块、2 个布局和路由更新。

## 数据库设计

### 新增表 1：inspection_plans（巡检计划表）

```sql
CREATE TABLE IF NOT EXISTS inspection_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '计划ID',
    inspector_id BIGINT NOT NULL COMMENT '巡检员用户ID',
    workshop_id BIGINT NOT NULL COMMENT '车间ID（对应 equipments.workshop_id）',
    week_start DATE NOT NULL COMMENT '周起始日期（周一）',
    status ENUM('pending', 'completed') DEFAULT 'pending' COMMENT '计划状态',
    remark VARCHAR(200) COMMENT '备注（如：该车间暂无设备）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (inspector_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_inspector_workshop_week (inspector_id, workshop_id, week_start),
    INDEX idx_week_start (week_start),
    INDEX idx_inspector (inspector_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡检计划表';
```

### 新增表 2：inspection_records（巡检记录表）

```sql
CREATE TABLE IF NOT EXISTS inspection_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    plan_id BIGINT NOT NULL COMMENT '关联巡检计划ID',
    equipment_id BIGINT NOT NULL COMMENT '设备ID',
    check_type ENUM('appearance', 'running', 'temperature', 'noise', 'electrical') NOT NULL COMMENT '检查类型',
    result ENUM('normal', 'abnormal', 'fault') DEFAULT NULL COMMENT '检查结果',
    remark VARCHAR(500) COMMENT '备注',
    repaired TINYINT(1) DEFAULT 0 COMMENT '是否已转报修',
    checked_at DATETIME COMMENT '检查时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (plan_id) REFERENCES inspection_plans(id) ON DELETE CASCADE,
    FOREIGN KEY (equipment_id) REFERENCES equipments(id) ON DELETE CASCADE,
    UNIQUE KEY uk_plan_equipment_check (plan_id, equipment_id, check_type),
    INDEX idx_plan (plan_id),
    INDEX idx_equipment (equipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡检记录表';
```

### 表关系

```
users (inspector) 1──N inspection_plans N──1 workshops (via workshop_id)
inspection_plans 1──N inspection_records N──1 equipments
inspection_records ──> feedbacks (异常转报修时创建)
```

## 后端设计

### 实体类

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `InspectionPlan` | `org.example.back.entity` | 巡检计划实体，映射 `inspection_plans` 表 |
| `InspectionRecord` | `org.example.back.entity` | 巡检记录实体，映射 `inspection_records` 表 |

#### InspectionPlan 字段

- `id` (Long) - 主键
- `inspectorId` (Long) - 巡检员ID
- `workshopId` (Long) - 车间ID
- `weekStart` (LocalDate) - 周起始日（周一）
- `status` (String) - pending/completed
- `remark` (String) - 备注
- `createdAt` (LocalDateTime) - 创建时间
- `updatedAt` (LocalDateTime) - 更新时间
- `inspectorName` (String) - 非DB字段，巡检员姓名
- `workshopName` (String) - 非DB字段，车间名称
- `checkedCount` (Integer) - 非DB字段，已检查设备数
- `totalCount` (Integer) - 非DB字段，总设备数

#### InspectionRecord 字段

- `id` (Long) - 主键
- `planId` (Long) - 关联计划ID
- `equipmentId` (Long) - 设备ID
- `checkType` (String) - 检查类型枚举
- `result` (String) - 检查结果
- `remark` (String) - 备注
- `repaired` (Boolean) - 是否已转报修
- `checkedAt` (LocalDateTime) - 检查时间
- `createdAt` (LocalDateTime) - 创建时间
- `equipmentName` (String) - 非DB字段，设备名称
- `equipmentLocation` (String) - 非DB字段，设备位置

### Mapper 层

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `InspectionPlanMapper` | `org.example.back.mapper.dispatcher` | 巡检计划 Mapper（调度员管理排班） |
| `InspectionRecordMapper` | `org.example.back.mapper.inspector` | 巡检记录 Mapper（巡检员执行检查） |

Mapper 均继承 `BaseMapper<T>`，使用 MyBatis-Plus 标准 CRUD。自定义 SQL 方法：

- `InspectionPlanMapper`:
  - `selectPlansWithNames(weekStart)` - 查询某周计划并关联巡检员姓名和车间名称
  - `selectPlansByInspector(inspectorId, weekStart)` - 查询某巡检员某周的计划

- `InspectionRecordMapper`:
  - `selectRecordsWithEquipment(planId)` - 查询某计划下所有记录并关联设备信息
  - `countCheckedByPlan(planId)` - 统计某计划已检查的设备数（至少有一项有结果的设备）

### Service 层

| 类名 | 包路径 | 说明 |
|------|--------|------|
| `InspectionScheduleService` | `org.example.back.service.dispatcher` | 调度员排班管理服务 |
| `InspectionService` | `org.example.back.service.inspector` | 巡检员巡检执行服务 |

#### InspectionScheduleService 核心方法

```java
// 获取某周排班列表（含进度信息）
List<InspectionPlan> getWeekPlans(LocalDate weekStart);

// 手动添加排班
InspectionPlan addPlan(Long inspectorId, Long workshopId, LocalDate weekStart);

// 删除排班（级联删除未完成记录）
void deletePlan(Long planId);

// 自动排班（均匀分配车间给巡检员）
List<InspectionPlan> autoSchedule(LocalDate weekStart);

// 获取某计划的巡检记录详情
List<InspectionRecord> getPlanRecords(Long planId);
```

#### InspectionService 核心方法

```java
// 获取当前巡检员本周的巡检计划
List<InspectionPlan> getMyWeekPlans(Long inspectorId);

// 获取某计划下按设备分组的巡检记录
Map<Long, List<InspectionRecord>> getPlanRecordsByEquipment(Long planId);

// 更新检查结果
void updateCheckResult(Long recordId, String result, String remark);

// 异常转报修
void convertToRepair(Long recordId, Long inspectorId);
```

### Controller 层

| 类名 | 包路径 | API 前缀 | 说明 |
|------|--------|----------|------|
| `InspectionScheduleController` | `org.example.back.controller.dispatcher` | `/api/inspection-schedule` | 调度员巡检排班接口 |
| `WeeklyInspectionController` | `org.example.back.controller.inspector` | `/api/weekly-inspection` | 巡检员每周巡检接口 |

#### InspectionScheduleController 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/inspection-schedule/plans` | 获取某周排班列表，参数 `weekStart` |
| POST | `/api/inspection-schedule/plans` | 添加排班，body: `{inspectorId, workshopId, weekStart}` |
| DELETE | `/api/inspection-schedule/plans/{id}` | 删除排班 |
| POST | `/api/inspection-schedule/auto` | 自动排班，参数 `weekStart` |
| GET | `/api/inspection-schedule/plans/{id}/records` | 查看某计划的巡检记录详情 |
| GET | `/api/inspection-schedule/inspectors` | 获取所有在职巡检员列表 |
| GET | `/api/inspection-schedule/workshops` | 获取所有车间列表 |

#### WeeklyInspectionController 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/weekly-inspection/plans` | 获取我的本周巡检计划 |
| GET | `/api/weekly-inspection/plans/{id}/records` | 获取某计划下的设备巡检记录 |
| PUT | `/api/weekly-inspection/records/{id}` | 更新检查结果，body: `{result, remark}` |
| POST | `/api/weekly-inspection/records/{id}/repair` | 异常转报修 |

## 前端设计

### API 模块

更新 `front/src/api/inspection.js`，新增以下方法：

```javascript
// === 调度员：巡检排班 ===
getInspectionPlans(weekStart)          // GET /inspection-schedule/plans
addInspectionPlan(data)                // POST /inspection-schedule/plans
deleteInspectionPlan(id)               // DELETE /inspection-schedule/plans/:id
autoScheduleInspection(weekStart)      // POST /inspection-schedule/auto
getInspectionPlanRecords(planId)       // GET /inspection-schedule/plans/:id/records
getInspectorList()                     // GET /inspection-schedule/inspectors
getWorkshopList()                      // GET /inspection-schedule/workshops

// === 巡检员：每周巡检 ===
getMyInspectionPlans()                 // GET /weekly-inspection/plans
getMyPlanRecords(planId)               // GET /weekly-inspection/plans/:id/records
updateInspectionRecord(id, data)       // PUT /weekly-inspection/records/:id
repairFromInspection(id)               // POST /weekly-inspection/records/:id/repair
```

### 页面组件

#### 1. 调度员 - 巡检排班页面（新建）

文件：`front/src/views/dispatcher/InspectionSchedule.vue`

功能：
- 周选择器（上一周/下一周/本周）
- 排班表格：行=巡检员，列=分配的车间，显示完成进度
- "添加排班"按钮 → 弹窗选择巡检员+车间
- "自动排班"按钮 → 确认后自动分配
- 点击某条计划 → 展开查看巡检记录详情
- 删除排班按钮（仅未完成的可删除）

UI 风格：与现有调度员页面一致，使用 Bootstrap 5 + Element Plus 组件

#### 2. 巡检员 - 每周巡检页面（改造现有 Inspection.vue）

文件：`front/src/views/inspector/Inspection.vue`（改造）

功能：
- 顶部显示本周日期范围和总体进度
- 左侧/顶部：巡检计划列表（按车间分组的卡片）
- 选择某个计划后，展示该车间下所有设备的巡检卡片
- 每台设备卡片包含 5 个检查项，每项有 正常/异常/故障 三个按钮
- 选择异常或故障时弹出备注输入框
- 设备卡片右上角显示"转报修"按钮（仅异常/故障时可用）
- 报修中的设备显示灰色不可操作状态

UI 风格：参考 `design/daily-inspection.html` 静态样板

### 路由更新

`front/src/router/index.js` 新增：

```javascript
// inspector children 新增
{ path: 'weekly-inspection', name: 'WeeklyInspection', component: () => import('@/views/inspector/Inspection.vue') }

// dispatcher children 新增
{ path: 'inspection-schedule', name: 'InspectionSchedule', component: () => import('@/views/dispatcher/InspectionSchedule.vue') }
```

注意：现有的 `inspection` 路由保留或重定向到 `weekly-inspection`。

### 侧边栏更新

- `InspectorLayout.vue`：在"我的任务"后面添加"每周巡检"菜单项，图标 `bi-clipboard2-check`，路由 `/inspector/weekly-inspection`
- `DispatcherLayout.vue`：在"问题反馈"后面添加"巡检排班"菜单项，图标 `bi-calendar2-week`，路由 `/dispatcher/inspection-schedule`

## 需求覆盖矩阵

| 需求 | 后端组件 | 前端组件 |
|------|----------|----------|
| 需求1：排班管理 | InspectionScheduleController + InspectionScheduleService | InspectionSchedule.vue |
| 需求2：自动排班 | InspectionScheduleService.autoSchedule() | InspectionSchedule.vue（自动排班按钮） |
| 需求3：任务自动生成 | InspectionScheduleService.addPlan()（创建计划时自动生成记录） | - |
| 需求4：巡检执行 | WeeklyInspectionController + InspectionService | Inspection.vue（改造） |
| 需求5：异常转报修 | InspectionService.convertToRepair() | Inspection.vue（转报修按钮） |
| 需求6：数据查询 | InspectionScheduleController.getPlanRecords() | InspectionSchedule.vue（详情展开） |
| 需求7：导航集成 | - | InspectorLayout.vue + DispatcherLayout.vue + router |
