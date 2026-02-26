<template>
  <div class="content-wrapper">
    <!-- 个人信息头部 -->
    <div class="ai-panel mb-4">
      <div class="d-flex align-items-center gap-4">
        <!-- 头像区域 -->
        <div class="position-relative avatar-container">
          <div v-if="userInfo.avatarUrl" 
               class="rounded-circle bg-white d-flex align-items-center justify-content-center overflow-hidden" 
               style="width: 80px; height: 80px;">
            <img :src="userInfo.avatarUrl" alt="头像" style="width: 100%; height: 100%; object-fit: cover;" />
          </div>
          <div v-else
               class="rounded-circle bg-white bg-opacity-25 d-flex align-items-center justify-content-center" 
               style="width: 80px; height: 80px; font-size: 40px;">
            💼
          </div>
          <!-- 上传按钮悬浮层 -->
          <div class="avatar-upload-overlay" @click="triggerFileInput">
            <i class="bi bi-camera-fill"></i>
          </div>
          <!-- 隐藏的文件选择器 -->
          <input 
            ref="fileInputRef" 
            type="file" 
            accept="image/*" 
            style="display: none;" 
            @change="handleFileSelect" 
          />
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
        <div class="d-flex gap-2">
          <button class="btn btn-light btn-sm" @click="showEditDialog">
            <i class="bi bi-pencil me-1"></i>编辑资料
          </button>
          <button class="btn btn-outline-light btn-sm" @click="showPasswordDialog">
            <i class="bi bi-key me-1"></i>修改密码
          </button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="row g-4 mb-4">
      <div class="col-md-3">
        <div class="stat-card text-center">
          <div class="stat-value text-primary">{{ stats.monthlyReports }}</div>
          <div class="stat-label">本月报告</div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card text-center">
          <div class="stat-value text-success">{{ stats.energySaved }}%</div>
          <div class="stat-label">能源节约率</div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card text-center">
          <div class="stat-value text-warning">{{ stats.optimizations }}</div>
          <div class="stat-label">优化建议</div>
        </div>
      </div>
      <div class="col-md-3">
        <div class="stat-card text-center">
          <div class="stat-value" style="color: #8b5cf6;">{{ stats.teamSize }}</div>
          <div class="stat-label">团队人数</div>
        </div>
      </div>
    </div>

    <!-- 考勤信息 -->
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
            <span>考勤记录</span>
            <div class="d-flex align-items-center gap-2">
              <button class="btn btn-sm btn-outline-secondary py-0 px-1" @click="changeMonth(-1)"><i class="bi bi-chevron-left"></i></button>
              <span class="text-muted small" style="min-width: 80px; text-align: center;">{{ currentMonth }}</span>
              <button class="btn btn-sm btn-outline-secondary py-0 px-1" @click="changeMonth(1)" :disabled="isCurrentMonth"><i class="bi bi-chevron-right"></i></button>
            </div>
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
              <span><span class="attendance-day early-leave d-inline-flex" style="width:20px;height:20px;">↓</span> 早退 {{ attendanceStats.earlyLeave }}天</span>
              <span><span class="attendance-day absent d-inline-flex" style="width:20px;height:20px;">×</span> 缺勤 {{ attendanceStats.absent }}天</span>
              <span><span class="attendance-day rest d-inline-flex" style="width:20px;height:20px;">休</span> 休息 {{ attendanceStats.rest }}天</span>
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
          <el-input v-model="editDialog.form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editDialog.form.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialog.visible" title="修改密码" width="450px">
      <el-form :model="passwordDialog.form" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordDialog.form.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordDialog.form.newPassword" type="password" show-password placeholder="请输入新密码（6-20位）" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordDialog.form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitChangePassword" :loading="passwordDialog.loading">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCurrentUser, updateUserInfo, uploadAvatar, getAvatar, changePassword } from '@/api/user'
import { getTodayAttendance, getMonthlyAttendance, getAttendanceStats, clockIn } from '@/api/attendance'
import { getManagerDashboard } from '@/api/manager'

const fileInputRef = ref(null)
const passwordFormRef = ref(null)

const userInfo = ref({
  name: '',
  roleName: '',
  employeeId: '',
  joinDate: '',
  phone: '',
  email: '',
  department: '',
  avatarUrl: ''
})

const stats = ref({
  monthlyReports: 0,
  energySaved: 0,
  optimizations: 0,
  teamSize: 0
})

const currentTime = ref('--:--:--')
const currentMonth = ref('')
const clockStatus = ref('clock-in')
const clockButtonText = ref('上班打卡')
const clockInTime = ref('--:--:--')
const clockOutTime = ref('--:--:--')
const calendarDays = ref([])
const calendarYear = ref(new Date().getFullYear())
const calendarMonth = ref(new Date().getMonth() + 1)
const isCurrentMonth = ref(true)
const attendanceStats = ref({
  normal: 0,
  late: 0,
  earlyLeave: 0,
  absent: 0,
  rest: 0
})

