<template>
  <div class="inspector-home">
    <!-- 欢迎横幅 -->
    <div class="ai-panel">
      <div class="d-flex justify-content-between align-items-center">
        <div>
          <h4 class="mb-1">{{ greeting }}，{{ userName }}！👋</h4>
          <p class="mb-0 opacity-75">今天是{{ weekDay }}，您有 <strong>{{ stats.inspectionTasks }}</strong> 项巡检任务和 <strong>{{ stats.pendingOrders }}</strong> 个待处理工单</p>
        </div>
        <div class="d-flex gap-2">
          <button class="btn btn-light btn-sm" @click="$router.push('/inspector/inspection')">
            <i class="bi bi-search me-1"></i>开始巡检
          </button>
          <button class="btn btn-light btn-sm" @click="$router.push('/inspector/equipment')">
            <i class="bi bi-card-checklist me-1"></i>查看工单
          </button>
        </div>
      </div>
    </div>

    <!-- 今日统计 -->
    <div class="row g-4 mb-4">
      <div class="col-sm-6 col-lg-3">
        <div class="stat-card">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon orange"><i class="bi bi-list-task"></i></div>
            <div>
              <div class="stat-value">{{ stats.inspectionTasks }}</div>
              <div class="stat-label">今日巡检任务</div>
            </div>
          </div>
          <div class="mt-2 small">
            <span class="text-success">已完成 {{ stats.completedInspections }}</span>
            <span class="text-muted mx-2">·</span>
            <span class="text-warning">进行中 {{ stats.ongoingInspections }}</span>
            <span class="text-muted mx-2">·</span>
            <span class="text-secondary">待开始 {{ stats.pendingInspections }}</span>
          </div>
        </div>
      </div>
      <div class="col-sm-6 col-lg-3">
        <div class="stat-card">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon blue"><i class="bi bi-tools"></i></div>
            <div>
              <div class="stat-value">{{ stats.pendingOrders }}</div>
              <div class="stat-label">待处理工单</div>
            </div>
          </div>
          <div class="mt-2 small">
            <span class="text-danger">紧急 {{ stats.urgentOrders }}</span>
            <span class="text-muted mx-2">·</span>
            <span class="text-warning">一般 {{ stats.normalOrders }}</span>
          </div>
        </div>
      </div>
      <div class="col-sm-6 col-lg-3">
        <div class="stat-card">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon green"><i class="bi bi-check-circle"></i></div>
            <div>
              <div class="stat-value">{{ stats.weekCompleted }}</div>
              <div class="stat-label">本周完成</div>
            </div>
          </div>
          <div class="mt-2 small text-success">
            <i class="bi bi-arrow-up"></i> 较上周 +{{ stats.weekGrowth }}%
          </div>
        </div>
      </div>
      <div class="col-sm-6 col-lg-3">
        <div class="stat-card">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon purple"><i class="bi bi-star"></i></div>
            <div>
              <div class="stat-value">{{ stats.rating }}</div>
              <div class="stat-label">服务评分</div>
            </div>
          </div>
          <div class="mt-2 small text-muted">
            本月收到 {{ stats.monthlyReviews }} 条好评
          </div>
        </div>
      </div>
    </div>

    <div class="row g-4">
      <!-- 今日任务 -->
      <div class="col-lg-8">
        <div class="card mb-4">
          <div class="card-header d-flex justify-content-between align-items-center">
            <span><i class="bi bi-calendar-check me-2"></i>今日任务</span>
            <a href="javascript:;" class="text-decoration-none small" @click="$router.push('/inspector/inspection')">查看全部 →</a>
          </div>
          <div class="card-body p-0" v-loading="loading">
            <div v-if="todayTasks.length === 0" class="p-4 text-center text-muted">
              <i class="bi bi-inbox" style="font-size: 48px;"></i>
              <p class="mt-2">暂无今日任务</p>
            </div>
            <div v-else>
              <div v-for="task in todayTasks" :key="task.id" class="task-item" :class="getTaskItemClass(task)">
                <div class="d-flex gap-3 align-items-start">
                  <div class="task-status-badge" :class="getStatusBadgeClass(task.status)">{{ getStatusText(task.status) }}</div>
                  <div class="flex-grow-1">
                    <div class="d-flex justify-content-between align-items-start">
                      <div>
                        <h6 class="mb-1">{{ task.title }}</h6>
                        <p class="text-muted small mb-0">
                          <i class="bi bi-geo-alt me-1"></i>{{ task.description || '位置信息' }}
                          <span class="mx-2">·</span>
                          <i class="bi bi-clock me-1"></i>{{ formatTaskTime(task) }}
                        </p>
                        <div v-if="task.status === 'IN_PROGRESS' && task.progress" class="d-flex align-items-center gap-2 mt-2">
                          <div class="progress flex-grow-1" style="height: 6px; max-width: 150px;">
                            <div class="progress-bar" :style="{ width: task.progress + '%' }"></div>
                          </div>
                          <span class="small text-muted">{{ task.progressText }}</span>
                        </div>
                      </div>
                      <button class="btn btn-sm" :class="getTaskButtonClass(task.status)" @click="handleTaskAction(task)">{{ getTaskButtonText(task.status) }}</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 本周工作量统计 -->
        <div class="card">
          <div class="card-header">本周工作量统计</div>
          <div class="card-body">
            <div ref="weekChartRef" style="height: 220px;"></div>
          </div>
        </div>
      </div>

      <!-- 右侧栏 -->
      <div class="col-lg-4">
        <!-- 快捷操作 -->
        <div class="card mb-4">
          <div class="card-header">快捷操作</div>
          <div class="card-body">
            <div class="row g-3">
              <div class="col-6">
                <a href="javascript:;" class="quick-action" @click="$router.push('/inspector/inspection')">
                  <div class="action-icon" style="background: #dbeafe;"><i class="bi bi-search text-primary"></i></div>
                  <span class="small">开始巡检</span>
                </a>
              </div>
              <div class="col-6">
                <a href="javascript:;" class="quick-action" @click="$router.push('/inspector/equipment')">
                  <div class="action-icon" style="background: #f3e8ff;"><i class="bi bi-hdd-stack" style="color: #8b5cf6;"></i></div>
                  <span class="small">设备台账</span>
                </a>
              </div>
              <div class="col-6">
                <a href="javascript:;" class="quick-action" @click="$router.push('/inspector/equipment')">
                  <div class="action-icon" style="background: #dcfce7;"><i class="bi bi-card-checklist text-success"></i></div>
                  <span class="small">我的工单</span>
                </a>
              </div>
              <div class="col-6">
                <a href="javascript:;" class="quick-action" @click="$router.push('/inspector/profile')">
                  <div class="action-icon" style="background: #fef3c7;"><i class="bi bi-clock-history text-warning"></i></div>
                  <span class="small">考勤打卡</span>
                </a>
              </div>
            </div>
          </div>
        </div>

        <!-- 预警设备 -->
        <div class="card mb-4">
          <div class="card-header d-flex justify-content-between align-items-center">
            <span><i class="bi bi-exclamation-triangle text-warning me-2"></i>预警设备</span>
            <span class="badge bg-warning text-dark">{{ warningDevices.length }}</span>
          </div>
          <div class="card-body p-0">
            <div v-if="warningDevices.length === 0" class="p-3 text-center text-muted">
              <p class="mb-0">暂无预警设备</p>
            </div>
            <div v-else>
              <div v-for="(device, index) in warningDevices" :key="device.id" class="p-3" :class="{ 'border-bottom': index < warningDevices.length - 1 }">
                <div class="d-flex justify-content-between align-items-center">
                  <div>
                    <div class="fw-semibold small">{{ device.name }}</div>
                    <div class="text-muted small">{{ device.location }} · {{ device.warning }}</div>
                  </div>
                  <span class="badge" :class="getDeviceBadgeClass(device.level)">{{ device.value }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 最新消息 -->
        <div class="card mb-4">
          <div class="card-header d-flex justify-content-between align-items-center">
            <span>最新消息</span>
            <a href="javascript:;" class="text-decoration-none small" @click="$router.push('/inspector/message')">全部 →</a>
          </div>
          <div class="card-body p-0">
            <div v-if="recentMessages.length === 0" class="p-3 text-center text-muted">
              <p class="mb-0">暂无消息</p>
            </div>
            <div v-else>
              <div v-for="(msg, index) in recentMessages" :key="msg.id" class="p-3" :class="{ 'border-bottom': index < recentMessages.length - 1 }">
                <div class="d-flex gap-3 align-items-start">
                  <div class="message-icon" :class="getMessageIconClass(msg.type)">
                    <i :class="msg.icon"></i>
                  </div>
                  <div class="flex-grow-1">
                    <div class="small">{{ msg.text }}</div>
                    <div class="text-muted" style="font-size: 12px;">{{ msg.time }}</div>
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyTasks, getMyTaskStats } from '@/api/task'
import * as echarts from 'echarts'

