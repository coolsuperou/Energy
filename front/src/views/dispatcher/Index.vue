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
          <div class="stat-value">{{ stats.todayDispatched }}</div>
          <div class="stat-label">今日已调度</div>
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
              <div class="value">{{ stats.currentPower }}</div>
              <div class="label">当前功率 (kW)</div>
            </div>
            <div class="load-stat">
              <div class="value">{{ stats.totalCapacity }}</div>
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
               :class="['order-card', { urgent: item.urgency === 'CRITICAL' }]">
            <div class="order-header">
              <div class="order-info">
                <h6>{{ item.workshopName || '车间' }} - {{ item.purpose }}</h6>
                <div class="order-meta">
                  <span><i class="bi bi-calendar"></i>{{ item.applyDate }} {{ item.startTime }}-{{ item.endTime }}</span>
                  <span><i class="bi bi-lightning-charge"></i>{{ item.power }} kW</span>
                </div>
              </div>
              <span :class="['badge', getUrgencyClass(item.urgency)]">{{ getUrgencyText(item.urgency) }}</span>
            </div>
            <div class="order-actions">
              <button class="btn btn-success btn-sm" @click="handleApprove(item.id)">
                <i class="bi bi-check-lg"></i>批准
              </button>
              <button class="btn btn-outline-warning btn-sm">调整</button>
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
          <a href="#" class="card-link">查看全部 →</a>
        </div>
        <div class="card-body">
          <div class="notice-item error">
            <i class="bi bi-exclamation-circle-fill"></i>
            <div class="notice-content">
              <h6>生产二车间功率超限</h6>
              <p>当前功率 185kW，超出限额 150kW</p>
              <div class="notice-actions">
                <button class="btn btn-primary btn-sm">创建工单</button>
                <button class="btn btn-outline-secondary btn-sm">查看详情</button>
              </div>
            </div>
          </div>
          <div class="notice-item warning">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <div class="notice-content">
              <h6>仓储车间用电异常波动</h6>
              <p>功率波动幅度超过20%，建议检查</p>
              <div class="notice-actions">
                <button class="btn btn-primary btn-sm">创建工单</button>
                <button class="btn btn-outline-secondary btn-sm">忽略</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 车间用电分配 -->
      <div class="card">
        <div class="card-header">各车间用电分配</div>
        <div class="card-body">
          <div v-for="workshop in workshopStats" :key="workshop.name" class="workshop-item">
            <div class="workshop-header">
              <span class="workshop-name">{{ workshop.name }}</span>
              <div class="workshop-status">
                <span :class="['badge', workshop.statusClass]">{{ workshop.statusText }}</span>
                <span class="workshop-power">{{ workshop.current }}/{{ workshop.limit }} kW</span>
              </div>
            </div>
            <div class="progress-bar-wrapper">
              <div class="progress-bar" :class="workshop.barClass" :style="{ width: workshop.percent + '%' }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getPendingApplications, approveApplication, rejectApplication } from '@/api/application'

const stats = ref({
  pendingApproval: 8,
  pendingAlerts: 3,
  loadRate: 78.5,
  todayDispatched: 156,
  currentPower: 2450,
  totalCapacity: 3120
})

const pendingApplications = ref([])

const workshopStats = ref([
  { name: '生产一车间', current: 98, limit: 150, statusText: '正常', statusClass: 'bg-success', barClass: 'bg-success', percent: 65 },
  { name: '生产二车间', current: 185, limit: 150, statusText: '超限', statusClass: 'bg-danger', barClass: 'bg-danger', percent: 100 },
  { name: '装配车间', current: 76, limit: 120, statusText: '正常', statusClass: 'bg-success', barClass: 'bg-success', percent: 63 },
  { name: '仓储车间', current: 84, limit: 100, statusText: '预警', statusClass: 'bg-warning text-dark', barClass: 'bg-warning', percent: 84 }
])

function getUrgencyClass(urgency) {
  const map = { 'CRITICAL': 'bg-danger', 'URGENT': 'bg-warning text-dark', 'NORMAL': 'bg-secondary' }
  return map[urgency] || 'bg-secondary'
}

function getUrgencyText(urgency) {
  const map = { 'CRITICAL': '紧急', 'URGENT': '加急', 'NORMAL': '普通' }
  return map[urgency] || '待审批'
}

async function loadPendingApplications() {
  try {
    const res = await getPendingApplications()
    if (res && res.code === 200 && res.data) {
      const data = res.data.records || res.data || []
      pendingApplications.value = data.slice(0, 3)
      stats.value.pendingApproval = data.length || 0
    }
  } catch (e) {
    console.warn('加载待审批申请失败，使用空数据', e)
    pendingApplications.value = []
  }
}

async function handleApprove(id) {
  try {
    const res = await approveApplication(id, { comment: '批准' })
    if (res.code === 200) {
      ElMessage.success('审批成功')
      loadPendingApplications()
    }
  } catch (e) {
    // 错误已在拦截器处理
  }
}

async function handleReject(id) {
  try {
    const res = await rejectApplication(id, { comment: '拒绝' })
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      loadPendingApplications()
    }
  } catch (e) {
    // 错误已在拦截器处理
  }
}

onMounted(() => {
  loadPendingApplications()
})
</script>

<style lang="scss">
@import '@/styles/dispatcher.scss';
</style>
