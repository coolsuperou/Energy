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

            👷

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



    <!-- 绩效统计 -->

    <div class="row g-4 mb-4">

      <div class="col-md-4">

        <div class="stat-card text-center">

          <div class="stat-value text-primary">{{ stats.monthlyCompleted }}</div>

          <div class="stat-label">本月完成工单</div>

        </div>

      </div>

      <div class="col-md-4">

        <div class="stat-card text-center">

          <div class="stat-value text-success">{{ stats.onTimeRate }}%</div>

          <div class="stat-label">按时完成率</div>

        </div>

      </div>

      <div class="col-md-4">

        <div class="stat-card text-center">

          <div class="stat-value" style="color: #8b5cf6;">{{ stats.totalCompleted.toLocaleString() }}</div>

          <div class="stat-label">累计工单</div>

        </div>

      </div>

    </div>



    <div class="row g-4 mb-4">

      <!-- 工作量趋势 -->

      <div class="col-lg-8">

        <div class="card h-100">

          <div class="card-header">近7天工作量统计</div>

          <div class="card-body">

            <div ref="workChartRef" style="height: 300px;"></div>

          </div>

        </div>

      </div>



      <!-- 右侧：技能认证 + 本周排班 -->

      <div class="col-lg-4 d-flex flex-column gap-4">

        <div class="card">

          <div class="card-header d-flex justify-content-between align-items-center">

            <span>技能认证</span>

            <button class="btn btn-sm btn-primary py-0 px-2" @click="showSkillApplyDialog">

              <i class="bi bi-plus me-1"></i>申请认证

            </button>

          </div>

          <div class="card-body p-0">

            <div v-for="skill in fixedSkills" :key="skill.name" class="p-3 border-bottom">

              <div class="d-flex justify-content-between align-items-center">

                <div>

                  <div class="fw-medium"><i :class="skill.icon" class="me-1"></i>{{ skill.name }}</div>

                  <div class="text-muted small">

                    <span v-if="skill.status === 'certified'">认证于 {{ skill.certifiedAt }}</span>

                    <span v-else-if="skill.status === 'rejected'" class="text-danger">已拒绝: {{ skill.rejectReason || '未说明原因' }}</span>

                    <span v-else-if="skill.status === 'pending'">审核中</span>

                    <span v-else class="text-secondary">尚未申请认证</span>

                  </div>

                </div>

                <el-tag :type="getSkillTagType(skill.status)" size="small">

                  {{ getSkillStatusText(skill.status) }}

                </el-tag>

              </div>

              <div v-if="skill.status === 'rejected'" class="mt-2">

                <button class="btn btn-sm btn-outline-primary py-0 px-2" @click="reapplySkill(skill)">

                  <i class="bi bi-arrow-repeat me-1"></i>重新申请

                </button>

              </div>

            </div>

          </div>

        </div>

        <div class="card">

          <div class="card-header d-flex justify-content-between align-items-center">

            <span><i class="bi bi-calendar-week me-1"></i>本周排班</span>

            <span class="text-muted small">{{ weekRange }}</span>

          </div>

          <div class="card-body p-2">

            <div class="schedule-compact">

              <div 

                v-for="(day, index) in weeklySchedule" 

                :key="index" 

                class="schedule-compact-day"

                :class="{ 'today': day.isToday, 'rest-day': day.isRest }">

                <div class="small fw-semibold">{{ day.weekday }}</div>

                <div class="schedule-shift-sm" :class="day.shiftClass">{{ day.shiftName }}</div>

              </div>

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



    <!-- 技能认证申请对话框 -->

    <el-dialog v-model="skillApplyDialog.visible" title="申请技能认证" width="450px">

      <el-form :model="skillApplyDialog.form" label-width="90px">

        <el-form-item label="技能名称" required>

          <el-select v-model="skillApplyDialog.form.skillName" placeholder="请选择技能认证" style="width: 100%;">

            <el-option label="电工设备" value="电工设备" />

            <el-option label="机械维修" value="机械维修" />

            <el-option label="电气安全" value="电气安全" />

          </el-select>

        </el-form-item>

        <el-form-item label="证书文件">

          <div>

            <el-upload

              :auto-upload="false"

              :show-file-list="true"

              :limit="1"

              accept="image/*,.pdf"

              :on-change="handleCertFileChange"

              :on-remove="handleCertFileRemove">

              <el-button size="small" type="primary" plain><i class="bi bi-upload me-1"></i>选择文件</el-button>

            </el-upload>

            <div class="text-muted small mt-1">支持图片或PDF，最大15MB（可选）</div>

          </div>

        </el-form-item>

      </el-form>

      <template #footer>

        <el-button @click="skillApplyDialog.visible = false">取消</el-button>

        <el-button type="primary" @click="submitSkillApply" :loading="skillApplyDialog.loading">提交申请</el-button>

      </template>

    </el-dialog>

  </div>

