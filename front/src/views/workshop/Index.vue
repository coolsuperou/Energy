<template>
  <div class="workshop-home" v-loading="loading">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><i class="bi bi-lightning-charge"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayEnergy }}</div>
          <div class="stat-label">今日用电量 (kWh)</div>
          <div class="stat-trend muted">实时统计</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="bi bi-calendar-month"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.monthEnergy }}</div>
          <div class="stat-label">本月用电量 (kWh)</div>
          <div class="stat-trend muted">累计统计</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><i class="bi bi-file-earmark-text"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.pendingApply }}</div>
          <div class="stat-label">待处理申请</div>
          <div class="stat-trend muted">{{ stats.pendingApproval }}个待审批</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red"><i class="bi bi-bell"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.unreadCount }}</div>
          <div class="stat-label">未读消息</div>
          <div class="stat-trend down" v-if="stats.unreadCount > 0">需要关注</div>
          <div class="stat-trend muted" v-else>暂无新消息</div>
        </div>
      </div>
    </div>

    <!-- 中间部分：快捷操作 + 功率曲线 -->
    <div class="content-grid">
      <!-- 快捷操作 -->
      <div class="card">
        <div class="card-header">快捷操作</div>
        <div class="card-body">
          <div class="action-grid">
            <router-link to="/workshop/apply" class="quick-action">
              <div class="action-icon blue"><i class="bi bi-plus-circle"></i></div>
              <span>新建申请</span>
            </router-link>
            <router-link to="/workshop/energy" class="quick-action">
              <div class="action-icon green"><i class="bi bi-graph-up"></i></div>
              <span>能耗统计</span>
            </router-link>
            <router-link to="/workshop/feedback" class="quick-action">
              <div class="action-icon orange"><i class="bi bi-tools"></i></div>
              <span>故障报修</span>
            </router-link>
          </div>
        </div>
      </div>

      <!-- 功率曲线 -->
      <div class="card">
        <div class="card-header">
          <span>今日功率曲线</span>
          <small class="text-muted">更新于 {{ currentTime }}</small>
        </div>
        <div class="card-body">
          <div ref="powerChartRef" style="height: 200px;"></div>
        </div>
      </div>
    </div>

    <!-- 下半部分：最近申请 + 消息提醒，3:2 比例 -->
    <div class="bottom-grid">
      <!-- 最近申请 -->
      <div class="card">
        <div class="card-header">
          <span>最近申请</span>
          <router-link to="/workshop/apply" class="card-link">查看全部 →</router-link>
        </div>
        <div class="card-body p-0">
          <table class="data-table">
            <thead>
              <tr>
                <th>申请编号</th>
                <th>申请时段</th>
                <th>预计功率</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in recentApplications" :key="item.id">
                <td class="fw-medium">{{ item.applicationNo }}</td>
                <td>{{ item.applyDate }} {{ item.startTime }}-{{ item.endTime }}</td>
                <td>{{ item.power }} kW</td>
                <td>
                  <span :class="['badge-status', getStatusClass(item.status)]">
                    {{ getStatusText(item.status) }}
                  </span>
                </td>
              </tr>
              <tr v-if="recentApplications.length === 0">
                <td colspan="4" class="text-center text-muted py-4">暂无申请记录</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 异常通知 -->
      <div class="card">
        <div class="card-header">
          <span>消息提醒</span>
          <router-link to="/workshop/message" class="card-link">查看全部 →</router-link>
        </div>
        <div class="card-body">
          <div class="notice-item warning" v-if="stats.unreadCount > 0">
            <i class="bi bi-bell-fill"></i>
            <div class="notice-content">
              <h6>您有 {{ stats.unreadCount }} 条未读消息</h6>
              <p>请及时查看消息中心了解最新动态</p>
            </div>
          </div>
          <div v-else class="empty-notice">
            <i class="bi bi-check-circle"></i>
            <span>暂无未读消息</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getDashboard } from '@/api/workshop'
import { getMyApplications } from '@/api/application'

const loading = ref(false)
const stats = ref({
  todayEnergy: '0',
  monthEnergy: '0',
  pendingApply: 0,
  pendingApproval: 0,
  unreadCount: 0
})

const currentTime = ref('')
const recentApplications = ref([])
const powerChartRef = ref(null)
let powerChart = null
const todayHourlyData = ref([])

function getStatusClass(status) {
  const map = {
    'PENDING': 'badge-pending',
    'APPROVED': 'badge-approved',
    'REJECTED': 'badge-rejected',
    'COMPLETED': 'badge-approved'
  }
  return map[status] || 'badge-pending'
}

