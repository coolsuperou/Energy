<template>
  <div class="dispatcher-home" v-loading="loading">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card" @click="goTo('/dispatcher/approval')">
        <div class="stat-icon orange"><i class="bi bi-clipboard-check"></i></div>
        <div class="stat-info">
          <div class="stat-value">
            {{ stats.pendingApplicationCount }}
            <el-badge v-if="stats.pendingApplicationCount > 0" :value="stats.pendingApplicationCount" class="stat-badge" />
          </div>
          <div class="stat-label">待审批申请</div>
        </div>
      </div>
      <div class="stat-card" @click="goTo('/dispatcher/order')">
        <div class="stat-icon blue"><i class="bi bi-list-task"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.inProgressTaskCount }}</div>
          <div class="stat-label">进行中工单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="bi bi-check-circle"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ todayMetrics.todayApprovedCount }}</div>
          <div class="stat-label">今日已审批</div>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <!-- 今日关键指标 -->
      <div class="card metrics-card">
        <div class="card-header">
          <span><i class="bi bi-graph-up me-2"></i>今日关键指标</span>
          <small class="text-muted">更新于 {{ currentTime }}</small>
        </div>
        <div class="card-body">
          <div class="metrics-list">
            <div class="metric-row">
              <div class="metric-icon blue"><i class="bi bi-lightning-charge"></i></div>
              <div class="metric-label">今日用电量</div>
              <div class="metric-value">{{ formatNumber(todayMetrics.todayTotalEnergy) }} <span class="metric-unit">kWh</span></div>
            </div>
            <div class="metric-row">
              <div class="metric-icon green"><i class="bi bi-currency-yen"></i></div>
              <div class="metric-label">今日费用</div>
              <div class="metric-value">{{ formatNumber(todayMetrics.todayTotalCost) }} <span class="metric-unit">元</span></div>
            </div>
            <div class="metric-row">
              <div class="metric-icon purple"><i class="bi bi-calendar-month"></i></div>
              <div class="metric-label">本月用电量</div>
              <div class="metric-value">{{ formatNumber(todayMetrics.monthTotalEnergy) }} <span class="metric-unit">kWh</span></div>
            </div>
            <div class="metric-row">
              <div class="metric-icon orange"><i class="bi bi-clipboard-check"></i></div>
              <div class="metric-label">今日审批数</div>
              <div class="metric-value">{{ todayMetrics.todayApprovedCount }} <span class="metric-unit">件</span></div>
            </div>
            <div class="metric-row">
              <div class="metric-icon teal"><i class="bi bi-check2-square"></i></div>
              <div class="metric-label">完成工单数</div>
              <div class="metric-value">{{ todayMetrics.todayCompletedTaskCount }} <span class="metric-unit">件</span></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷入口 -->
      <div class="card">
        <div class="card-header">快捷入口</div>
        <div class="card-body">
          <div class="action-grid">
            <router-link to="/dispatcher/approval" class="quick-action">
              <div class="action-icon orange">
                <i class="bi bi-clipboard-check"></i>
                <el-badge v-if="stats.pendingApplicationCount > 0" :value="stats.pendingApplicationCount" class="action-badge" />
              </div>
              <span>审批管理</span>
            </router-link>
            <router-link to="/dispatcher/task" class="quick-action">
              <div class="action-icon blue"><i class="bi bi-list-task"></i></div>
              <span>工单管理</span>
            </router-link>
            <router-link to="/dispatcher/dispatch" class="quick-action">
              <div class="action-icon green"><i class="bi bi-sliders"></i></div>
              <span>调度管理</span>
            </router-link>
            <router-link to="/dispatcher/energy" class="quick-action">
              <div class="action-icon purple"><i class="bi bi-graph-up"></i></div>
              <span>能耗统计</span>
            </router-link>
            <router-link to="/dispatcher/report" class="quick-action">
              <div class="action-icon teal"><i class="bi bi-file-earmark-bar-graph"></i></div>
              <span>报表中心</span>
            </router-link>
          </div>
        </div>
      </div>

      <!-- 待审批申请 -->
      <div class="card pending-approval">
        <div class="card-header">
          <span>待审批申请</span>
          <router-link to="/dispatcher/approval" class="card-link">查看全部 →</router-link>
        </div>
        <div class="card-body">
          <div v-for="item in pendingApplications" :key="item.id" 
               :class="['order-card', { urgent: item.urgency === 'CRITICAL' || item.urgency === 'critical' }]">
            <div class="order-header">
              <div class="order-info">
                <h6>{{ item.userName || '车间用户' }} - {{ item.purpose || '用电申请' }}</h6>
                <div class="order-meta">
                  <span><i class="bi bi-calendar"></i>{{ item.applyDate }} {{ formatTime(item.startTime) }}-{{ formatTime(item.endTime) }}</span>
                  <span><i class="bi bi-lightning-charge"></i>{{ item.power }} kW</span>
                </div>
              </div>
              <span :class="['badge', getUrgencyClass(item.urgency)]">{{ getUrgencyText(item.urgency) }}</span>
            </div>
            <div class="order-actions">
              <button class="btn btn-success btn-sm" @click="handleApprove(item.id)">
                <i class="bi bi-check-lg"></i>批准
              </button>
              <button class="btn btn-outline-warning btn-sm" @click="goToApproval(item.id)">调整</button>
              <button class="btn btn-outline-danger btn-sm" @click="handleReject(item.id)">拒绝</button>
            </div>
          </div>
          <div v-if="pendingApplications.length === 0" class="empty-state">
            <i class="bi bi-inbox"></i>
            <span>暂无待审批申请</span>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDashboard } from '@/api/dispatcher'
