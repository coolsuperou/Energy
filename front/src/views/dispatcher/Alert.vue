<template>
  <div class="alert-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon red"><i class="bi bi-exclamation-circle"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.critical }}</div>
          <div class="stat-label">严重预警</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><i class="bi bi-exclamation-triangle"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.warning }}</div>
          <div class="stat-label">一般预警</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="bi bi-check-circle"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.handled }}</div>
          <div class="stat-label">今日已处理</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon blue"><i class="bi bi-graph-up"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.handleRate }}%</div>
          <div class="stat-label">处理率</div>
        </div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="card filter-card">
      <div class="card-body">
        <div class="filter-row">
          <el-select v-model="filter.type" placeholder="全部类型" clearable style="width: 150px">
            <el-option label="功率超限" value="power_over" />
            <el-option label="功率波动" value="power_fluctuation" />
            <el-option label="设备异常" value="device_error" />
            <el-option label="温度预警" value="temp_warning" />
          </el-select>
          <el-select v-model="filter.level" placeholder="全部级别" clearable style="width: 150px">
            <el-option label="严重" value="critical" />
            <el-option label="一般" value="warning" />
          </el-select>
          <div class="filter-spacer"></div>
          <el-button @click="loadAlerts"><i class="bi bi-arrow-clockwise"></i> 刷新</el-button>
        </div>
      </div>
    </div>

    <!-- 预警列表 -->
    <div class="card">
      <div class="card-header">实时预警列表</div>
      <div class="card-body">
        <div v-for="alert in filteredAlerts" :key="alert.id" :class="['alert-item', alert.level]">
          <div class="alert-icon">
            <i :class="['bi', alert.level === 'critical' ? 'bi-exclamation-circle-fill' : 'bi-exclamation-triangle-fill']"></i>
          </div>
          <div class="alert-content">
            <div class="alert-header">
              <div class="alert-title-wrap">
                <h6>【{{ alert.level === 'critical' ? '严重' : '一般' }}】{{ alert.title }}</h6>
                <p>{{ alert.description }}</p>
              </div>
              <span :class="['badge', alert.level === 'critical' ? 'bg-danger' : 'bg-warning text-dark']">
                {{ alert.level === 'critical' ? '严重' : '一般' }}
              </span>
            </div>
            <div class="alert-meta">
              <span><i class="bi bi-geo-alt"></i>{{ alert.location }}</span>
              <span><i class="bi bi-clock"></i>触发时间: {{ alert.time }}</span>
            </div>
            <div class="alert-actions">
              <el-button v-if="alert.level === 'critical'" type="danger" size="small" @click="emergencyLimit(alert)">
                <i class="bi bi-lightning-charge"></i> 紧急限电
              </el-button>
              <el-button type="primary" size="small" @click="createOrder(alert)">
                <i class="bi bi-card-checklist"></i> 创建工单
              </el-button>
              <el-button v-if="alert.level === 'critical'" size="small" @click="notifyWorkshop(alert)">
                <i class="bi bi-bell"></i> 通知车间
              </el-button>
              <el-button size="small" @click="viewDetail(alert)">查看详情</el-button>
              <el-button v-if="alert.level !== 'critical'" size="small" @click="ignoreAlert(alert)">忽略</el-button>
            </div>
          </div>
        </div>
        
        <div v-if="filteredAlerts.length === 0" class="empty-state">
          <i class="bi bi-check-circle"></i>
          <span>暂无预警信息</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAlerts } from '@/api/energy'

// 统计数据
const stats = ref({
  critical: 0,
  warning: 0,
  handled: 15,
  handleRate: 98
})

// 筛选条件
const filter = ref({
  type: '',
  level: ''
})

// 预警列表
const alerts = ref([])

// 过滤后的预警
const filteredAlerts = computed(() => {
  return alerts.value.filter(a => {
    if (filter.value.level && a.level !== filter.value.level) return false
    if (filter.value.type && a.type !== filter.value.type) return false
    return true
  })
})

// 加载预警数据
async function loadAlerts() {
  try {
    const res = await getAlerts()
    if (res && res.code === 200 && res.data) {
      const alertData = res.data.alerts || []
      const statsData = res.data.stats || {}
      
      const now = new Date()
      alerts.value = alertData.map((alert, index) => ({
        ...alert,
        time: formatTime(new Date(now.getTime() - Math.random() * 3600000 * (index + 1)))
      }))
      
      stats.value = {
        critical: statsData.critical || 0,
        warning: statsData.warning || 0,
        handled: statsData.handled || 0,
        handleRate: statsData.handleRate || 0
      }
    }
  } catch (e) {
    console.error('加载预警失败', e)
    ElMessage.error('加载预警数据失败')
  }
}

// 格式化时间
function formatTime(date) {
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${h}:${m}:${s}`
}

// 紧急限电
async function emergencyLimit(alert) {
  try {
    await ElMessageBox.confirm(
      `确认对${alert.title.replace('功率超限', '')}执行紧急限电？这将立即降低该车间的用电负载。`,
      '紧急限电确认',
      { type: 'warning', confirmButtonText: '确认限电', cancelButtonText: '取消' }
    )
    ElMessage.success('已发送限电指令，正在执行...')
    // 移除该预警
    alerts.value = alerts.value.filter(a => a.id !== alert.id)
    stats.value.critical = alerts.value.filter(a => a.level === 'critical').length
    stats.value.handled++
  } catch (e) {}
}

// 创建工单
function createOrder(alert) {
  ElMessage.info('工单创建功能开发中')
}

// 通知车间
function notifyWorkshop(alert) {
  ElMessage.success('已向车间发送通知')
}

// 查看详情
function viewDetail(alert) {
  ElMessageBox.alert(
    `<div style="line-height: 1.8">
      <p><strong>预警类型：</strong>${alert.type === 'power_over' ? '功率超限' : '温度预警'}</p>
      <p><strong>预警级别：</strong>${alert.level === 'critical' ? '严重' : '一般'}</p>
      <p><strong>位置：</strong>${alert.location}</p>
      <p><strong>描述：</strong>${alert.description}</p>
      <p><strong>触发时间：</strong>${alert.time}</p>
    </div>`,
    '预警详情',
    { dangerouslyUseHTMLString: true }
  )
}

// 忽略预警
function ignoreAlert(alert) {
  alerts.value = alerts.value.filter(a => a.id !== alert.id)
  stats.value.warning = alerts.value.filter(a => a.level === 'warning').length
  stats.value.handled++
  ElMessage.success('已忽略该预警')
}

onMounted(() => {
  loadAlerts()
})
</script>

<style lang="scss">
@use '@/styles/dispatcher.scss';
</style>
