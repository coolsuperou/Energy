# 工具制造电能数据平台 - 任务列表

## 1. 项目基础设施与代码重构

- [x] 1.1 Mapper包结构重组：将根目录散落的Mapper迁移到对应角色包
  - [x] 1.1.1 将 UserMapper、AttendanceMapper、CommentMapper、EnergyDataMapper、QuotaMapper 迁移到 mapper/common/
  - [x] 1.1.2 将 OperationLogMapper 迁移到 mapper/admin/
  - [x] 1.1.3 创建 mapper/workshop/ 包，将 ApplicationMapper、FeedbackMapper 迁入
  - [x] 1.1.4 更新所有引用这些Mapper的Service和Controller的import路径

- [x] 1.2 Controller包结构重组
  - [x] 1.2.1 将 ApplicationController 从 controller/common/ 拆分为 dispatcher/ApplicationController（审批）和 workshop/WorkshopApplicationController（提交申请）
  - [x] 1.2.2 将 EnergyDataController 从 controller/common/ 迁移到 controller/dispatcher/
  - [x] 1.2.3 将 EquipmentController 从 controller/common/ 迁移到 controller/workshop/
  - [x] 1.2.4 创建 controller/admin/ 包、controller/inspector/ 包

- [x] 1.3 Service包结构重组
  - [x] 1.3.1 将 AiService 从 service/common/ 迁移到 service/manager/
  - [x] 1.3.2 创建 service/admin/、service/inspector/、service/manager/ 包结构
  - [x] 1.3.3 更新所有引用迁移后类的import路径

- [x] 1.4 数据库schema清理：移除 tasks.status 中的 'assigned' 枚举值，只保留 pending/in_progress/completed

---

## 2. 公共模块（所有角色）

- [x] 2.1 评论功能（需求1.4）
  - [x] 2.1.1 创建 CommentController（controller/common/），实现 POST /comments 和 GET /comments 接口
  - [x] 2.1.2 创建 CommentService（service/common/），实现发表评论和查询评论列表逻辑
  - [x] 2.1.3 前端：在用电申请详情页和工单详情页添加评论交流组件

---

## 3. 车间用户模块（workshop）

- [x] 3.1 首页概览（需求2.1）
  - [x] 3.1.1 后端：在 WorkshopController 添加 GET /workshop/dashboard 接口，返回今日用电量、本月用电量、最近申请状态、未读消息数
  - [x] 3.1.2 前端：完善 views/workshop/Index.vue 首页概览页面

- [x] 3.2 用电申请（需求2.2）
  - [x] 3.2.1 创建 WorkshopApplicationController（controller/workshop/），实现 POST /applications 和 GET /applications/my 接口
  - [x] 3.2.2 前端：完善 views/workshop/Apply.vue 用电申请页面，包含申请表单和申请列表

- [x] 3.3 能耗查看（需求2.3）
  - [x] 3.3.1 创建 WorkshopEnergyController（controller/workshop/），实现 GET /energy-data/my-workshop 接口
  - [x] 3.3.2 前端：完善 views/workshop/Energy.vue 能耗查看页面，包含今日能耗曲线和历史查询

- [x] 3.4 故障报修（需求2.4）
  - [x] 3.4.1 完善 FeedbackController（controller/workshop/），添加图片上传接口 POST /feedback/{id}/images
  - [x] 3.4.2 前端：完善 views/workshop/Feedback.vue 故障报修页面，支持多图上传

- [x] 3.5 消息中心（需求2.5）
  - [x] 3.5.1 前端：在 views/workshop/ 添加消息中心页面，复用 common/Message.vue 组件

- [x] 3.6 个人中心（需求2.6）
  - [x] 3.6.1 前端：完善 views/workshop/Profile.vue 个人中心页面，包含头像上传、信息修改、考勤记录

---

## 4. 能源调度员模块（dispatcher）