import { getPendingApplications, approveApplication, rejectApplication } from '@/api/application'

const router = useRouter()
const loading = ref(false)
const currentTime = ref('')

// 统计数据
const stats = reactive({
  pendingApplicationCount: 0,
  inProgressTaskCount: 0
})

// 今日关键指标
const todayMetrics = reactive({
  todayTotalEnergy: 0,
  todayTotalCost: 0,
  monthTotalEnergy: 0,
  todayApprovedCount: 0,
  todayCompletedTaskCount: 0
})

// 待审批申请
const pendingApplications = ref([])

// 格式化数字
function formatNumber(num) {
  if (num === null || num === undefined) return '0'
  const n = parseFloat(num)
  if (isNaN(n)) return '0'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

// 格式化时间
function formatTime(time) {
  if (!time) return ''
  return time.substring(0, 5)
}

// 获取紧急程度样式
function getUrgencyClass(urgency) {
  const u = (urgency || '').toUpperCase()
  const map = { 'CRITICAL': 'bg-danger', 'URGENT': 'bg-warning', 'NORMAL': 'bg-secondary' }
  return map[u] || 'bg-secondary'
}

// 获取紧急程度文本
function getUrgencyText(urgency) {
  const u = (urgency || '').toUpperCase()
  const map = { 'CRITICAL': '紧急', 'URGENT': '加急', 'NORMAL': '普通' }
  return map[u] || '待审批'
}

// 跳转到指定页面
function goTo(path) {
  router.push(path)
}

// 加载工作台数据
async function loadDashboard() {
  try {
    const res = await getDashboard()
    if (res && res.code === 200 && res.data) {
      const data = res.data
      
      // 更新统计数据
      stats.pendingApplicationCount = data.pendingApplicationCount || 0
      stats.inProgressTaskCount = data.inProgressTaskCount || 0
      
      // 更新今日关键指标
      if (data.todayMetrics) {
        todayMetrics.todayTotalEnergy = data.todayMetrics.todayTotalEnergy || 0
        todayMetrics.todayTotalCost = data.todayMetrics.todayTotalCost || 0
        todayMetrics.monthTotalEnergy = data.todayMetrics.monthTotalEnergy || 0
        todayMetrics.todayApprovedCount = data.todayMetrics.todayApprovedCount || 0
        todayMetrics.todayCompletedTaskCount = data.todayMetrics.todayCompletedTaskCount || 0
      }
    }
  } catch (e) {
    console.warn('加载工作台数据失败', e)
  }
}

// 加载待审批申请
async function loadPendingApplications() {
  try {
    const res = await getPendingApplications({ page: 1, size: 100 })
    if (res && res.code === 200 && res.data) {
      const data = res.data.records || res.data || []
      const urgencyOrder = { 'CRITICAL': 0, 'URGENT': 1, 'NORMAL': 2 }
      data.sort((a, b) => {
        const oa = urgencyOrder[(a.urgency || '').toUpperCase()] ?? 3
        const ob = urgencyOrder[(b.urgency || '').toUpperCase()] ?? 3
        return oa - ob
      })
      pendingApplications.value = data.slice(0, 3)
    }
  } catch (e) {
    console.warn('加载待审批申请失败', e)
    pendingApplications.value = []
  }
}

// 加载所有数据
async function loadAllData() {
  loading.value = true
  try {
    await Promise.all([
      loadDashboard(),
      loadPendingApplications()
    ])
  } finally {
    loading.value = false
  }
}

// 批准申请
async function handleApprove(id) {
  try {
    await ElMessageBox.confirm('确认批准该用电申请？', '确认', { type: 'info' })
    const res = await approveApplication(id, { comment: '批准' })
    if (res.code === 200) {
      ElMessage.success('审批成功，已通知申请人')
      loadDashboard()
      loadPendingApplications()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('审批失败', e)
    }
  }
}

// 拒绝申请
async function handleReject(id) {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', {
      inputPlaceholder: '峰时负载已满，建议调整至谷时段',
      inputType: 'textarea'
    })
    const res = await rejectApplication(id, { comment: value || '拒绝' })
    if (res.code === 200) {
      ElMessage.success('已拒绝，已通知申请人')
      loadDashboard()
      loadPendingApplications()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('拒绝失败', e)
    }
  }
}