</template>



<script setup>

import { ref, computed, onMounted, onUnmounted } from 'vue'

import { ElMessage, ElMessageBox } from 'element-plus'

import * as echarts from 'echarts'

import { getCurrentUser, getUserStats, getWorkStats, updateUserInfo, uploadAvatar, getAvatar } from '@/api/user'

import { getTodayAttendance, getMonthlyAttendance, getAttendanceStats, clockIn, getWeeklySchedule } from '@/api/attendance'

import { getMySkills, applySkillCertification, reapplySkillCertification } from '@/api/inspection'



const workChartRef = ref(null)

const fileInputRef = ref(null)



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

  monthlyCompleted: 0,

  onTimeRate: 0,

  totalCompleted: 0

})



const skills = ref([])



// 固定3个技能，匹配后端已有认证数据

const FIXED_SKILL_LIST = [

  { name: '电工设备', icon: 'bi bi-lightning-charge' },

  { name: '机械维修', icon: 'bi bi-tools' },

  { name: '电气安全', icon: 'bi bi-shield-check' }

]



const fixedSkills = computed(() => {

  return FIXED_SKILL_LIST.map(fixed => {

    const match = skills.value.find(s => s.name === fixed.name)

    if (match) {

      return { ...match, icon: fixed.icon }

    }

    return { name: fixed.name, icon: fixed.icon, status: 'none', certifiedAt: null, rejectReason: null, id: null }

  })

})

const weeklySchedule = ref([])

const weekRange = ref('')



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



// 技能申请对话框

const skillApplyDialog = ref({

  visible: false,

  loading: false,

  form: {

    skillName: '',

    certFile: null

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

        department: res.data.department || '',

        avatarUrl: res.data.avatarUrl || ''

      }

      

      // 如果没有头像URL,尝试从头像接口获取

      if (!userInfo.value.avatarUrl) {

        loadAvatar()

      }

    }

  } catch (e) {

    console.error('加载用户信息失败', e)

  }

}



// 加载头像

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



// 触发文件选择

function triggerFileInput() {

  fileInputRef.value?.click()

}



// 处理文件选择

function handleFileSelect(event) {

  const file = event.target.files[0]

  if (!file) return

  

  // 验证文件类型

  if (!file.type.startsWith('image/')) {

    ElMessage.error('只能上传图片文件')

    return

  }

  

  // 验证文件大小(15MB)

  if (file.size > 15 * 1024 * 1024) {

    ElMessage.error('图片大小不能超过15MB')

    return

  }

  

  // 上传头像

  uploadAvatarFile(file)

}



// 上传头像文件

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

    // 清空文件选择器,允许重复选择同一文件

    if (fileInputRef.value) {

      fileInputRef.value.value = ''

    }

  }

}



// 加载统计数据

