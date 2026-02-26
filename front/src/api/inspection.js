import request from './request'

/**
 * 获取巡检任务列表
 */
export function getInspectionTasks(params) {
  return request.get('/inspections', { params })
}

/**
 * 获取今日巡检任务
 */
export function getTodayInspections() {
  return request.get('/inspections/today')
}

/**
 * 获取巡检任务详情
 */
export function getInspectionById(id) {
  return request.get(`/inspections/${id}`)
}

/**
 * 开始巡检
 */
export function startInspection(id) {
  return request.post(`/inspections/${id}/start`)
}

/**
 * 提交巡检记录
 */
export function submitInspection(id, data) {
  return request.post(`/inspections/${id}/submit`, data)
}

/**
 * 获取巡检检查项
 */
export function getInspectionItems(inspectionId) {
  return request.get(`/inspections/${inspectionId}/items`)
}

/**
 * 更新检查项结果
 */
export function updateInspectionItem(inspectionId, itemId, data) {
  return request.put(`/inspections/${inspectionId}/items/${itemId}`, data)
}


/**
 * 获取巡检员工作台数据
 */
export function getInspectorDashboard() {
  return request.get('/inspector/dashboard')
}

/**
 * 提交技能认证申请
 * @param {Object} data - { skillName: 技能名称, certificateUrl: 证书URL(可选) }
 */
export function applySkillCertification(data) {
  return request.post('/inspector/skills/apply', data)
}

/**
 * 获取当前用户的技能列表
 */
export function getMySkills() {
  return request.get('/inspector/skills/my')
}

/**
 * 重新申请被拒绝的技能认证
 * @param {number} id - 被拒绝的认证记录ID
 * @param {Object} data - { certificateUrl: 新证书URL(可选) }
 */
export function reapplySkillCertification(id, data) {
  return request.post(`/inspector/skills/${id}/reapply`, data || {})
}


// ============================================
// 调度员：巡检排班管理
// ============================================

/**
 * 获取某周排班列表
 * @param {string} weekStart - 周一日期，格式 YYYY-MM-DD
 */
export function getInspectionPlans(weekStart) {
  return request.get('/inspection-schedule/plans', { params: { weekStart } })
}

/**
 * 添加排班
 * @param {Object} data - { inspectorId, workshopId, weekStart }
 */
export function addInspectionPlan(data) {
  return request.post('/inspection-schedule/plans', data)
}

/**
 * 删除排班
 */
export function deleteInspectionPlan(id) {
  return request.delete(`/inspection-schedule/plans/${id}`)
}

/**
 * 自动排班
 * @param {string} weekStart - 周一日期
 */
export function autoScheduleInspection(weekStart) {
  return request.post('/inspection-schedule/auto', null, { params: { weekStart } })
}

/**
 * 获取某计划的巡检记录详情
 */
export function getInspectionPlanRecords(planId) {
  return request.get(`/inspection-schedule/plans/${planId}/records`)
}

/**
 * 获取所有在职巡检员列表
 */
export function getInspectorList() {
  return request.get('/inspection-schedule/inspectors')
}

/**
 * 获取所有车间列表
 */
export function getWorkshopList() {
  return request.get('/inspection-schedule/workshops')
}

// ============================================
// 巡检员：每周巡检执行
// ============================================

/**
 * 获取我的本周巡检计划
 */
export function getMyInspectionPlans() {
  return request.get('/weekly-inspection/plans')
}

/**
 * 获取某计划下的设备巡检记录（按设备分组）
 */
export function getMyPlanRecords(planId) {
  return request.get(`/weekly-inspection/plans/${planId}/records`)
}

/**
 * 更新检查结果
 * @param {number} id - 记录ID
 * @param {Object} data - { result, remark }
 */
export function updateInspectionRecord(id, data) {
  return request.put(`/weekly-inspection/records/${id}`, data)
}

/**
 * 异常转报修
 */
export function repairFromInspection(id) {
  return request.post(`/weekly-inspection/records/${id}/repair`)
}

/**
 * 恢复报修设备为正常状态
 */
export function restoreFromRepair(planId, equipmentId) {
  return request.post(`/weekly-inspection/plans/${planId}/equipment/${equipmentId}/restore`)
}
