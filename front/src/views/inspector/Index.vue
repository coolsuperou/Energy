<template>
  <div class="inspector-home">
    <!-- 欢迎横幅 -->
    <div class="ai-panel">
      <div class="d-flex justify-content-between align-items-center">
        <div>
          <h4 class="mb-1">{{ greeting }}，{{ userName }}！👋</h4>
          <p class="mb-0 opacity-75">今天是{{ weekDay }}，您有 <strong>{{ taskStats.inProgressCount }}</strong> 项进行中任务和 <strong>{{ taskStats.pendingCount }}</strong> 个待处理工单</p>
        </div>
        <div class="d-flex gap-2">
          <button class="btn btn-light btn-sm" @click="$router.push('/inspector/equipment')">
            <i class="bi bi-card-checklist me-1"></i>查看工单
          </button>
        </div>
      </div>
    </div>

    <!-- 任务统计 -->
    <div class="row g-4 mb-4">
      <div class="col-sm-6 col-lg-3">
        <div class="stat-card">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon orange"><i class="bi bi-hourglass-split"></i></div>
            <div>
              <div class="stat-value">{{ taskStats.pendingCount }}</div>
              <div class="stat-label">待处理任务</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-sm-6 col-lg-3">
        <div class="stat-card">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon blue"><i class="bi bi-play-circle"></i></div>
            <div>
              <div class="stat-value">{{ taskStats.inProgressCount }}</div>
              <div class="stat-label">进行中任务</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-sm-6 col-lg-3">
        <div class="stat-card">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon green"><i class="bi bi-check-circle"></i></div>
            <div>
              <div class="stat-value">{{ taskStats.completedCount }}</div>
              <div class="stat-label">本月已完成</div>
            </div>
          </div>
          <div class="mt-2 small">
            <span class="text-success">今日完成 {{ taskStats.todayCompletedCount }}</span>
            <span class="text-muted mx-2">·</span>
            <span class="text-primary">本周 {{ taskStats.weekCompletedCount }}</span>
          </div>
        </div>
      </div>
      <div class="col-sm-6 col-lg-3">
        <div class="stat-card">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon purple"><i class="bi bi-graph-up"></i></div>
            <div>
              <div class="stat-value">{{ taskStats.onTimeRate }}%</div>
              <div class="stat-label">按时完成率</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="row g-4">
      <!-- 今日待完成任务 -->
      <div class="col-lg-8">
        <div class="card mb-4">
          <div class="card-header d-flex justify-content-between align-items-center">
            <span><i class="bi bi-calendar-check me-2"></i>今日待完成任务</span>
            <a href="javascript:;" class="text-decoration-none small" @click="$router.push('/inspector/equipment')">查看全部 →</a>
          </div>
          <div class="card-body p-0" v-loading="loading">
            <div v-if="todayTasks.length === 0" class="p-4 text-center text-muted">
              <i class="bi bi-inbox" style="font-size: 48px;"></i>
              <p class="mt-2">暂无今日待完成任务</p>
            </div>
            <div v-else>
              <div v-for="task in todayTasks" :key="task.id" class="task-item">
                <div class="d-flex gap-3 align-items-start">
                  <div class="task-status-badge" :class="getStatusBadgeClass(task.status)">{{ getStatusText(task.status) }}</div>
                  <div class="flex-grow-1">
                    <div class="d-flex justify-content-between align-items-start">
                      <div>
                        <h6 class="mb-1">{{ task.title }}</h6>
                        <p class="text-muted small mb-0">
                          <i class="bi bi-tag me-1"></i>{{ getTaskTypeText(task.taskType) }}
                          <span class="mx-2">·</span>
                          <i class="bi bi-flag me-1"></i>{{ getPriorityText(task.priority) }}
                          <span v-if="task.dueDate" class="mx-2">·</span>
                          <span v-if="task.dueDate"><i class="bi bi-clock me-1"></i>截止: {{ task.dueDate }}</span>
                        </p>
                      </div>
                      <el-tag :type="getPriorityTagType(task.priority)" size="small">{{ getPriorityText(task.priority) }}</el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 工作量趋势图 -->
        <div class="card">
          <div class="card-header">近7天工作量趋势</div>
          <div class="card-body">
            <div ref="trendChartRef" style="height: 250px;"></div>
          </div>
        </div>
      </div>

      <!-- 右侧栏 -->
      <div class="col-lg-4">
        <!-- 技能认证（与个人中心一致） -->
        <div class="card mb-4">
          <div class="card-header d-flex justify-content-between align-items-center">
            <span>技能认证</span>
            <button class="btn btn-sm btn-primary py-0 px-2" type="button" @click="showSkillApplyDialog">
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
                <button type="button" class="btn btn-sm btn-outline-primary py-0 px-2" @click="reapplySkill(skill)">
                  <i class="bi bi-arrow-repeat me-1"></i>重新申请
                </button>
              </div>
            </div>
          </div>
        </div>

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
                  :on-remove="handleCertFileRemove"
                >
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

        <!-- 快捷操作 -->
        <div class="card mb-4">
          <div class="card-header">快捷操作</div>
          <div class="card-body">
            <div class="row g-3">
              <div class="col-6">
                <a href="javascript:;" class="quick-action" @click="$router.push('/inspector/equipment')">
                  <div class="action-icon" style="background: #dcfce7;"><i class="bi bi-card-checklist text-success"></i></div>
                  <span class="small">我的工单</span>
                </a>
              </div>
              <div class="col-6">
                <a href="javascript:;" class="quick-action" @click="$router.push('/inspector/message')">
                  <div class="action-icon" style="background: #dbeafe;"><i class="bi bi-bell text-primary"></i></div>
                  <span class="small">消息中心</span>
                </a>
              </div>
              <div class="col-6">
                <a href="javascript:;" class="quick-action" @click="$router.push('/inspector/profile')">
                  <div class="action-icon" style="background: #fef3c7;"><i class="bi bi-clock-history text-warning"></i></div>
                  <span class="small">考勤打卡</span>
                </a>
              </div>
              <div class="col-6">
                <a href="javascript:;" class="quick-action" @click="$router.push('/inspector/profile')">
                  <div class="action-icon" style="background: #f3e8ff;"><i class="bi bi-person" style="color: #8b5cf6;"></i></div>
                  <span class="small">个人中心</span>
                </a>
              </div>
            </div>
          </div>
        </div>

        <!-- 最新消息 -->
        <div class="card">
          <div class="card-header d-flex justify-content-between align-items-center">
            <span>最新消息</span>
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="me-2">
              <a href="javascript:;" class="text-decoration-none small" @click="$router.push('/inspector/message')">全部 →</a>
            </el-badge>
          </div>
          <div class="card-body p-0">
            <div v-if="recentMessages.length === 0" class="p-3 text-center text-muted">
              <p class="mb-0">暂无消息</p>
            </div>
            <div v-else>
              <div v-for="(msg, index) in recentMessages" :key="msg.id" class="p-3" :class="{ 'border-bottom': index < recentMessages.length - 1 }">
                <div class="d-flex gap-3 align-items-start">
                  <div class="message-icon" :class="getMessageIconClass(msg.type)">
                    <i :class="getNotificationIcon(msg.type)"></i>
                  </div>
                  <div class="flex-grow-1">
                    <div class="small">{{ msg.content }}</div>
                    <div class="text-muted" style="font-size: 12px;">{{ formatTime(msg.createdAt) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>


<script setup>
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getInspectorDashboard, applySkillCertification, getMySkills, reapplySkillCertification } from '@/api/inspection'
import { getCurrentUser } from '@/api/user'
import { getMyNotifications } from '@/api/notification'
import * as echarts from 'echarts'

// 用户信息
const userName = ref('')

// 问候语
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 9) return '早上好'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  if (hour < 22) return '晚上好'
  return '晚安'
})

