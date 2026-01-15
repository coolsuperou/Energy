<template>
  <div class="dispatcher-home">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon orange"><i class="bi bi-clipboard-check"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.pendingApproval }}</div>
          <div class="stat-label">待审批申请</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red"><i class="bi bi-exclamation-triangle"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.pendingAlerts }}</div>
          <div class="stat-label">待处理预警</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon blue"><i class="bi bi-speedometer2"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.loadRate }}%</div>
          <div class="stat-label">当前负荷率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="bi bi-check-circle"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayApproved }}</div>
          <div class="stat-label">今日已审批</div>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <!-- 负荷监控 -->
      <div class="card load-monitor">
        <div class="card-header">全厂负荷监控</div>
        <div class="card-body">
          <div class="load-chart">
            <svg width="180" height="180" viewBox="0 0 180 180">
              <circle cx="90" cy="90" r="70" fill="none" stroke="#e2e8f0" stroke-width="14"/>
              <circle cx="90" cy="90" r="70" fill="none" stroke="url(#gradient)" stroke-width="14" 
                  :stroke-dasharray="440" :stroke-dashoffset="440 - 440 * stats.loadRate / 100" stroke-linecap="round"
                  transform="rotate(-90 90 90)"/>
              <defs>
                <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" style="stop-color:#3b82f6"/>
                  <stop offset="100%" style="stop-color:#8b5cf6"/>
                </linearGradient>
              </defs>
            </svg>
            <div class="load-value">
              <div class="value">{{ stats.loadRate }}%</div>
              <div class="label">负荷率</div>
            </div>
          </div>
          <div class="load-stats">
            <div class="load-stat">
              <div class="value">{{ stats.currentPower.toLocaleString() }}</div>
              <div class="label">当前功率 (kW)</div>
            </div>
            <div class="load-stat">
              <div class="value">{{ stats.totalCapacity.toLocaleString() }}</div>
              <div class="label">总容量 (kW)</div>
            </div>
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

      <!-- 实时预警 -->
      <div class="card">
        <div class="card-header">
          <span>实时预警</span>
          <router-link to="/dispatcher/alert" class="card-link">查看全部 →</router-link>
        </div>
        <div class="card-body">
          <div v-for="alert in alerts" :key="alert.id" :class="['notice-item', alert.level]">
            <i :class="['bi', alert.level === 'error' ? 'bi-exclamation-circle-fill' : 'bi-exclamation-triangle-fill']"></i>
            <div class="notice-content">
              <h6>{{ alert.title }}</h6>
              <p>{{ alert.description }}</p>
              <div class="notice-actions">
                <button class="btn btn-primary btn-sm" @click="createOrder(alert)">创建工单</button>
                <button class="btn btn-outline-secondary btn-sm" @click="ignoreAlert(alert.id)">
                  {{ alert.level === 'error' ? '查看详情' : '忽略' }}
                </button>
              </div>
            </div>
          </div>
          <div v-if="alerts.length === 0" class="empty-state">
            <i class="bi bi-check-circle"></i>
            <span>暂无预警信息</span>
          </div>
        </div>
      </div>

      <!-- 车间用电分配 -->
      <div class="card">
        <div class="card-header">
          <span>各车间用电分配</span>
          <router-link to="/dispatcher/dispatch" class="card-link">调度管理 →</router-link>
        </div>
        <div class="card-body">
          <div v-for="workshop in workshopStats" :key="workshop.id" class="workshop-item">
            <div class="workshop-header">
              <span class="workshop-name">{{ workshop.name }}</span>
              <div class="workshop-status">
                <span :class="['badge', workshop.statusClass]">{{ workshop.statusText }}</span>
                <span class="workshop-power">{{ workshop.current }}/{{ workshop.limit }} kW</span>
              </div>
            </div>
            <div class="progress-bar-wrapper">
              <div class="progress-bar" :class="workshop.barClass" :style="{ width: Math.min(workshop.percent, 100) + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingApplications, getApplications, approveApplication, rejectApplication } from '@/api/application'
import { getAlerts } from '@/api/energy'

const router = useRouter()

// 统计数据
const stats = ref({
  pendingApproval: 0,
  pendingAlerts: 0,
  loadRate: 0,
  todayApproved: 0,
  currentPower: 0,
  totalCapacity: 3120
})

// 待审批申请
const pendingApplications = ref([])

// 预警信息 - 根据车间用电情况动态生成
const alerts = ref([])

// 车间用电统计
const workshopStats = ref([])

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

