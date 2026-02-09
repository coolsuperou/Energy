<template>
  <div class="content-wrapper">
    <!-- 个人信息头部 -->
    <div class="ai-panel mb-4">
      <div class="d-flex align-items-center gap-4">
        <div class="rounded-circle bg-white bg-opacity-25 d-flex align-items-center justify-content-center" 
             style="width: 80px; height: 80px; font-size: 40px;">
          👷
        </div>
        <div class="flex-grow-1">
          <h4 class="mb-1">{{ userInfo.name }}</h4>
          <p class="mb-1 opacity-75">{{ userInfo.roleName }} · 工号: {{ userInfo.employeeId }} · 入职: {{ userInfo.joinDate }}</p>
          <p class="mb-0 opacity-75 small">
            <i class="bi bi-phone me-1"></i>{{ userInfo.phone }}
            <i class="bi bi-envelope ms-3 me-1"></i>{{ userInfo.email }}
            <i class="bi bi-building ms-3 me-1"></i>{{ userInfo.department }}
          </p>
        </div>
        <button class="btn btn-light btn-sm" @click="showEditDialog">
          <i class="bi bi-pencil me-1"></i>编辑资料
        </button>
      </div>
    </div>

    <div class="row g-4 mb-4">
      <!-- 考勤打卡 -->
      <div class="col-lg-4">
        <div class="card h-100">
          <div class="card-header d-flex justify-content-between align-items-center">
            <span>今日考勤</span>
            <span class="text-muted small">{{ currentTime }}</span>
          </div>
          <div class="card-body text-center">
            <button class="clock-btn mb-3" :class="clockStatus" @click="handleClock" :disabled="clockStatus === 'clocked'">
              <i class="bi d-block mb-1" :class="getClockIcon()"></i>
              {{ clockButtonText }}
            </button>
            <div class="row text-center mt-3">
              <div class="col-6">
                <div class="text-muted small">上班时间</div>
                <div class="fw-semibold text-success">{{ clockInTime }}</div>
              </div>
              <div class="col-6">
                <div class="text-muted small">下班时间</div>
                <div class="fw-semibold">{{ clockOutTime }}</div>
              </div>
            </div>
            <div v-if="clockInTime !== '--:--:--'" class="mt-3 p-2 bg-success bg-opacity-10 rounded small">
              <i class="bi bi-check-circle text-success me-1"></i>今日已正常打卡上班
            </div>
          </div>
        </div>
      </div>

      <!-- 本月考勤统计 -->
      <div class="col-lg-8">
        <div class="card h-100">
          <div class="card-header d-flex justify-content-between align-items-center">
            <span>本月考勤记录</span>
            <span class="text-muted small">{{ currentMonth }}</span>
          </div>
          <div class="card-body">
            <!-- 日历表格 -->
            <div class="calendar-grid mb-3">
              <!-- 星期标题行 -->
              <div class="calendar-header">
                <div class="calendar-weekday">周一</div>
                <div class="calendar-weekday">周二</div>
                <div class="calendar-weekday">周三</div>
                <div class="calendar-weekday">周四</div>
                <div class="calendar-weekday">周五</div>
                <div class="calendar-weekday">周六</div>
                <div class="calendar-weekday">周日</div>
              </div>
              <!-- 日期网格 -->
              <div class="calendar-body">
                <div 
                  v-for="day in calendarDays" 
                  :key="day.key" 
                  class="calendar-day" 
                  :class="[day.status, { 'other-month': !day.isCurrentMonth }]">
                  <span class="day-number">{{ day.day }}</span>
                </div>
              </div>
            </div>
            <!-- 统计图例 -->
            <div class="d-flex gap-4 small">
              <span><span class="attendance-day normal d-inline-flex" style="width:20px;height:20px;">✓</span> 正常 {{ attendanceStats.normal }}天</span>
              <span><span class="attendance-day late d-inline-flex" style="width:20px;height:20px;">!</span> 迟到 {{ attendanceStats.late }}天</span>
              <span><span class="attendance-day absent d-inline-flex" style="width:20px;height:20px;">×</span> 缺勤 {{ attendanceStats.absent }}天</span>
              <span><span class="attendance-day rest d-inline-flex" style="width:20px;height:20px;">休</span> 休息 {{ attendanceStats.rest }}天</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 绩效统计 -->
    <div class="row g-4 mb-4">
      <div class="col-md-3">
        <div class="stat-card text-center">
          <div class="stat-value text-primary">{{ stats.monthlyCompleted }}</div>
          <div class="stat-label">本月完成工单</div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card text-center">
          <div class="stat-value text-warning">{{ stats.rating }}</div>
          <div class="stat-label">服务评分</div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card text-center">
          <div class="stat-value text-success">{{ stats.onTimeRate }}%</div>
          <div class="stat-label">按时完成率</div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card text-center">
          <div class="stat-value" style="color: #8b5cf6;">{{ stats.totalCompleted.toLocaleString() }}</div>
          <div class="stat-label">累计工单</div>
        </div>
      </div>
    </div>

    <div class="row g-4">
      <!-- 工作量趋势 -->
      <div class="col-lg-8">
        <div class="card">
          <div class="card-header">近7天工作量统计</div>
          <div class="card-body">
            <div ref="workChartRef" style="height: 250px;"></div>
          </div>
        </div>
      </div>

      <!-- 技能认证 & 排班 -->
      <div class="col-lg-4">
        <div class="card mb-4">
          <div class="card-header">技能认证</div>
          <div class="card-body">
            <div v-for="skill in skills" :key="skill.id" 
                 class="d-flex justify-content-between align-items-center p-2 rounded mb-2"
                 :class="getSkillBgClass(skill.status)">
              <span class="small">
                <i :class="skill.icon" class="me-1"></i>{{ skill.name }}
              </span>
              <span class="badge" :class="getSkillBadge(skill.status)">{{ getSkillStatusText(skill.status) }}</span>
            </div>
          </div>
        </div>
        
        <div class="card">
          <div class="card-header">本周排班</div>
          <div class="card-body p-2">
            <div v-for="day in schedule" :key="day.day" 
                 class="d-flex justify-content-between align-items-center p-2 rounded mb-1 small"
                 :class="getScheduleBgClass(day.type)"
                 :style="day.type === 'night' ? 'background: #f3e8ff;' : ''">
              <span>{{ day.day }}</span>
              <span>{{ day.time }}</span>
              <span :class="getScheduleTextClass(day.type)" :style="day.type === 'night' ? 'color: #8b5cf6;' : ''">{{ day.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑资料对话框 -->
    <el-dialog v-model="editDialog.visible" title="编辑资料" width="500px">
      <el-form :model="editDialog.form" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="editDialog.form.name" disabled />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editDialog.form.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editDialog.form.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getCurrentUser, getUserStats, getUserSkills, getUserSchedule, getWorkStats, updateUserInfo } from '@/api/user'
import { getTodayAttendance, getMonthlyAttendance, getAttendanceStats, clockIn } from '@/api/attendance'

const workChartRef = ref(null)
const userInfo = ref({
  name: '',
  roleName: '',
  employeeId: '',
  joinDate: '',
  phone: '',
  email: '',
  department: ''
})

const stats = ref({
  monthlyCompleted: 0,
  rating: 0,
  onTimeRate: 0,
  totalCompleted: 0
})

const skills = ref([])
const schedule = ref([])

const currentTime = ref('--:--:--')
const currentMonth = ref('')
const clockStatus = ref('clock-in')
const clockButtonText = ref('上班打卡')
const clockInTime = ref('--:--:--')
const clockOutTime = ref('--:--:--')

const calendarDays = ref([])

const attendanceStats = ref({
  normal: 0,
  late: 0,
  absent: 0,
  rest: 0
})

let timeInterval = null

// 编辑对话框
const editDialog = ref({
  visible: false,
  form: {
    name: '',
    phone: '',
    email: ''
  }
})

function updateTime() {
  const now = new Date()
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${hours}:${minutes}:${seconds}`
}

// 加载用户信息
async function loadUserInfo() {
  try {
    const res = await getCurrentUser()
    if (res && res.code === 200 && res.data) {
      userInfo.value = {
        name: res.data.name || '',
        roleName: res.data.roleName || '设备巡检员',
        employeeId: res.data.employeeId || '',
        joinDate: res.data.joinDate || '',
        phone: res.data.phone || '',
        email: res.data.email || '',
        department: res.data.department || ''
      }
    }
  } catch (e) {
    console.error('加载用户信息失败', e)
  }
}

// 加载统计数据
async function loadStats() {
  try {
    const res = await getUserStats()
    if (res && res.code === 200 && res.data) {
      stats.value = {
        monthlyCompleted: res.data.monthlyCompleted || 0,
        rating: res.data.rating || 0,
        onTimeRate: res.data.onTimeRate || 0,
        totalCompleted: res.data.totalCompleted || 0
      }
    }
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}

// 加载技能认证
async function loadSkills() {
  try {
    const res = await getUserSkills()
    if (res && res.code === 200 && res.data) {
      skills.value = (res.data || []).map(item => ({
        id: item.id,
        name: item.name,
        icon: item.icon || 'bi bi-award',
        status: item.status || 'approved'
      }))
    }
  } catch (e) {
    console.error('加载技能认证失败', e)
  }
}

// 加载排班信息
async function loadSchedule() {
  try {
    const now = new Date()
    const startDate = new Date(now.setDate(now.getDate() - now.getDay() + 1))
    const endDate = new Date(startDate)
    endDate.setDate(startDate.getDate() + 4)
    
    const res = await getUserSchedule(
      startDate.toISOString().split('T')[0],
      endDate.toISOString().split('T')[0]
    )
    if (res && res.code === 200 && res.data) {
      schedule.value = (res.data || []).map(item => ({
        day: item.dayOfWeek,
        time: item.timeRange || item.time,
        type: item.shiftType === 'DAY' ? 'day' : item.shiftType === 'NIGHT' ? 'night' : 'off',
        label: item.shiftType === 'DAY' ? '白班' : item.shiftType === 'NIGHT' ? '夜班' : '休'
      }))
    }
  } catch (e) {
    console.error('加载排班信息失败', e)
  }
}

// 加载今日考勤
async function loadTodayAttendance() {
  try {
    const res = await getTodayAttendance()
    if (res && res.code === 200 && res.data) {
      clockInTime.value = res.data.clockInTime || '--:--:--'
      clockOutTime.value = res.data.clockOutTime || '--:--:--'
      
      if (res.data.clockOutTime) {
        clockStatus.value = 'clocked'
        clockButtonText.value = '已下班'
      } else if (res.data.clockInTime) {
        clockStatus.value = 'clock-out'
        clockButtonText.value = '下班打卡'
      } else {
        clockStatus.value = 'clock-in'
        clockButtonText.value = '上班打卡'
      }
    }
  } catch (e) {
    console.error('加载今日考勤失败', e)
  }
}

// 生成日历数据
function generateCalendarDays(year, month, attendanceRecords) {
  const days = []
  
  // 获取当月第一天和最后一天
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  
  // 获取当月第一天是星期几 (0=周日, 1=周一, ..., 6=周六)
  let firstDayOfWeek = firstDay.getDay()
  // 转换为周一为第一天 (0=周一, 1=周二, ..., 6=周日)
  firstDayOfWeek = firstDayOfWeek === 0 ? 6 : firstDayOfWeek - 1
  
  // 创建考勤记录映射表
  const recordMap = {}
  attendanceRecords.forEach(record => {
    const date = new Date(record.date)
    const key = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`
    recordMap[key] = record.status
  })
  
  // 添加上月末尾的日期（填充到周一）
  const prevMonthLastDay = new Date(year, month - 1, 0).getDate()
  for (let i = firstDayOfWeek - 1; i >= 0; i--) {
    const day = prevMonthLastDay - i
    days.push({
      key: `prev-${day}`,
      day: day,
      isCurrentMonth: false,
      status: 'future'
    })
  }
  
  // 添加当月所有日期
  const daysInMonth = lastDay.getDate()
  for (let day = 1; day <= daysInMonth; day++) {
    const key = `${year}-${month}-${day}`
    const status = recordMap[key] ? 
      (recordMap[key] === 'NORMAL' ? 'normal' : 
       recordMap[key] === 'LATE' ? 'late' :
       recordMap[key] === 'ABSENT' ? 'absent' :
       recordMap[key] === 'REST' ? 'rest' : 'future') : 'future'
    
    days.push({
      key: `current-${day}`,
      day: day,
      isCurrentMonth: true,
      status: status
    })
  }
  
  // 添加下月开头的日期（填充到周日）
  const remainingDays = 7 - (days.length % 7)
  if (remainingDays < 7) {
    for (let day = 1; day <= remainingDays; day++) {
      days.push({
        key: `next-${day}`,
        day: day,
        isCurrentMonth: false,
        status: 'future'
      })
    }
  }
  
  return days
}