// 星期
const weekDay = computed(() => {
  const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return days[new Date().getDay()]
})

// 任务统计数据
const taskStats = ref({
  pendingCount: 0,
  inProgressCount: 0,
  completedCount: 0,
  todayCompletedCount: 0,
  weekCompletedCount: 0,
  onTimeRate: 100
})

// 今日待完成任务列表
const todayTasks = ref([])

// 工作量趋势数据
const workloadTrend = ref([])

// 技能列表
const skills = ref([])

// 固定3个技能，与个人中心、后端数据一致
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

// 未读消息数
const unreadCount = ref(0)

// 最新消息
const recentMessages = ref([])

// 加载状态
const loading = ref(false)

// 图表引用
const trendChartRef = ref(null)
let trendChart = null

// 技能认证申请（与个人中心一致）
const skillApplyDialog = ref({
  visible: false,
  loading: false,
  form: {
    skillName: '',
    certFile: null
  }
})

// 获取状态徽章样式
function getStatusBadgeClass(status) {
  const map = {
    IN_PROGRESS: 'bg-primary text-white',
    PENDING: 'bg-warning text-dark',
    COMPLETED: 'bg-success text-white'
  }
  return map[status] || 'bg-secondary text-white'
}

// 获取状态文本
function getStatusText(status) {
  const map = {
    IN_PROGRESS: '进行中',
    PENDING: '待处理',
    COMPLETED: '已完成'
  }
  return map[status] || status
}

// 获取任务类型文本
function getTaskTypeText(type) {
  const map = {
    INSPECTION: '巡检',
    REPAIR: '维修',
    MAINTENANCE: '维护'
  }
  return map[type] || type
}

// 获取优先级文本
function getPriorityText(priority) {
  const map = {
    LOW: '低',
    NORMAL: '普通',
    HIGH: '高',
    URGENT: '紧急'
  }
  return map[priority] || priority
}

// 获取优先级标签类型
function getPriorityTagType(priority) {
  const map = {
    LOW: 'info',
    NORMAL: '',
    HIGH: 'warning',
    URGENT: 'danger'
  }
  return map[priority] || ''
}