async function loadStats() {

  try {

    const res = await getUserStats()

    if (res && res.code === 200 && res.data) {

      stats.value = {

        monthlyCompleted: res.data.monthlyCompleted || 0,

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

    const res = await getMySkills()

    if (res && res.code === 200 && res.data) {

      skills.value = (res.data || []).map(item => ({

        id: item.id,

        name: item.skillName,

        icon: 'bi bi-award',

        status: item.status ? item.status.toLowerCase() : 'pending',

        certificateUrl: item.certificateUrl,

        rejectReason: item.rejectReason,

        certifiedAt: item.reviewedAt ? item.reviewedAt.substring(0, 10) : null

      }))

    }

  } catch (e) {

    console.error('加载技能认证失败', e)

  }

}



const weekdayNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']



function getShiftInfo(status) {

  switch (status) {

    case 'NORMAL':

      return { name: '正常班', icon: 'bi-sun', class: 'shift-normal', time: '08:00-17:00' }

    case 'DAY':

      return { name: '白班', icon: 'bi-sun', class: 'shift-day', time: '08:00-17:00' }

    case 'MORNING':

      return { name: '早班', icon: 'bi-sunrise', class: 'shift-morning', time: '06:00-14:00' }

    case 'AFTERNOON':

      return { name: '中班', icon: 'bi-sun', class: 'shift-afternoon', time: '14:00-22:00' }

    case 'NIGHT':

      return { name: '夜班', icon: 'bi-moon-stars', class: 'shift-night', time: '22:00-06:00' }

    case 'REST':

      return { name: '休息', icon: 'bi-house', class: 'shift-rest', time: '休息日' }

    default:

      return { name: '待排班', icon: 'bi-question-circle', class: 'shift-pending', time: '--' }

  }

}



// 加载本周排班

async function loadWeeklySchedule() {

  try {

    const now = new Date()

    const dayOfWeek = now.getDay()

    const monday = new Date(now)

    monday.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1))

    const sunday = new Date(monday)

    sunday.setDate(monday.getDate() + 6)

    

    weekRange.value = `${monday.getMonth() + 1}月${monday.getDate()}日 - ${sunday.getMonth() + 1}月${sunday.getDate()}日`

    

    const res = await getWeeklySchedule()

    const scheduleMap = {}

    

    if (res && res.code === 200 && res.data) {

      res.data.forEach(record => {

        const date = new Date(record.date)

        const key = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`

        scheduleMap[key] = record.shiftType || record.status

      })

    }

    

    const schedule = []

    for (let i = 0; i < 7; i++) {

      const date = new Date(monday)

      date.setDate(monday.getDate() + i)

      const key = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`

      const status = scheduleMap[key] || null

      const shiftInfo = getShiftInfo(status)

      const isToday = date.toDateString() === now.toDateString()

      

      schedule.push({

        weekday: weekdayNames[date.getDay()],

        date: `${date.getMonth() + 1}/${date.getDate()}`,

        shiftName: shiftInfo.name,

        shiftIcon: shiftInfo.icon,

        shiftClass: shiftInfo.class,

        workTime: shiftInfo.time,

        isToday: isToday,

        isRest: status === 'REST'

      })

    }

    

    weeklySchedule.value = schedule

  } catch (e) {

    console.error('加载本周排班失败', e)

    generateDefaultSchedule()

  }

}



function generateDefaultSchedule() {

  const now = new Date()

  const dayOfWeek = now.getDay()

  const monday = new Date(now)

  monday.setDate(now.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1))

  const sunday = new Date(monday)

  sunday.setDate(monday.getDate() + 6)

  

  weekRange.value = `${monday.getMonth() + 1}月${monday.getDate()}日 - ${sunday.getMonth() + 1}月${sunday.getDate()}日`

  

  const schedule = []

  for (let i = 0; i < 7; i++) {

    const date = new Date(monday)

    date.setDate(monday.getDate() + i)

    const isWeekend = date.getDay() === 0 || date.getDay() === 6

    // 巡检员按白班/夜班轮换

    const isNightShift = i % 2 === 1 && !isWeekend

    const shiftInfo = isWeekend ? getShiftInfo('REST') : (isNightShift ? getShiftInfo('NIGHT') : getShiftInfo('DAY'))

    const isToday = date.toDateString() === now.toDateString()

    

    schedule.push({

      weekday: weekdayNames[date.getDay()],

      date: `${date.getMonth() + 1}/${date.getDate()}`,

      shiftName: shiftInfo.name,

      shiftIcon: shiftInfo.icon,

      shiftClass: shiftInfo.class,

      workTime: shiftInfo.time,

      isToday: isToday,

      isRest: isWeekend

    })

  }

  

  weeklySchedule.value = schedule

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

  

  // 创建考勤记录映射表（保存完整记录信息）

  const recordMap = {}

  attendanceRecords.forEach(record => {

    const date = new Date(record.date)

    const key = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`

    recordMap[key] = record

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



function getSkillTagType(status) {

  const map = {

    certified: 'success',

    pending: 'warning',

    rejected: 'danger',

    none: 'info'

  }

  return map[status] || 'info'

}



function getSkillStatusText(status) {

  const map = {

    certified: '已认证',

    pending: '待审核',

    rejected: '已拒绝',

    none: '未认证'

  }

  return map[status] || status

}



function showSkillApplyDialog() {

  skillApplyDialog.value = {

    visible: true,

    loading: false,

    form: { skillName: '', certFile: null }

  }

}



function reapplySkill(skill) {

  ElMessageBox.confirm(

    `确定要重新申请「${skill.name}」的技能认证吗？`,

    '重新申请',

    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }

  ).then(async () => {

    try {

      const res = await reapplySkillCertification(skill.id)

      if (res?.code === 200) {

        ElMessage.success('已重新提交申请，请等待审核')

        loadSkills()

      } else {

        ElMessage.error(res?.message || '重新申请失败')

      }

    } catch (e) {

      ElMessage.error(e?.response?.data?.message || '重新申请失败')

    }

  }).catch(() => {})

}



function handleCertFileChange(file) {

  skillApplyDialog.value.form.certFile = file.raw

}



function handleCertFileRemove() {

  skillApplyDialog.value.form.certFile = null

}



async function submitSkillApply() {

  const { skillName, certFile } = skillApplyDialog.value.form

  if (!skillName.trim()) {

    ElMessage.warning('请输入技能名称')

    return

  }



  skillApplyDialog.value.loading = true

  try {

    let certificateUrl = null



    // 如果有证书文件，先上传

    if (certFile) {

      const formData = new FormData()

      formData.append('file', certFile)

      const uploadRes = await import('@/api/request').then(m => m.default.post('/inspector/skills/upload-certificate', formData, {

        headers: { 'Content-Type': 'multipart/form-data' }

      }))

      if (uploadRes?.code === 200 && uploadRes.data) {

        certificateUrl = uploadRes.data.fileUrl

      }

    }



    const res = await applySkillCertification({

      skillName: skillName.trim(),

      certificateUrl

    })



    if (res?.code === 200) {

      ElMessage.success('技能认证申请已提交，请等待审核')

      skillApplyDialog.value.visible = false

      loadSkills()

    } else {

      ElMessage.error(res?.message || '申请失败')

    }

  } catch (e) {

    console.error('提交技能申请失败', e)

    ElMessage.error(e?.response?.data?.message || '申请失败')

  } finally {

    skillApplyDialog.value.loading = false

  }

}



function initWorkChart(data) {

  if (!workChartRef.value) return

  

  const chart = echarts.init(workChartRef.value)

  // 后端返回 dates(如 "2/16"), completed, pending

  const days = data?.dates || []

  const completedTasks = data?.completed || []

  const inspections = data?.pending || []

  

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

  loadTodayAttendance()

  loadMonthlyAttendance()

  loadWeeklySchedule()

  loadWorkStats()

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

  /* aspect-ratio: 1; */

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



/* 排班信息样式 */

.schedule-grid {

  display: grid;

  grid-template-columns: repeat(7, 1fr);

  gap: 12px;

}



.schedule-day {

  background: #f8fafc;

  border-radius: 12px;

  padding: 16px 12px;

  text-align: center;

  transition: all 0.3s;

  border: 2px solid transparent;

}



.schedule-day:hover {

  transform: translateY(-2px);

  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

}



.schedule-day.today {

  border-color: #667eea;

  background: linear-gradient(135deg, #667eea10, #764ba210);

}



.schedule-day.rest-day {

  background: #f3f4f6;

}



.schedule-weekday {

  font-size: 14px;

  font-weight: 600;

  color: #374151;

  margin-bottom: 4px;

}



.schedule-date {

  font-size: 12px;

  color: #9ca3af;

  margin-bottom: 12px;

}



.schedule-shift {

  display: inline-flex;

  align-items: center;

  gap: 4px;

  padding: 6px 12px;

  border-radius: 20px;

  font-size: 13px;

  font-weight: 500;

  margin-bottom: 8px;

}



.schedule-shift i {

  font-size: 14px;

}



.schedule-time {

  font-size: 11px;

}



/* 班次颜色 */

.shift-normal {

  background: #dcfce7;

  color: #16a34a;

}



.shift-day {

  background: #dcfce7;

  color: #16a34a;

}



.shift-morning {

  background: #fef3c7;

  color: #d97706;

}



.shift-afternoon {

  background: #dbeafe;

  color: #2563eb;

}



.shift-night {

  background: #e0e7ff;

  color: #4f46e5;

}



.shift-rest {

  background: #f3f4f6;

  color: #6b7280;

}



.shift-pending {

  background: #fef2f2;

  color: #dc2626;

}



/* 响应式适配 */

@media (max-width: 992px) {

  .schedule-grid {

    grid-template-columns: repeat(4, 1fr);

  }

}



@media (max-width: 768px) {

  .schedule-grid {

    grid-template-columns: repeat(2, 1fr);

  }

}



@media (max-width: 576px) {

  .schedule-grid {

    grid-template-columns: 1fr;

  }

}



/* 紧凑排班样式 */

.schedule-compact {

  display: grid;

  grid-template-columns: repeat(7, 1fr);

  gap: 4px;

}



.schedule-compact-day {

  text-align: center;

  padding: 8px 4px;

  border-radius: 8px;

  background: #f8fafc;

  transition: all 0.2s;

}



.schedule-compact-day.today {

  border: 2px solid #667eea;

  background: linear-gradient(135deg, #667eea10, #764ba210);

}



.schedule-compact-day.rest-day {

  background: #f3f4f6;

}



.schedule-shift-sm {

  display: inline-block;

  padding: 2px 6px;

  border-radius: 10px;

  font-size: 11px;

  font-weight: 500;

  margin-top: 4px;

}

</style>