// 加载月度考勤
async function loadMonthlyAttendance() {
  try {
    const now = new Date()
    const year = now.getFullYear()
    const month = now.getMonth() + 1
    currentMonth.value = `${year}年${month}月`
    
    const res = await getMonthlyAttendance(year, month)
    if (res && res.code === 200 && res.data) {
      const records = res.data || []
      calendarDays.value = generateCalendarDays(year, month, records)
    } else {
      calendarDays.value = generateCalendarDays(year, month, [])
    }
    
    const statsRes = await getAttendanceStats(year, month)
    if (statsRes && statsRes.code === 200 && statsRes.data) {
      attendanceStats.value = {
        normal: statsRes.data.normalDays || 0,
        late: statsRes.data.lateDays || 0,
        absent: statsRes.data.absentDays || 0,
        rest: statsRes.data.restDays || 0
      }
    }
  } catch (e) {
    console.error('加载月度考勤失败', e)
  }
}

// 加载工作量统计
async function loadWorkStats() {
  try {
    const res = await getWorkStats(7)
    if (res && res.code === 200 && res.data) {
      initWorkChart(res.data)
    }
  } catch (e) {
    console.error('加载工作量统计失败', e)
  }
}

function getClockIcon() {
  if (clockStatus.value === 'clock-in') {
    return 'bi-box-arrow-in-right'
  } else if (clockStatus.value === 'clock-out') {
    return 'bi-box-arrow-right'
  } else {
    return 'bi-check-circle'
  }
}

