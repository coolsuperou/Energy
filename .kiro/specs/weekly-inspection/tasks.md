# 实施任务：每周设备巡检

## 任务列表

### 任务 1：数据库表创建
- [x] 在 `schema.sql` 末尾添加 `inspection_plans` 和 `inspection_records` 建表语句
- [x] 提供 ALTER TABLE SQL 供用户手动执行

### 任务 2：后端实体类
- [x] 创建 `InspectionPlan` 实体类 → `org.example.back.entity`
- [x] 创建 `InspectionRecord` 实体类 → `org.example.back.entity`

### 任务 3：后端 Mapper 层
- [x] 创建 `InspectionPlanMapper` → `org.example.back.mapper.dispatcher`
- [x] 创建 `InspectionRecordMapper` → `org.example.back.mapper.inspector`

### 任务 4：后端 Service 层 - 调度员排班
- [x] 创建 `InspectionScheduleService` → `org.example.back.service.dispatcher`
- [x] 实现 getWeekPlans、addPlan（含自动生成记录）、deletePlan、autoSchedule、getPlanRecords

### 任务 5：后端 Service 层 - 巡检员执行
- [x] 创建 `InspectionService` → `org.example.back.service.inspector`
- [x] 实现 getMyWeekPlans、getPlanRecordsByEquipment、updateCheckResult、convertToRepair

### 任务 6：后端 Controller 层
- [x] 创建 `InspectionScheduleController` → `org.example.back.controller.dispatcher`
- [x] 创建 `WeeklyInspectionController` → `org.example.back.controller.inspector`

### 任务 7：前端 API 模块
- [x] 更新 `front/src/api/inspection.js`，添加调度员排班和巡检员巡检的 API 方法

### 任务 8：前端 - 调度员巡检排班页面
- [x] 创建 `front/src/views/dispatcher/InspectionSchedule.vue`
- [x] 实现周选择器、排班表格、添加排班弹窗、自动排班、详情查看

### 任务 9：前端 - 巡检员每周巡检页面
- [x] 改造 `front/src/views/inspector/Inspection.vue`
- [x] 实现计划列表、设备卡片、检查项操作、异常转报修

### 任务 10：前端路由和导航集成
- [x] 更新 `front/src/router/index.js` 添加新路由
- [x] 更新 `InspectorLayout.vue` 添加"每周巡检"菜单项
- [x] 更新 `DispatcherLayout.vue` 添加"巡检排班"菜单项