// 用户信息
const userName = ref('王五')

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

// 统计数据
const stats = ref({
  inspectionTasks: 3,        // 今日巡检任务
  completedInspections: 1,   // 已完成巡检
  ongoingInspections: 1,     // 进行中巡检
  pendingInspections: 1,     // 待开始巡检
  pendingOrders: 2,          // 待处理工单
  urgentOrders: 1,           // 紧急工单
  normalOrders: 1,           // 一般工单
  weekCompleted: 18,         // 本周完成
  weekGrowth: 12,            // 较上周增长
  rating: 4.9,               // 服务评分
  monthlyReviews: 23         // 本月好评
})

// 今日任务列表
const todayTasks = ref([
  {
    id: 1,
    title: '配电柜异常发热处理',
    description: '生产二车间 - 配电室',
    status: 'URGENT',
    taskType: 'REPAIR',
    assignedAt: '10:15',
    assignedBy: '调度员'
  },
  {
    id: 2,
    title: '生产二车间例行巡检',
    description: '配电室、UPS机房',
    status: 'IN_PROGRESS',
    taskType: 'INSPECTION',
    timeRange: '10:30 - 12:00',
    progress: 53,
    progressText: '8/15 项'
  },
  {
    id: 3,
    title: '装配车间例行巡检',
    description: '配电柜、照明系统',
    status: 'PENDING',
    taskType: 'INSPECTION',
    timeRange: '14:00 - 16:00',
    startIn: '2小时后开始'
  },
  {
    id: 4,
    title: '变压器温度检查',
    description: '仓储车间 - 3号变压器',
    status: 'ASSIGNED',
    taskType: 'MAINTENANCE',
    assignedAt: '11:00',
    assignedBy: '调度员'
  }
])