- [x] 4.1 工作台（需求3.1）
  - [x] 4.1.1 后端：在 DispatcherController 添加 GET /dispatcher/dashboard 接口，返回待审批数、待处理预警数、进行中工单数、今日关键指标
  - [x] 4.1.2 前端：完善 views/dispatcher/Index.vue 工作台页面

- [x] 4.2 申请审批（需求3.2）
  - [x] 4.2.1 完善 ApplicationController（controller/dispatcher/），确保审批接口（approve/reject/adjust）正常工作
  - [x] 4.2.2 前端：完善 views/dispatcher/Approval.vue 申请审批页面，包含待审批列表和审批操作

- [x] 4.3 电力调度（需求3.3）
  - [x] 4.3.1 前端：完善 views/dispatcher/Dispatch.vue 电力调度页面，显示实时功率监控

- [x] 4.4 预警处理（需求3.4）
  - [x] 4.4.1 创建 EnergyAlertService（service/dispatcher/），实现预警检测逻辑（功率超限、接近限额、异常波动）
  - [x] 4.4.2 前端：完善 views/dispatcher/Alert.vue 预警处理页面

- [x] 4.5 工单管理（需求3.5）
  - [x] 4.5.1 完善 TaskController（controller/dispatcher/），确保创建、派单、查询接口正常工作
  - [x] 4.5.2 前端：完善 views/dispatcher/Order.vue 工单管理页面

- [x] 4.6 反馈处理（需求3.5）
  - [x] 4.6.1 创建 FeedbackHandleController（controller/dispatcher/），实现 GET /feedback/all、POST /feedback/{id}/handle、POST /feedback/{id}/dispatch 接口
  - [x] 4.6.2 创建 FeedbackHandleService（service/dispatcher/），实现反馈处理和反馈转工单逻辑
  - [x] 4.6.3 前端：在工单管理页面添加反馈处理功能

- [x] 4.7 消息通知（需求3.6）
  - [x] 4.7.1 前端：在 views/dispatcher/ 添加消息通知页面，复用 common/Message.vue 组件

- [x] 4.8 个人中心（需求3.7）
  - [x] 4.8.1 前端：完善 views/dispatcher/Profile.vue 个人中心页面

---

## 5. 设备巡检员模块（inspector）

- [x] 5.1 工作台（需求4.1）
  - [x] 5.1.1 后端：创建 InspectorController（controller/inspector/），添加 GET /inspector/dashboard 接口
  - [x] 5.1.2 前端：完善 views/inspector/Index.vue 工作台页面，显示任务统计、今日待完成、工作量趋势、技能列表

- [x] 5.2 我的任务（需求4.2）
  - [x] 5.2.1 创建 MyTaskController（controller/inspector/），实现 GET /tasks/my、GET /tasks/today、POST /tasks/{id}/complete 接口
  - [x] 5.2.2 创建 InspectorTaskService（service/inspector/），实现我的任务查询和完成工单逻辑
  - [x] 5.2.3 前端：完善 views/inspector/Equipment.vue 我的任务页面（进行中/已完成标签页）

- [x] 5.3 技能认证申请（需求4.1）
  - [x] 5.3.1 创建 SkillController（controller/inspector/），实现 POST /skills/apply、GET /users/current/skills 接口
  - [x] 5.3.2 创建 SkillService（service/inspector/），实现技能认证申请逻辑
  - [x] 5.3.3 前端：在工作台页面添加技能认证申请功能

- [x] 5.4 消息中心（需求4.3）
  - [x] 5.4.1 前端：完善 views/inspector/Message.vue 消息中心页面

- [x] 5.5 个人中心（需求4.4）
  - [x] 5.5.1 前端：完善 views/inspector/Profile.vue 个人中心页面

---

## 6. 经理模块（manager）

