# 需求文档：每周设备巡检

## 简介

每周设备巡检功能为电能管理系统新增按周排班的巡检管理能力。调度员按周为巡检员分配负责的车间，系统根据排班计划自动生成巡检任务，巡检员按分配完成设备检查。该功能涉及两个新数据库表（`inspection_plans`、`inspection_records`），调度员端新增"巡检排班"页面，巡检员端改造现有巡检页面为"每周巡检"页面。

## 术语表

- **Inspection_Plan（巡检计划）**: 一条排班记录，表示某巡检员在某周负责某车间的巡检任务，存储在 `inspection_plans` 表中
- **Inspection_Record（巡检记录）**: 一条设备检查明细，记录某台设备的某个检查项的检查结果，存储在 `inspection_records` 表中
- **Dispatcher（调度员）**: 负责排班和管理巡检计划的用户角色
- **Inspector（巡检员）**: 负责执行设备巡检的用户角色
- **Week_Start（周起始日期）**: 每周一的日期，作为巡检计划的周标识
- **Check_Type（检查类型）**: 固定5种检查项之一：外观检查(appearance)、运行状态(running)、温度检测(temperature)、噪音振动(noise)、电气安全(electrical)
- **Check_Result（检查结果）**: 检查项的结果状态：正常(normal)、异常(abnormal)、故障(fault)
- **Plan_Status（计划状态）**: 巡检计划的状态：待完成(pending)、已完成(completed)

## 需求

### 需求 1：巡检排班管理

**用户故事：** 作为调度员，我希望按周为巡检员分配负责的车间，以便系统化管理每周巡检工作安排。

#### 验收标准

1. WHEN 调度员打开巡检排班页面，THE 排班系统 SHALL 以周历形式展示当前周的排班计划，包含所有巡检员和车间的分配关系
2. WHEN 调度员选择某一周并为巡检员分配车间，THE 排班系统 SHALL 创建对应的 Inspection_Plan 记录，包含 inspector_id、workshop_id 和 week_start 字段
3. WHEN 调度员为同一巡检员在同一周分配多个车间，THE 排班系统 SHALL 为每个车间创建独立的 Inspection_Plan 记录
4. WHEN 调度员切换周视图，THE 排班系统 SHALL 加载并展示目标周的排班数据
5. WHEN 调度员删除某条排班记录，THE 排班系统 SHALL 移除对应的 Inspection_Plan 记录及其关联的未完成 Inspection_Record 记录

### 需求 2：系统自动排班

**用户故事：** 作为调度员，我希望点击"自动排班"按钮让系统自动为巡检员分配车间，以便快速完成排班工作。

#### 验收标准

1. WHEN 调度员点击"自动排班"按钮，THE 排班系统 SHALL 自动将所有可用车间均匀分配给所有在职巡检员，生成当前周的 Inspection_Plan 记录
2. WHEN 当前周已存在排班记录时调度员点击"自动排班"，THE 排班系统 SHALL 提示用户确认是否覆盖现有排班
3. IF 系统中没有在职巡检员，THEN THE 排班系统 SHALL 提示调度员当前无可用巡检员，无法自动排班
4. IF 系统中没有任何车间，THEN THE 排班系统 SHALL 提示调度员当前无车间数据，无法自动排班

### 需求 3：巡检任务自动生成

**用户故事：** 作为调度员，我希望系统根据排班计划自动生成巡检任务明细，以便巡检员能直接看到需要检查的设备和检查项。

#### 验收标准

1. WHEN 一条 Inspection_Plan 被创建，THE 任务生成系统 SHALL 为该计划对应车间的每台设备自动生成5条 Inspection_Record 记录，分别对应5种 Check_Type
2. WHEN 车间内设备列表发生变化后重新生成记录，THE 任务生成系统 SHALL 基于当前设备列表生成记录，确保每台设备恰好有5条检查记录
3. IF 车间内没有任何设备，THEN THE 任务生成系统 SHALL 创建 Inspection_Plan 但不生成任何 Inspection_Record，并在计划中标注"该车间暂无设备"

### 需求 4：巡检员每周巡检执行

**用户故事：** 作为巡检员，我希望在一个直观美观的页面上查看和操作本周所有分配设备的巡检状态，以便高效完成巡检工作。

#### 验收标准

1. WHEN 巡检员打开每周巡检页面，THE 巡检系统 SHALL 仅展示当前周分配给该巡检员的 Inspection_Plan 列表及其关联的车间信息
2. WHEN 巡检员选择某个巡检计划，THE 巡检系统 SHALL 以卡片式布局展示该车间下所有设备，每台设备显示5个 Check_Type 及其当前 Check_Result，支持直接在页面上切换每项检查结果
3. WHEN 巡检员为某个检查项选择结果（normal/abnormal/fault），THE 巡检系统 SHALL 立即更新对应 Inspection_Record 的 result 字段和 checked_at 时间戳
4. WHEN 巡检员选择 abnormal 或 fault 结果，THE 巡检系统 SHALL 要求填写 remark 备注字段，且备注不得为空
5. WHEN 某个 Inspection_Plan 下所有 Inspection_Record 均已填写结果且无 abnormal 或 fault 状态，THE 巡检系统 SHALL 自动将该 Inspection_Plan 的 status 更新为 completed
6. WHILE 某台设备处于报修中状态，THE 巡检系统 SHALL 将该设备的检查项显示为不可操作状态，并标注"报修中"

### 需求 5：异常转报修与设备状态联动

**用户故事：** 作为巡检员，我希望将巡检中发现的异常或故障设备转为报修，转报修后该设备在巡检页面和车间页面均显示为"报修中"，以便各角色了解设备真实状态。

#### 验收标准

1. WHEN 巡检员在检查结果为 abnormal 或 fault 的设备上点击"转报修"，THE 巡检系统 SHALL 创建一条 Feedback 记录（type 为 fault，关联 equipment_id，description 包含检查类型和备注），并将该设备在 equipments 表的 status 更新为 fault
2. WHEN 转报修成功后，THE 巡检系统 SHALL 在对应 Inspection_Record 上标记已转报修状态，该设备的所有检查项变为不可操作
3. WHEN 车间用户查看设备列表，THE 设备系统 SHALL 对状态为 fault 的设备显示"报修中"标识，车间用户无法对报修中的设备发起用电申请

### 需求 6：巡检数据查询

**用户故事：** 作为调度员，我希望查看各周巡检计划的完成情况，以便监控巡检工作进度。

#### 验收标准

1. WHEN 调度员在排班页面查看某周数据，THE 排班系统 SHALL 展示每条 Inspection_Plan 的完成进度，格式为"已检查设备数/总设备数"
2. WHEN 调度员点击某条巡检计划查看详情，THE 排班系统 SHALL 展示该计划下所有 Inspection_Record 的检查结果明细

### 需求 7：页面导航集成

**用户故事：** 作为系统用户，我希望通过侧边栏菜单快速访问巡检相关页面，以便顺畅使用巡检功能。

#### 验收标准

1. THE 巡检员侧边栏 SHALL 包含"每周巡检"菜单项，路由指向 `/inspector/weekly-inspection` 页面
2. THE 调度员侧边栏 SHALL 包含"巡检排班"菜单项，路由指向 `/dispatcher/inspection-schedule` 页面
3. WHEN 用户点击对应菜单项，THE 路由系统 SHALL 正确导航到目标页面并高亮当前菜单项