// 预警设备
const warningDevices = ref([
  { id: 1, name: '2号配电柜', location: '生产二车间', warning: '温度过高', value: '65°C', level: 'danger' },
  { id: 2, name: '3号变压器', location: '仓储车间', warning: '负载偏高', value: '92%', level: 'warning' },
  { id: 3, name: '电表A3', location: '装配车间', warning: '通讯异常', value: '离线', level: 'secondary' }
])

// 最新消息
const recentMessages = ref([
  { id: 1, icon: 'bi-file-earmark-text', text: '李四（调度员）给您分配了新工单', time: '10分钟前', type: 'info' },
  { id: 2, icon: 'bi-clock', text: '装配车间巡检任务即将开始', time: '30分钟前', type: 'warning' },
  { id: 3, icon: 'bi-star', text: '您收到一条5星好评', time: '2小时前', type: 'success' }
])

// 加载状态
const loading = ref(false)

// 图表引用
const weekChartRef = ref(null)
let weekChart = null

// 获取任务项样式类
function getTaskItemClass(task) {
  if (task.status === 'URGENT') return 'bg-danger bg-opacity-10'
  return ''
}

// 获取状态徽章样式
function getStatusBadgeClass(status) {
  const map = {
    URGENT: 'bg-danger text-white',
    IN_PROGRESS: 'bg-warning text-dark',
    PENDING: 'bg-secondary text-white',
    ASSIGNED: 'bg-info text-white'
  }
  return `task-status-badge ${map[status] || 'bg-secondary text-white'}`
}