- [x] 6.1 首页概览（需求5.1）
  - [x] 6.1.1 后端：创建 ManagerDashboardController（controller/manager/），添加 GET /manager/dashboard 接口
  - [x] 6.1.2 前端：完善 views/manager/Index.vue 首页概览页面，显示全厂用电指标、车间排名、待处理事项

- [x] 6.2 数据分析（需求5.2）
  - [x] 6.2.1 创建 ManagerAnalysisController（controller/manager/），实现 GET /energy-data/summary 接口
  - [x] 6.2.2 前端：完善 views/manager/Analysis.vue 数据分析页面，包含车间对比、时段分析、成本统计

- [x] 6.3 AI分析（需求5.3）
  - [x] 6.3.1 完善 AiController（controller/manager/），确保流式对话和一键分析接口正常工作
  - [x] 6.3.2 实现5种快速操作指令：overview、peak-valley、anomaly、saving、forecast
  - [x] 6.3.3 前端：完善 views/manager/AI.vue AI分析页面，包含快速指令按钮和对话界面

- [x] 6.4 用户管理（需求5.4）
  - [x] 6.4.1 创建 ManagerUserController（controller/manager/），实现用户CRUD接口和技能认证审核接口
  - [x] 6.4.2 创建 ManagerUserService（service/manager/），实现用户管理和技能认证审核逻辑
  - [x] 6.4.3 前端：创建 views/manager/Users.vue 用户管理页面，包含用户列表、添加/编辑用户、技能认证审核

- [x] 6.5 排班考勤管理（需求5.5）
  - [x] 6.5.1 创建 ScheduleAttendanceController（controller/manager/），实现排班生成、排班调整、考勤查看、补卡接口
  - [x] 6.5.2 创建 ScheduleAttendanceService（service/manager/），实现排班考勤管理逻辑
  - [x] 6.5.3 前端：创建 views/manager/Schedule.vue 排班考勤管理页面，包含排班表、考勤记录、补卡功能

- [x] 6.6 消息中心（需求5.6）
  - [x] 6.6.1 前端：在 views/manager/ 添加消息中心页面，复用 common/Message.vue 组件

- [x] 6.7 个人中心（需求5.7）
  - [x] 6.7.1 前端：完善 views/manager/Profile.vue 个人中心页面

---

## 7. 系统管理员模块（admin）

- [x] 7.1 首页概览（需求6.1）
  - [x] 7.1.1 创建 AdminDashboardController（controller/admin/），实现 GET /admin/dashboard 接口
  - [x] 7.1.2 前端：完善 views/admin/Index.vue 首页概览页面，显示用户统计、活跃用户、系统状态

- [x] 7.2 用户管理（需求6.2）
  - [x] 7.2.1 创建 AdminUserController（controller/admin/），实现用户CRUD和启用/禁用接口
  - [x] 7.2.2 前端：完善 views/admin/Users.vue 用户管理页面

- [x] 7.3 系统配置（需求6.3）
  - [x] 7.3.1 创建 SystemConfigController（controller/admin/），实现 GET /config 和 PUT /config 接口
  - [x] 7.3.2 创建 SystemConfigService（service/admin/），实现系统配置管理逻辑
  - [x] 7.3.3 前端：完善 views/admin/Config.vue 系统配置页面，包含电价参数、预警阈值配置

- [x] 7.4 操作日志（需求6.4）
  - [x] 7.4.1 创建 OperationLogController（controller/admin/），实现 GET /logs 接口
  - [x] 7.4.2 创建 OperationLogService（service/admin/），实现操作日志查询逻辑
  - [x] 7.4.3 前端：完善 views/admin/Logs.vue 操作日志页面

- [x] 7.5 消息中心（需求6.5）
  - [x] 7.5.1 前端：在 views/admin/ 添加消息中心页面，复用 common/Message.vue 组件

- [x] 7.6 个人中心（需求6.6）
  - [x] 7.6.1 前端：完善 views/admin/Profile.vue 个人中心页面

---

## 8. 系统级功能