const editDialog = ref({
  visible: false,
  form: {
    name: '',
    phone: '',
    email: ''
  }
})

const passwordDialog = ref({
  visible: false,
  loading: false,
  form: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
})

// 密码验证规则
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordDialog.value.form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在6-20位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

let timeInterval = null

function updateTime() {
  const now = new Date()
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${hours}:${minutes}:${seconds}`
}

async function loadUserInfo() {
  try {
    const res = await getCurrentUser()
    if (res && res.code === 200 && res.data) {
      userInfo.value = {
        name: res.data.name || '',
        roleName: res.data.roleName || '能源经理',
        employeeId: res.data.employeeId || '',
        joinDate: res.data.joinDate || '',
        phone: res.data.phone || '',
        email: res.data.email || '',
        department: res.data.department || '',
        avatarUrl: res.data.avatarUrl || ''
      }
      
      if (!userInfo.value.avatarUrl) {
        loadAvatar()
      }
    }
  } catch (e) {
    console.error('加载用户信息失败', e)
  }
}

async function loadAvatar() {
  try {
    const res = await getAvatar()
    if (res && res.code === 200 && res.data && res.data.avatarUrl) {
      userInfo.value.avatarUrl = res.data.avatarUrl
    }
  } catch (e) {
    console.error('加载头像失败', e)
  }
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileSelect(event) {
  const file = event.target.files[0]
  if (!file) return
  
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件')
    return
  }
  
  if (file.size > 15 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过15MB')
    return
  }
  
  uploadAvatarFile(file)
}

async function uploadAvatarFile(file) {
  try {
    const loadingMsg = ElMessage({
      message: '正在上传头像...',
      type: 'info',
      duration: 0
    })
    
    const res = await uploadAvatar(file)
    loadingMsg.close()
    
    if (res && res.code === 200 && res.data) {
      userInfo.value.avatarUrl = res.data.avatarUrl
      ElMessage.success('头像上传成功')
    } else {
      ElMessage.error(res?.message || '头像上传失败')
    }
  } catch (e) {
    console.error('上传头像失败', e)
    ElMessage.error('头像上传失败')
  } finally {
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
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

function showPasswordDialog() {
  passwordDialog.value = {
    visible: true,
    loading: false,
    form: {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  }
}

async function submitChangePassword() {
  if (!passwordFormRef.value) return
  
  try {
    await passwordFormRef.value.validate()
    
    passwordDialog.value.loading = true
    const res = await changePassword(
      passwordDialog.value.form.oldPassword,
      passwordDialog.value.form.newPassword
    )
    
    if (res && res.code === 200) {
      ElMessage.success('密码修改成功')
      passwordDialog.value.visible = false
    } else {
      ElMessage.error(res?.message || '密码修改失败')
    }
  } catch (e) {
    if (e !== false) {
      console.error('修改密码失败', e)
      ElMessage.error(e?.response?.data?.message || '密码修改失败')
    }
  } finally {
    passwordDialog.value.loading = false
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

function generateCalendarDays(year, month, attendanceRecords) {
  const days = []
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  let firstDayOfWeek = firstDay.getDay()
  firstDayOfWeek = firstDayOfWeek === 0 ? 6 : firstDayOfWeek - 1
  
  const recordMap = {}
  attendanceRecords.forEach(record => {
    const date = new Date(record.date)
    const key = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`
    recordMap[key] = record
  })
  
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
  
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const daysInMonth = lastDay.getDate()
  for (let day = 1; day <= daysInMonth; day++) {
    const key = `${year}-${month}-${day}`
    const dateObj = new Date(year, month - 1, day)
    const isFuture = dateObj > today
    const record = recordMap[key]
    
    let status = 'future'
    if (record) {
      if (isFuture) {
        // 未来日期：显示排班信息
        if (record.shiftType === 'REST') {
          status = 'rest'
        } else if (record.shiftType) {
          status = 'scheduled'
        }
      } else {
        // 今天及过去：显示考勤状态（后端已修正）
        status = record.status === 'NORMAL' ? 'normal' : 
                 record.status === 'LATE' ? 'late' :
                 record.status === 'EARLY_LEAVE' ? 'early-leave' :
                 record.status === 'ABSENT' ? 'absent' :
                 record.status === 'REST' ? 'rest' : 'future'
      }
    }
    
    days.push({
      key: `current-${day}`,
      day: day,
      isCurrentMonth: true,
      status: status
    })
  }
  
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

async function loadMonthlyAttendance() {
  try {
    const year = calendarYear.value
    const month = calendarMonth.value
    currentMonth.value = `${year}年${month}月`
    
    const now = new Date()
    isCurrentMonth.value = (year === now.getFullYear() && month === now.getMonth() + 1)
    
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
        earlyLeave: statsRes.data.earlyLeaveDays || 0,
        absent: statsRes.data.absentDays || 0,
        rest: statsRes.data.restDays || 0
      }
    }
  } catch (e) {
    console.error('加载月度考勤失败', e)
  }
}