// 获取技能状态标签类型（与个人中心 getSkillTagType 一致）
function getSkillTagType(status) {
  const map = {
    certified: 'success',
    pending: 'warning',
    rejected: 'danger',
    none: 'info'
  }
  return map[status] || 'info'
}

// 获取技能状态文本
function getSkillStatusText(status) {
  const map = {
    certified: '已认证',
    pending: '待审核',
    rejected: '已拒绝',
    none: '未认证'
  }
  return map[status] || status
}

// 获取消息图标样式
function getMessageIconClass(type) {
  const map = {
    TASK: 'text-primary',
    ALERT: 'text-warning',
    SYSTEM: 'text-secondary'
  }
  return map[type] || 'text-secondary'
}

// 获取通知图标
function getNotificationIcon(type) {
  const map = {
    TASK: 'bi bi-file-earmark-text',
    ALERT: 'bi bi-exclamation-triangle',
    SYSTEM: 'bi bi-info-circle'
  }
  return map[type] || 'bi bi-bell'
}

// 格式化时间
function formatTime(timestamp) {
  if (!timestamp) return ''
  const now = new Date()
  const time = new Date(timestamp)
  const diff = Math.floor((now - time) / 1000)
  
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 604800) return `${Math.floor(diff / 86400)}天前`
  return time.toLocaleDateString()
}


// 初始化工作量趋势图表
function initTrendChart() {
  if (!trendChartRef.value) return
  
  trendChart = echarts.init(trendChartRef.value)
  
  // 使用真实日期 M/D 格式作为 x 轴
  const days = workloadTrend.value.map(item => {
    if (item.date) {
      const d = new Date(item.date)
      return (d.getMonth() + 1) + '/' + d.getDate()
    }
    return item.dayOfWeek
  })
  const completedData = workloadTrend.value.map(item => item.completedCount)
  const assignedData = workloadTrend.value.map(item => item.assignedCount)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['完成工单', '新分配任务'],
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: days
    },
    yAxis: {
      type: 'value',
      minInterval: 1
    },
    series: [
      {
        name: '完成工单',
        type: 'bar',
        data: completedData,
        itemStyle: { color: '#22c55e' }
      },
      {
        name: '新分配任务',
        type: 'bar',
        data: assignedData,
        itemStyle: { color: '#3b82f6' }
      }
    ]
  }
  
  trendChart.setOption(option)
}

// 加载用户信息
async function loadUserInfo() {
  try {
    const res = await getCurrentUser()
    if (res && res.code === 200 && res.data) {
      userName.value = res.data.name || ''
    }
  } catch (e) {
    console.error('加载用户信息失败', e)
  }
}

// 加载工作台数据
async function loadDashboard() {
  loading.value = true
  try {
    const res = await getInspectorDashboard()
    if (res && res.code === 200 && res.data) {
      const data = res.data
      
      // 任务统计
      if (data.taskStats) {
        taskStats.value = {
          pendingCount: data.taskStats.pendingCount || 0,
          inProgressCount: data.taskStats.inProgressCount || 0,
          completedCount: data.taskStats.completedCount || 0,
          todayCompletedCount: data.taskStats.todayCompletedCount || 0,
          weekCompletedCount: data.taskStats.weekCompletedCount || 0,
          onTimeRate: data.taskStats.onTimeRate || 100
        }
      }
      
      // 今日待完成任务
      todayTasks.value = data.todayTasks || []
      
      // 工作量趋势
      workloadTrend.value = data.workloadTrend || []
      
      // 未读消息数
      unreadCount.value = data.unreadCount || 0
      
      // 初始化图表
      nextTick(() => {
        initTrendChart()
      })
    }
  } catch (e) {
    console.error('加载工作台数据失败', e)
    ElMessage.error('加载工作台数据失败')
  }
  loading.value = false
}

// 加载技能认证（与个人中心 loadSkills 相同数据源）
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

// 加载最新消息
async function loadRecentMessages() {
  try {
    const res = await getMyNotifications({ page: 1, size: 3 })
    if (res && res.code === 200 && res.data) {
      recentMessages.value = res.data.records || res.data || []
    }
  } catch (e) {
    console.error('加载消息失败', e)
  }
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
  )
    .then(async () => {
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
    })
    .catch(() => {})
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

    if (certFile) {
      const formData = new FormData()
      formData.append('file', certFile)
      const uploadRes = await import('@/api/request').then(m =>
        m.default.post('/inspector/skills/upload-certificate', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
      )
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

// 窗口大小改变处理
function handleResize() {
  trendChart?.resize()
}

// 页面加载时调用
onMounted(() => {
  loadUserInfo()
  loadDashboard()
  loadSkills()
  loadRecentMessages()
  
  window.addEventListener('resize', handleResize)
})

// 页面卸载时清理
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style lang="scss">
@use '@/styles/inspector.scss';
</style>