async function handleClock() {
  try {
    const type = clockStatus.value === 'clock-in' ? 'IN' : 'OUT'
    const res = await clockIn(type)
    
    if (res && res.code === 200) {
      const now = new Date()
      const timeStr = now.toTimeString().substring(0, 8)
      
      if (type === 'IN') {
        clockInTime.value = timeStr
        clockStatus.value = 'clock-out'
        clockButtonText.value = '下班打卡'
        ElMessage.success('上班打卡成功')
      } else {
        clockOutTime.value = timeStr
        clockStatus.value = 'clocked'
        clockButtonText.value = '已下班'
        ElMessage.success('下班打卡成功')
      }
    }
  } catch (e) {
    console.error('打卡失败', e)
    ElMessage.error('打卡失败')
  }
}

function showEditDialog() {
  editDialog.value = {
    visible: true,
    form: {
      name: userInfo.value.name,
      phone: userInfo.value.phone,
      email: userInfo.value.email
    }
  }
}

async function saveProfile() {
  try {
    const res = await updateUserInfo({
      phone: editDialog.value.form.phone,
      email: editDialog.value.form.email
    })
    
    if (res && res.code === 200) {
      userInfo.value.phone = editDialog.value.form.phone
      userInfo.value.email = editDialog.value.form.email
      editDialog.value.visible = false
      ElMessage.success('资料已更新')
    }
  } catch (e) {
    console.error('更新资料失败', e)
    ElMessage.error('更新资料失败')
  }
}