- [x] 8.1 能耗数据自动生成（需求7.1）
  - [x] 8.1.1 完善 EnergyDataGenerator（service/common/），确保审批通过后自动生成能耗数据
  - [x] 8.1.2 验证时段类型判定逻辑（峰/平/谷）和电价计算

- [x] 8.2 车间配额管理（需求7.2）
  - [x] 8.2.1 完善 QuotaService（service/common/），实现配额查询和更新逻辑
  - [x] 8.2.2 在能耗数据生成时更新已用配额

- [x] 8.3 文件存储服务（需求7.3）
  - [x] 8.3.1 完善 MinioService（service/common/），确保头像上传、反馈图片上传、技能证书上传正常工作

---

## 9. 属性测试（Property-Based Testing）

- [x] 9.1 能耗数据属性测试（P3.1-P3.4）
  - [x] 9.1.1 创建 EnergyDataPropertyTest.java，测试时段类型判定属性（任意小时0-23映射到正确时段）
  - [x] 9.1.2 测试电价计算属性（时段类型→电价对应关系）
  - [x] 9.1.3 测试费用计算属性（费用=电量×电价，精度误差≤0.01）
  - [x] 9.1.4 测试电量计算属性（电量=功率×1小时）

- [x] 9.2 用户认证属性测试（P1.1-P1.4）
  - [x] 9.2.1 创建 AuthPropertyTest.java，测试登录成功属性
  - [x] 9.2.2 测试登录失败属性（错误密码）
  - [x] 9.2.3 测试禁用用户登录属性
  - [x] 9.2.4 测试用户名重复注册属性

- [x] 9.3 用电申请属性测试（P2.1-P2.5）
  - [x] 9.3.1 创建 ApplicationPropertyTest.java，测试申请提交属性（状态为PENDING，生成唯一编号）
  - [x] 9.3.2 测试审批通过属性（状态变更，生成能耗数据）
  - [x] 9.3.3 测试审批拒绝属性（状态变更，不生成能耗数据）
  - [x] 9.3.4 测试审批幂等性属性（已处理申请不能重复审批）

- [x] 9.4 工单管理属性测试（P4.1-P4.4）
  - [x] 9.4.1 创建 TaskPropertyTest.java，测试工单创建属性（状态为PENDING）
  - [x] 9.4.2 测试派单属性（状态直接变为IN_PROGRESS）
  - [x] 9.4.3 测试完成工单属性（状态为COMPLETED，记录完成时间）
  - [x] 9.4.4 测试工单权限属性（只有被分配的巡检员才能完成）

- [x] 9.5 反馈管理属性测试（P5.1-P5.4）
  - [x] 9.5.1 创建 FeedbackPropertyTest.java，测试反馈提交属性（状态为PENDING，生成唯一编号）
  - [x] 9.5.2 测试撤回前置条件属性（只有PENDING状态可撤回）
  - [x] 9.5.3 测试撤回权限属性（只能撤回自己的反馈）

- [x] 9.6 考勤属性测试（P6.1-P6.3）
  - [x] 9.6.1 创建 AttendancePropertyTest.java，测试迟到判定属性
  - [x] 9.6.2 测试早退判定属性
  - [x] 9.6.3 测试工作时长计算属性

---

## 10. 前端路由与布局完善

- [x] 10.1 路由配置更新
  - [x] 10.1.1 在 router/index.js 添加经理模块新路由：/manager/users、/manager/schedule
  - [x] 10.1.2 在 router/index.js 添加管理员消息中心路由：/admin/message

- [x] 10.2 侧边栏菜单更新
  - [x] 10.2.1 更新 ManagerLayout.vue 侧边栏，添加"用户管理"和"排班考勤管理"菜单项
  - [x] 10.2.2 更新 AdminLayout.vue 侧边栏，添加"消息中心"菜单项
  - [x] 10.2.3 在调度员和车间侧边栏添加未读消息badge显示