function changeMonth(delta) {
  let y = calendarYear.value
  let m = calendarMonth.value + delta
  if (m < 1) { m = 12; y-- }
  if (m > 12) { m = 1; y++ }
  calendarYear.value = y
  calendarMonth.value = m
  loadMonthlyAttendance()
}

async function loadStats() {
  try {
    const res = await getManagerDashboard()
    if (res?.code === 200 && res.data) {
      const d = res.data
      const indicators = d.energyIndicators || {}
      const pending = d.pendingItems || {}
      const trend = d.energyTrend || []
      stats.value = {
        monthlyReports: trend.length,
        energySaved: indicators.energyChangeRate != null ? Math.abs(indicators.energyChangeRate) : 0,
        optimizations: pending.total || 0,
        teamSize: (d.workshopRanking || []).length
      }
    }
  } catch (e) { console.error('加载统计数据失败', e) }
}

onMounted(() => {
  updateTime()
  timeInterval = setInterval(updateTime, 1000)
  
  loadUserInfo()
  loadTodayAttendance()
  loadMonthlyAttendance()
  loadStats()
})

onUnmounted(() => {
  if (timeInterval) {
    clearInterval(timeInterval)
  }
})
</script>

<style scoped>
/* 头像容器样式 */
.avatar-container {
  position: relative;
  cursor: pointer;
}

/* 头像上传悬浮层 */
.avatar-upload-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  cursor: pointer;
}

.avatar-container:hover .avatar-upload-overlay {
  opacity: 1;
}

.avatar-upload-overlay i {
  color: white;
  font-size: 24px;
}

/* 统计卡片样式 */
.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 8px;
}

.stat-label {
  color: #6b7280;
  font-size: 14px;
}

/* 打卡按钮样式 */
.clock-btn {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 3px solid #667eea;
  background: white;
  color: #667eea;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.clock-btn:hover:not(:disabled) {
  background: #667eea;
  color: white;
  transform: scale(1.05);
}

.clock-btn.clock-out {
  border-color: #f59e0b;
  color: #f59e0b;
}

.clock-btn.clock-out:hover:not(:disabled) {
  background: #f59e0b;
  color: white;
}

.clock-btn.clocked {
  border-color: #22c55e;
  color: #22c55e;
  cursor: not-allowed;
  opacity: 0.6;
}

.clock-btn i {
  font-size: 32px;
}

/* 日历样式 */
.calendar-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.calendar-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
  margin-bottom: 4px;
}

.calendar-weekday {
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  padding: 8px 0;
}

.calendar-body {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.calendar-day {
  height: 45px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  position: relative;
}

.calendar-day.other-month {
  opacity: 0.3;
}

.calendar-day.normal {
  background: #dcfce7;
  color: #16a34a;
}

.calendar-day.normal::after {
  content: '✓';
  position: absolute;
  bottom: 2px;
  right: 2px;
  font-size: 10px;
}

.calendar-day.late {
  background: #fef3c7;
  color: #d97706;
}

.calendar-day.late::after {
  content: '!';
  position: absolute;
  bottom: 2px;
  right: 2px;
  font-size: 10px;
}

.calendar-day.early-leave {
  background: #fed7aa;
  color: #ea580c;
}

.calendar-day.early-leave::after {
  content: '↓';
  position: absolute;
  bottom: 2px;
  right: 2px;
  font-size: 10px;
}

.calendar-day.absent {
  background: #fee2e2;
  color: #dc2626;
}

.calendar-day.absent::after {
  content: '×';
  position: absolute;
  bottom: 2px;
  right: 2px;
  font-size: 10px;
}

.calendar-day.rest {
  background: #f3f4f6;
  color: #6b7280;
}

.calendar-day.rest::after {
  content: '休';
  position: absolute;
  bottom: 2px;
  right: 2px;
  font-size: 8px;
}

.calendar-day.future {
  background: white;
  color: #9ca3af;
  border: 1.5px dashed #e5e7eb;
  border-radius: 20px;
}

.calendar-day.scheduled {
  background: #e0e7ff;
  color: #4f46e5;
}

.calendar-day.scheduled::after {
  content: '班';
  position: absolute;
  bottom: 2px;
  right: 2px;
  font-size: 8px;
}

/* 考勤统计图例 */
.attendance-day {
  border-radius: 4px;
  margin-right: 4px;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}
.attendance-day.normal {
  background: #dcfce7;
  color: #16a34a;
}
.attendance-day.late {
  background: #fef3c7;
  color: #d97706;
}
.attendance-day.early-leave {
  background: #fef3c7;
  color: #d97706;
}
.attendance-day.absent {
  background: #fee2e2;
  color: #dc2626;
}
.attendance-day.rest {
  background: #f3f4f6;
  color: #9ca3af;
}
</style>
