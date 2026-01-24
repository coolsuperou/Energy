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
            <div class="d-flex justify-content-between mb-3">
              <div class="attendance-record">
                <div v-for="day in attendanceWeek1" :key="day.day" class="attendance-day" :class="day.status">
                  {{ day.day }}
                </div>
              </div>
              <div class="attendance-record">
                <div v-for="day in attendanceWeek2" :key="day.day" class="attendance-day" :class="day.status">
                  {{ day.day }}
                </div>
              </div>
            </div>
            <div class="d-flex justify-content-between mb-3">
              <div class="attendance-record">
                <div v-for="day in attendanceWeek3" :key="day.day" class="attendance-day" :class="day.status">
                  {{ day.day }}
                </div>
              </div>
              <div class="attendance-record">
                <div v-for="day in attendanceWeek4" :key="day.day" class="attendance-day" :class="day.status">
                  {{ day.day }}
                </div>
              </div>
            </div>
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

const workChartRef = ref(null)
const userInfo = ref({
  name: '王五',
  roleName: '设备巡检员',
  employeeId: 'INS20230001',
  joinDate: '2023-03-15',
  phone: '138****8888',
  email: 'wangwu@company.com',
  department: '设备运维部'
})

const stats = ref({
  monthlyCompleted: 156,
  rating: 4.9,
  onTimeRate: 98,
  totalCompleted: 1280
})

const skills = ref([
  { id: 1, name: '高压电工证', icon: 'bi bi-lightning-charge text-warning', status: 'approved' },
  { id: 2, name: '设备维修资格证', icon: 'bi bi-tools text-primary', status: 'approved' },
  { id: 3, name: '安全生产证', icon: 'bi bi-shield-check text-success', status: 'pending' }
])

const schedule = ref([
  { day: '周一', time: '08:00 - 17:00', type: 'day', label: '白班' },
  { day: '周二', time: '08:00 - 17:00', type: 'day', label: '白班' },
  { day: '周三', time: '08:00 - 17:00', type: 'day', label: '白班' },
  { day: '周四', time: '17:00 - 02:00', type: 'night', label: '夜班' },
  { day: '周五', time: '休息', type: 'off', label: '休' }
])

const currentTime = ref('--:--:--')
const currentMonth = ref('2026年1月')
const clockStatus = ref('clock-out')
const clockButtonText = ref('下班打卡')
const clockInTime = ref('08:32:15')
const clockOutTime = ref('--:--:--')

const attendanceWeek1 = ref([
  { day: 1, status: 'normal' },
  { day: 2, status: 'normal' },
  { day: 3, status: 'late' },
  { day: 4, status: 'rest' },
  { day: 5, status: 'rest' },
  { day: 6, status: 'normal' },
  { day: 7, status: 'normal' }
])

const attendanceWeek2 = ref([
  { day: 8, status: 'normal' },
  { day: 9, status: 'normal' },
  { day: 10, status: 'normal' },
  { day: 11, status: 'rest' },
  { day: 12, status: 'rest' },
  { day: 13, status: 'normal' },
  { day: 14, status: 'normal' }
])

const attendanceWeek3 = ref([
  { day: 15, status: 'normal' },
  { day: 16, status: 'future' },
  { day: 17, status: 'future' },
  { day: 18, status: 'future' },
  { day: 19, status: 'future' },
  { day: 20, status: 'future' },
  { day: 21, status: 'future' }
])

const attendanceWeek4 = ref([
  { day: 22, status: 'future' },
  { day: 23, status: 'future' },
  { day: 24, status: 'future' },
  { day: 25, status: 'future' },
  { day: 26, status: 'future' },
  { day: 27, status: 'future' },
  { day: 28, status: 'future' }
])

const attendanceStats = ref({
  normal: 11,
  late: 1,
  absent: 0,
  rest: 4
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

function getClockIcon() {
  if (clockStatus.value === 'clock-in') {
    return 'bi-box-arrow-in-right'
  } else if (clockStatus.value === 'clock-out') {
    return 'bi-box-arrow-right'
  } else {
    return 'bi-check-circle'
  }
}

function handleClock() {
  const now = new Date()
  const timeStr = now.toTimeString().substring(0, 8)
  
  if (clockStatus.value === 'clock-in') {
    clockInTime.value = timeStr
    clockStatus.value = 'clock-out'
    clockButtonText.value = '下班打卡'
    ElMessage.success('上班打卡成功')
  } else if (clockStatus.value === 'clock-out') {
    clockOutTime.value = timeStr
    clockStatus.value = 'clocked'
    clockButtonText.value = '已下班'
    ElMessage.success('下班打卡成功')
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

function saveProfile() {
  userInfo.value.phone = editDialog.value.form.phone
  userInfo.value.email = editDialog.value.form.email
  editDialog.value.visible = false
  ElMessage.success('资料已更新')
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

function initWorkChart() {
  if (!workChartRef.value) return
  
  const chart = echarts.init(workChartRef.value)
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
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
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
        data: [8, 12, 10, 0, 15, 6, 5]
      },
      { 
        name: '巡检次数', 
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { color: '#22c55e', width: 2 },
        itemStyle: { color: '#22c55e' },
        data: [3, 4, 3, 0, 5, 2, 2]
      }
    ]
  })
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  initWorkChart()
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>

<style lang="scss" scoped>
.clock-btn {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  font-size: 18px;
  font-weight: 600;
  border: none;
  transition: all 0.3s;
  
  &.clock-in {
    background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
    color: #fff;
    box-shadow: 0 8px 25px rgba(34, 197, 94, 0.4);
  }
  
  &.clock-out {
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    color: #fff;
    box-shadow: 0 8px 25px rgba(245, 158, 11, 0.4);
  }
  
  &:hover:not(:disabled) {
    transform: scale(1.05);
  }
  
  &:disabled {
    background: #e5e7eb;
    color: #9ca3af;
    box-shadow: none;
    transform: none;
  }
}

.attendance-record {
  display: flex;
  gap: 4px;
}

.attendance-day {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  
  &.normal {
    background: #dcfce7;
    color: #16a34a;
  }
  
  &.late {
    background: #fef3c7;
    color: #d97706;
  }
  
  &.absent {
    background: #fee2e2;
    color: #dc2626;
  }
  
  &.rest {
    background: #f3f4f6;
    color: #9ca3af;
  }
  
  &.future {
    background: #f9fafb;
    color: #d1d5db;
    border: 1px dashed #e5e7eb;
  }
}

.bg-purple {
  background-color: #722ed1 !important;
}

// 夜班特殊样式
.small > span:last-child {
  &:not(.text-success):not(.text-muted) {
    color: #8b5cf6;
  }
}

// 夜班背景
.rounded.mb-1.small:has(span:last-child:not(.text-success):not(.text-muted)) {
  background: #f3e8ff !important;
}
</style>