// 加载待审批申请
async function loadPendingApplications() {
  try {
    const res = await getPendingApplications({ page: 1, size: 100 })
    if (res && res.code === 200 && res.data) {
      const data = res.data.records || res.data || []
      pendingApplications.value = data.slice(0, 3)
      stats.value.pendingApproval = data.length
    }
  } catch (e) {
    console.warn('加载待审批申请失败', e)
    pendingApplications.value = []
  }
}

// 加载已审批统计
async function loadApprovedStats() {
  try {
    const res = await getApplications({ page: 1, size: 100, status: 'APPROVED' })
    if (res && res.code === 200 && res.data) {
      const data = res.data.records || res.data || []
      // 统计今日已审批数量
      const today = new Date().toISOString().split('T')[0]
      const todayApproved = data.filter(a => {
        const approvedDate = a.approvedAt ? a.approvedAt.split('T')[0] : ''
        return approvedDate === today
      })
      stats.value.todayApproved = todayApproved.length
      
      // 计算当前功率和负荷率
      const now = new Date()
      const currentHour = now.getHours()
      let totalPower = 0
      
      data.forEach(app => {
        const startH = parseInt((app.startTime || '00:00').split(':')[0])
        const endH = parseInt((app.endTime || '00:00').split(':')[0])
        if (currentHour >= startH && currentHour < endH) {
          totalPower += parseFloat(app.power) || 0
        }
      })
      
      stats.value.currentPower = Math.round(totalPower)
      stats.value.loadRate = Math.round((totalPower / stats.value.totalCapacity) * 100 * 10) / 10
      
      // 生成车间统计数据
      generateWorkshopStats(data)
      
      // 生成预警信息
      generateAlerts()
    }
  } catch (e) {
    console.warn('加载审批统计失败', e)
  }
}

// 生成车间统计数据
function generateWorkshopStats(applications) {
  // 模拟车间数据，实际应从后端获取
  const workshops = [
    { id: 1, name: '生产一车间', limit: 800 },
    { id: 2, name: '生产二车间', limit: 700 },
    { id: 3, name: '装配车间', limit: 600 },
    { id: 4, name: '仓储车间', limit: 500 }
  ]
  
  const now = new Date()
  const currentHour = now.getHours()
  
  workshopStats.value = workshops.map(ws => {
    // 计算该车间当前功率
    let current = 15 // 基础负载
    applications.forEach(app => {
      if (app.workshopId === ws.id) {
        const startH = parseInt((app.startTime || '00:00').split(':')[0])
        const endH = parseInt((app.endTime || '00:00').split(':')[0])
        if (currentHour >= startH && currentHour < endH) {
          current += parseFloat(app.power) || 0
        }
      }
    })
    
    // 添加随机波动
    current = Math.round(current * (0.9 + Math.random() * 0.2))
    
    const percent = Math.round((current / ws.limit) * 100)
    let statusText = '正常'
    let statusClass = 'bg-success'
    let barClass = 'bg-success'
    
    if (percent >= 100) {
      statusText = '超限'
      statusClass = 'bg-danger'
      barClass = 'bg-danger'
    } else if (percent >= 80) {
      statusText = '预警'
      statusClass = 'bg-warning text-dark'
      barClass = 'bg-warning'
    }
    
    return {
      ...ws,
      current,
      percent,
      statusText,
      statusClass,
      barClass
    }
  })
}

// 生成预警信息
async function generateAlerts() {
  try {
    const res = await getAlerts()
    if (res && res.code === 200 && res.data) {
      const alertData = res.data.alerts || []
      
      alerts.value = alertData.slice(0, 3).map(alert => ({
        id: alert.id,
        level: alert.level === 'critical' ? 'error' : 'warning',
        title: alert.title,
        description: alert.description,
        workshopId: alert.workshopId
      }))
      
      stats.value.pendingAlerts = alertData.length
    }
  } catch (e) {
    console.error('加载预警数据失败', e)
  }
}

// 批准申请
async function handleApprove(id) {
  try {
    await ElMessageBox.confirm('确认批准该用电申请？', '确认', { type: 'info' })
    const res = await approveApplication(id, { comment: '批准' })
    if (res.code === 200) {
      ElMessage.success('审批成功，已通知申请人')
      loadPendingApplications()
      loadApprovedStats()
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

// 创建工单
function createOrder(alert) {
  ElMessage.info('工单创建功能开发中')
}

// 忽略预警
function ignoreAlert(id) {
  alerts.value = alerts.value.filter(a => a.id !== id)
  stats.value.pendingAlerts = alerts.value.length
  ElMessage.success('已忽略该预警')
}

onMounted(() => {
  loadPendingApplications()
  loadApprovedStats()
})
</script>

<style lang="scss">
@use '@/styles/dispatcher.scss';
</style>
