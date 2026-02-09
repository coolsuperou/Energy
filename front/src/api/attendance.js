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