function getSkillBadge(status) {
  const map = {
    approved: 'bg-success',
    pending: 'bg-warning',
    expired: 'bg-danger'
  }
  return map[status] || 'bg-secondary'
}

function getSkillBgClass(status) {
  const map = {
    approved: 'bg-success bg-opacity-10',
    pending: 'bg-warning bg-opacity-10',
    expired: 'bg-danger bg-opacity-10'
  }
  return map[status] || 'bg-light'
}

function getSkillStatusText(status) {
  const map = {
    approved: '已认证',
    pending: '待续期',
    expired: '已过期'
  }
  return map[status] || status
}

function getScheduleBadge(type) {
  const map = {
    day: 'bg-success',
    night: 'bg-purple',
    off: 'bg-secondary'
  }
  return map[type] || 'bg-secondary'
}

function getScheduleBgClass(type) {
  const map = {
    day: 'bg-primary bg-opacity-10',
    night: '',
    off: 'bg-light'
  }
  return map[type] || 'bg-light'
}

function getScheduleTextClass(type) {
  const map = {
    day: 'text-success',
    night: '',
    off: 'text-muted'
  }
  return map[type] || ''
}

function initWorkChart(data) {
  if (!workChartRef.value) return
  
  const chart = echarts.init(workChartRef.value)
  const days = data?.days || ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const completedTasks = data?.completedTasks || [0, 0, 0, 0, 0, 0, 0]
  const inspections = data?.inspections || [0, 0, 0, 0, 0, 0, 0]
  
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { 
      data: ['完成工单', '巡检次数'], 
      right: 20, 
      top: 0 
    },
    grid: { 
      left: '3%', 
      right: '4%', 
      bottom: '3%', 
      top: '15%', 
      containLabel: true 
    },
    xAxis: { 
      type: 'category', 
      data: days,
      axisLine: { lineStyle: { color: '#e0e0e0' } },
      axisLabel: { color: '#666' }
    },
    yAxis: { 
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
      axisLabel: { color: '#666' }
    },
    series: [
      { 
        name: '完成工单', 
        type: 'bar', 
        barWidth: '30%',
        itemStyle: { 
          color: '#667eea',
          borderRadius: [4, 4, 0, 0]
        },
        data: completedTasks
      },
      { 
        name: '巡检次数', 
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#22c55e', width: 2 },
        itemStyle: { color: '#22c55e' },
        data: inspections
      }
    ]
  })
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  
  loadUserInfo()
  loadStats()
  loadSkills()
  loadSchedule()
  loadTodayAttendance()
  loadMonthlyAttendance()
  loadWorkStats()
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>
