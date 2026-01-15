<template>
  <div class="workshop-home">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><i class="bi bi-lightning-charge"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayEnergy }}</div>
          <div class="stat-label">今日能耗 (kWh)</div>
          <div class="stat-trend up"><i class="bi bi-arrow-up"></i> 较昨日 +5.2%</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="bi bi-speedometer2"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.currentPower }}</div>
          <div class="stat-label">当前功率 (kW)</div>
          <div class="stat-trend muted">实时监测中</div>
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
        <div class="stat-icon red"><i class="bi bi-exclamation-triangle"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.alerts }}</div>
          <div class="stat-label">异常通知</div>
          <div class="stat-trend down" v-if="stats.alerts > 0">需要关注</div>
          <div class="stat-trend muted" v-else>暂无异常</div>
        </div>
      </div>
    </div>

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
            <router-link to="/workshop/message" class="quick-action">
              <div class="action-icon orange"><i class="bi bi-tools"></i></div>
              <span>故障报修</span>
            </router-link>
            <router-link to="/workshop/message" class="quick-action">
              <div class="action-icon purple"><i class="bi bi-lightbulb"></i></div>
              <span>节能建议</span>
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
        <div class="card-header">异常通知</div>
        <div class="card-body">
          <div class="notice-item warning" v-if="stats.alerts > 0">
            <i class="bi bi-exclamation-triangle-fill"></i>
            <div class="notice-content">
              <h6>用电超限预警</h6>
              <p>当前功率已达到申请上限的90%，请注意控制</p>
              <small>10分钟前</small>
            </div>
          </div>
          <div class="notice-item info">
            <i class="bi bi-info-circle-fill"></i>
            <div class="notice-content">
              <h6>系统维护通知</h6>
              <p>系统将于今晚22:00-23:00进行维护升级</p>
              <small>2小时前</small>
            </div>
          </div>
          <div v-if="stats.alerts === 0" class="empty-notice">
            <i class="bi bi-check-circle"></i>
            <span>暂无异常通知</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getMyApplications } from '@/api/application'

const stats = ref({
  todayEnergy: '1,256',
  currentPower: '89.5',
  pendingApply: 3,
  pendingApproval: 2,
  alerts: 1
})

const currentTime = ref('')
const recentApplications = ref([])
const powerChartRef = ref(null)
let powerChart = null
const approvedApplies = ref([])

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

// 根据已批准申请生成功率数据
function generatePowerData() {
  const data = []
  const now = new Date()
  const currentHour = now.getHours()
  
  for (let h = 0; h < 24; h++) {
    let basePower = 15 // 基础负载
    
    // 叠加已批准申请的功率
    approvedApplies.value.forEach(apply => {
      if (h >= apply.start && h < apply.end) {
        basePower += apply.power * (0.85 + Math.random() * 0.2)
      }
    })
    
    // 未来时段使用预估值
    if (h > currentHour) {
      basePower = h >= 8 && h < 18 ? 60 + Math.random() * 20 : 15
    }
    
    data.push(Math.round(basePower * 10) / 10)
  }
  return data
}

// 渲染功率曲线图
function renderPowerChart() {
  if (!powerChart) return
  
  const powerData = generatePowerData()
  const currentHour = new Date().getHours()
  const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
  
  // 更新当前功率统计
  stats.value.currentPower = powerData[currentHour].toFixed(1)
  
  // 计算今日能耗
  const totalEnergy = powerData.slice(0, currentHour + 1).reduce((sum, p) => sum + p, 0)
  stats.value.todayEnergy = Math.round(totalEnergy).toLocaleString()
  
  powerChart.setOption({
    tooltip: { 
      trigger: 'axis', 
      formatter: '{b}<br/>功率: {c} kW' 
    },
    grid: { 
      left: '3%', 
      right: '4%', 
      bottom: '3%', 
      top: '10%', 
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
        data: powerData.slice(0, currentHour + 1),
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
        data: powerData.map((v, i) => i > currentHour ? v : null),
        lineStyle: { color: '#94a3b8', type: 'dashed', width: 2 },
        symbol: 'none'
      }
    ]
  })
}

async function loadData() {
  try {
    const res = await getMyApplications({ page: 1, size: 100 })
    if (res && res.code === 200 && res.data) {
      const list = res.data.records || res.data || []
      recentApplications.value = list.slice(0, 5)
      
      // 统计待处理申请数
      const pending = list.filter(a => a.status === 'PENDING')
      stats.value.pendingApply = pending.length
      stats.value.pendingApproval = pending.length
      
      // 提取已批准的申请用于功率曲线
      const approved = list.filter(a => (a.status || '').toUpperCase() === 'APPROVED')
      approvedApplies.value = approved.map(a => {
        const startH = parseInt((a.startTime || '08:00').split(':')[0])
        const endH = parseInt((a.endTime || '18:00').split(':')[0])
        return {
          id: a.applicationNo,
          equipment: a.equipmentName || '设备',
          power: a.power || 50,
          start: startH,
          end: endH
        }
      })
      
      // 渲染图表
      renderPowerChart()
    }
  } catch (e) {
    console.warn('加载申请列表失败', e)
    recentApplications.value = []
    renderPowerChart()
  }
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