// 获取状态文本
function getStatusText(status) {
  const map = {
    URGENT: '紧急',
    IN_PROGRESS: '进行中',
    PENDING: '待开始',
    ASSIGNED: '工单'
  }
  return map[status] || status
}

// 格式化任务时间
function formatTaskTime(task) {
  if (task.timeRange) return task.timeRange
  if (task.assignedBy && task.assignedAt) return `${task.assignedBy} ${task.assignedAt} 分配`
  if (task.startIn) return task.startIn
  return ''
}

// 获取任务按钮样式
function getTaskButtonClass(status) {
  const map = {
    URGENT: 'btn-danger',
    IN_PROGRESS: 'btn-primary',
    PENDING: 'btn-outline-secondary',
    ASSIGNED: 'btn-outline-primary'
  }
  return map[status] || 'btn-primary'
}

// 获取任务按钮文本
function getTaskButtonText(status) {
  const map = {
    URGENT: '立即处理',
    IN_PROGRESS: '继续巡检',
    PENDING: '2小时后开始',
    ASSIGNED: '开始处理'
  }
  return map[status] || '开始'
}

// 任务操作
function handleTaskAction(task) {
  if (task.status === 'PENDING') {
    ElMessage.info('任务还未到开始时间')
    return
  }
  ElMessage.success('跳转到任务详情页面')
}

// 获取设备徽章样式
function getDeviceBadgeClass(level) {
  const map = {
    danger: 'bg-danger',
    warning: 'bg-warning text-dark',
    secondary: 'bg-secondary'
  }
  return map[level] || 'bg-secondary'
}

// 获取消息图标样式
function getMessageIconClass(type) {
  const map = {
    info: 'text-primary',
    warning: 'text-warning',
    success: 'text-success'
  }
  return map[type] || 'text-secondary'
}

// 初始化图表
function initWeekChart() {
  if (!weekChartRef.value) return
  
  weekChart = echarts.init(weekChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['完成工单', '设备巡检'],
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
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '完成工单',
        type: 'bar',
        data: [3, 4, 2, 5, 3, 0, 1],
        itemStyle: { color: '#3b82f6' }
      },
      {
        name: '设备巡检',
        type: 'bar',
        data: [2, 3, 4, 2, 3, 0, 0],
        itemStyle: { color: '#22c55e' }
      }
    ]
  }
  
  weekChart.setOption(option)
}

// 加载统计数据
async function loadStats() {
  try {
    const res = await getMyTaskStats()
    if (res && res.code === 200 && res.data) {
      Object.assign(stats.value, res.data)
    }
  } catch (e) {
    console.error('加载统计数据失败', e)
  }
}

// 加载今日任务
async function loadTodayTasks() {
  loading.value = true
  try {
    const res = await getMyTasks({ limit: 10 })
    if (res && res.code === 200 && res.data) {
      // todayTasks.value = res.data.records || res.data || []
    }
  } catch (e) {
    console.error('加载任务列表失败', e)
  }
  loading.value = false
}

// 页面加载时调用
onMounted(() => {
  loadStats()
  loadTodayTasks()
  
  nextTick(() => {
    initWeekChart()
  })
  
  // 窗口大小改变时重绘图表
  window.addEventListener('resize', () => {
    weekChart?.resize()
  })
})
</script>

<style lang="scss">
@use '@/styles/inspector.scss';
</style>