// 跳转到审批页面
function goToApproval(id) {
  router.push('/dispatcher/approval')
}

onMounted(() => {
  const now = new Date()
  currentTime.value = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`
  
  loadAllData()
})
</script>

<style lang="scss">
@use '@/styles/dispatcher.scss';

// 今日关键指标样式
.metrics-card {
  .metrics-list {
    display: flex;
    flex-direction: column;
    gap: 0;
  }
  
  .metric-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    border-bottom: 1px solid #f1f5f9;
    transition: background 0.2s;
    
    &:last-child { border-bottom: none; }
    &:hover { background: #f8fafc; }
  }
  
  .metric-icon {
    width: 36px;
    height: 36px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1rem;
    flex-shrink: 0;
    
    &.blue { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
    &.green { background: rgba(34, 197, 94, 0.1); color: #22c55e; }
    &.orange { background: rgba(249, 115, 22, 0.1); color: #f97316; }
    &.purple { background: rgba(139, 92, 246, 0.1); color: #8b5cf6; }
    &.teal { background: rgba(20, 184, 166, 0.1); color: #14b8a6; }
  }
  
  .metric-label {
    flex: 1;
    font-size: 0.875rem;
    color: #64748b;
  }
  
  .metric-value {
    font-size: 1.125rem;
    font-weight: 700;
    color: #1e293b;
    text-align: right;
    
    .metric-unit {
      font-size: 0.75rem;
      font-weight: 400;
      color: #94a3b8;
      margin-left: 2px;
    }
  }
}

// 快捷入口样式
.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem;
  
  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.quick-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background: #f8fafc;
  border-radius: 8px;
  text-decoration: none;
  color: #475569;
  transition: all 0.2s;
  
  &:hover {
    background: #f1f5f9;
    transform: translateY(-2px);
    color: #1e293b;
  }
  
  .action-icon {
    position: relative;
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 1.5rem;
    
    &.blue { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
    &.green { background: rgba(34, 197, 94, 0.1); color: #22c55e; }
    &.orange { background: rgba(249, 115, 22, 0.1); color: #f97316; }
    &.red { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
    &.purple { background: rgba(139, 92, 246, 0.1); color: #8b5cf6; }
    &.teal { background: rgba(20, 184, 166, 0.1); color: #14b8a6; }
    
    .action-badge {
      position: absolute;
      top: -5px;
      right: -5px;
    }
  }
  
  span {
    font-size: 0.875rem;
    font-weight: 500;
  }
}

// 统计卡片 badge 样式
.stat-card {
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }
  
  .stat-value {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  
  .stat-badge {
    margin-left: 0.25rem;
  }
}
</style>
