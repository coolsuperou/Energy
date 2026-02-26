import request from './request'

/**
 * 打卡
 */
export function clockIn(type) {
  return request.post('/attendance/clock', null, {
    params: { type }
  })
}

/**
 * 获取今日考勤
 */
export function getTodayAttendance() {
  return request.get('/attendance/today')
}

/**
 * 获取本月考勤记录
 */
export function getMonthlyAttendance(year, month) {
  return request.get('/attendance/monthly', {
    params: { year, month }
  })
}

/**
 * 获取考勤统计
 */
export function getAttendanceStats(year, month) {
  return request.get('/attendance/stats', {
    params: { year, month }
  })
}

/**
 * 获取本周排班
 */
export function getWeeklySchedule() {
  return request.get('/attendance/schedule/weekly')
}


// ==================== 经理排班考勤管理接口 ====================

/**
 * 自动生成月度排班计划
 */
export function generateSchedule(year, month) {
  return request.post('/manager/schedule/generate', null, {
    params: { year, month }
  })
}

/**
 * 获取所有人员的排班表
 */
export function getScheduleList(params) {
  return request.get('/manager/schedule/list', { params })
}

/**
 * 调整排班
 */
export function adjustSchedule(id, data) {
  return request.put(`/manager/schedule/${id}`, data)
}

/**
 * 获取所有人员的考勤记录
 */
export function getAttendanceList(params) {
  return request.get('/manager/schedule/attendance', { params })
}

/**
 * 获取每日考勤汇总统计
 */
export function getDailySummary(date) {
  return request.get('/manager/schedule/attendance/daily-summary', {
    params: { date }
  })
}

/**
 * 补卡操作
 */
export function supplementAttendance(id, data) {
  return request.put(`/manager/schedule/attendance/${id}/supplement`, data)
}

/**
 * 获取月度考勤统计报表
 */
export function getMonthlyReport(year, month) {
  return request.get('/manager/schedule/attendance/monthly-report', {
    params: { year, month }
  })
}