function getStatusText(status) {
  const map = {
    'PENDING': '待审批',
    'APPROVED': '已批准',
    'REJECTED': '已拒绝',
    'COMPLETED': '已完成'
  }
  return map[status] || status
}

// 渲染功率曲线图（使用后端真实数据）
function renderPowerChart() {
  if (!powerChart) return
  
  const currentHour = new Date().getHours()
  const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
  
  // 按小时聚合功率数据
  const hourlyPower = new Array(24).fill(null)
  todayHourlyData.value.forEach(item => {
    const hour = item.recordHour
    if (hour >= 0 && hour < 24) {
      hourlyPower[hour] = (hourlyPower[hour] || 0) + parseFloat(item.power || 0)
    }
  })
  
  // 实际功率：只到当前小时
  const actualData = hourlyPower.map((v, i) => i <= currentHour ? (v || 0) : null)
  // 预测功率：当前小时之后，简单用最近有数据的小时值做预估
  const lastKnownPower = actualData.filter(v => v !== null).pop() || 0
  const predictData = hourlyPower.map((v, i) => i > currentHour ? (lastKnownPower * (0.8 + Math.random() * 0.2)) : null)
  
  powerChart.setOption({
    tooltip: { 
      trigger: 'axis',
      formatter: function(params) {
        let result = params[0].axisValue + '<br/>'
        params.forEach(p => {
          if (p.value != null) {
            result += p.marker + ' ' + p.seriesName + ': ' + Math.round(p.value * 10) / 10 + ' kW<br/>'
          }
        })
        return result
      }
    },
    legend: {
      data: [
        { name: '实际功率' },
        { name: '预测功率', itemStyle: { opacity: 0 }, lineStyle: { type: 'dashed', color: '#94a3b8' } }
      ],
      top: 0,
      right: 0,
      textStyle: { fontSize: 11 }
    },
    grid: { 
      left: '3%', 
      right: '4%', 
      bottom: '3%', 
      top: '30px', 
      containLabel: true 
    },
    xAxis: { 
      type: 'category', 
      data: hours, 
      axisLabel: { interval: 2, fontSize: 10 }
    },
    yAxis: { 
      type: 'value', 
      name: 'kW',
      nameTextStyle: { fontSize: 10 }
    },
    series: [
      {
        name: '实际功率',
        type: 'line',
        smooth: true,
        data: actualData,
        connectNulls: false,
        lineStyle: { color: '#3b82f6', width: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
          ])
        },
        symbol: 'circle',
        symbolSize: 4
      },
      {
        name: '预测功率',
        type: 'line',
        smooth: true,
        data: predictData,
        connectNulls: false,
        lineStyle: { color: '#94a3b8', type: 'dashed', width: 2 },
        symbol: 'none'
      }
    ]
  })
}

async function loadData() {
  loading.value = true
  try {
    // 调用后端 dashboard 接口获取概览数据
    const dashboardRes = await getDashboard()
    if (dashboardRes && dashboardRes.code === 200 && dashboardRes.data) {
      const data = dashboardRes.data
      
      // 今日用电量
      stats.value.todayEnergy = formatNumber(data.todayEnergy || 0)
      
      // 本月用电量
      stats.value.monthEnergy = formatNumber(data.monthEnergy || 0)
      
      // 未读消息数
      stats.value.unreadCount = data.unreadCount || 0
      
      // 最近申请状态（最近5条）
      const applications = data.recentApplications || []
      recentApplications.value = applications
      
      // 统计待处理申请数
      const pending = applications.filter(a => a.status === 'PENDING')
      stats.value.pendingApply = pending.length
      stats.value.pendingApproval = pending.length
      
      // 使用后端返回的今日能耗真实数据渲染功率曲线
      todayHourlyData.value = data.todayHourlyData || []
      
      // 渲染图表
      renderPowerChart()
    }
  } catch (e) {
    console.warn('加载首页数据失败', e)
    recentApplications.value = []
    renderPowerChart()
  } finally {
    loading.value = false
  }
}

// 格式化数字显示
function formatNumber(num) {
  if (num === null || num === undefined) return '0'
  const n = parseFloat(num)
  if (isNaN(n)) return '0'
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

onMounted(() => {
  const now = new Date()
  currentTime.value = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`
  
  // 初始化图表
  if (powerChartRef.value) {
    powerChart = echarts.init(powerChartRef.value)
  }
  
  loadData()
  
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (powerChart) {
    powerChart.dispose()
    powerChart = null
  }
})

function handleResize() {
  powerChart?.resize()
}
</script>

<style lang="scss">
@use '@/styles/workshop.scss';
</style>
