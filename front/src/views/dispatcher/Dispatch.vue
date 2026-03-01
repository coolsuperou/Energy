<template>
  <div class="dispatch-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><i class="bi bi-lightning-charge"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.currentPower.toLocaleString() }} <small>kW</small></div>
          <div class="stat-label">当前总功率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="bi bi-bar-chart"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalCapacity.toLocaleString() }} <small>kW</small></div>
          <div class="stat-label">总容量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><i class="bi bi-speedometer2"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.loadRate }}%</div>
          <div class="stat-label">负荷率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><i class="bi bi-plug"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.available.toLocaleString() }} <small>kW</small></div>
          <div class="stat-label">可用余量</div>
        </div>
      </div>
    </div>

    <!-- 各车间用电分配 -->
    <div class="card">
      <div class="card-header">
        <span>各车间用电分配</span>
        <el-button size="small" @click="showSettings"><i class="bi bi-gear"></i> 调度设置</el-button>
      </div>
      <div class="card-body">
        <div class="workshop-grid">
          <div v-for="ws in workshops" :key="ws.id" 
               :class="['workshop-card', { 'workshop-danger': ws.percent >= 100, 'workshop-warning': ws.percent >= 80 && ws.percent < 100 }]">
            <div class="workshop-header">
              <div class="workshop-title">
                <i :class="['bi', ws.icon]"></i>
                <span>{{ ws.name }}</span>
              </div>
              <span :class="['badge', ws.statusClass]">{{ ws.statusText }}</span>
            </div>
            <div class="workshop-stats">
              <div class="ws-stat">
                <div class="ws-value" :class="ws.valueClass">{{ ws.current }}</div>
                <div class="ws-label">当前功率(kW)</div>
              </div>
              <div class="ws-stat">
                <div class="ws-value">{{ ws.limit.toLocaleString() }}</div>
                <div class="ws-label">分配配额(kWh)</div>
              </div>
              <div class="ws-stat">
                <div class="ws-value" :class="ws.valueClass">{{ ws.percent }}%</div>
                <div class="ws-label">使用率</div>
              </div>
            </div>
            <div class="workshop-progress">
              <div class="progress-fill" :class="ws.barClass" :style="{ width: Math.min(ws.percent, 100) + '%' }"></div>
            </div>
            <div class="workshop-actions">
              <el-button v-if="ws.percent >= 100" type="warning" size="small" @click="sendLimitNotice(ws)">
                限电通知
              </el-button>
              <el-button size="small" @click="adjustLimit(ws)">调整限额</el-button>
              <el-button size="small" @click="viewDetail(ws)">查看详情</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 调整限额对话框 -->
    <el-dialog v-model="adjustDialog.visible" title="调整用电限额" width="400px">
      <el-form label-width="100px">
        <el-form-item label="车间名称">
          <span>{{ adjustDialog.workshop?.name }}</span>
        </el-form-item>
        <el-form-item label="当前配额">
          <span>{{ adjustDialog.workshop?.limit?.toLocaleString() }} kWh</span>
        </el-form-item>
        <el-form-item label="新配额">
          <el-input-number v-model="adjustDialog.newLimit" :min="1000" :max="200000" :step="5000" />
          <span style="margin-left: 8px">kWh</span>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="adjustDialog.reason" type="textarea" :rows="2" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApplications } from '@/api/application'
import { getWorkshops as fetchWorkshops } from '@/api/dispatcher'

// 统计数据
const stats = ref({
  currentPower: 0,
  totalCapacity: 3120,
  loadRate: 0,
  available: 0
})

// 车间列表
const workshops = ref([])

// 调整对话框
const adjustDialog = ref({
  visible: false,
  workshop: null,
  newLimit: 0,
  reason: ''
})

// 加载数据
async function loadData() {
  try {
    // 从后端获取真实车间和配额数据
    const wsRes = await fetchWorkshops()
    if (wsRes && wsRes.code === 200 && wsRes.data) {
      generateWorkshopData(wsRes.data)
    } else {
      generateMockData()
    }
  } catch (e) {
    console.warn('加载数据失败', e)
    generateMockData()
  }
}

const icons = ['bi-building', 'bi-building', 'bi-tools', 'bi-box-seam']

// 根据后端返回的车间配额数据生成展示数据
function generateWorkshopData(workshopData) {
  let totalPower = 0
  
  workshops.value = workshopData.map((ws, index) => {
    const current = parseFloat(ws.currentPower) || 0
    const limit = parseFloat(ws.totalQuota) || 50000
    const used = parseFloat(ws.usedQuota) || 0
    const percent = limit > 0 ? Math.round((used / limit) * 100) : 0
    totalPower += current
    
    let statusText = '正常'
    let statusClass = 'bg-success'
    let barClass = 'bg-success'
    let valueClass = 'text-primary'
    
    if (percent >= 100) {
      statusText = '超限'
      statusClass = 'bg-danger'
      barClass = 'bg-danger'
      valueClass = 'text-danger'
    } else if (percent >= 80) {
      statusText = '预警'
      statusClass = 'bg-warning text-dark'
      barClass = 'bg-warning'
      valueClass = 'text-warning'
    }
    
    return {
      id: ws.workshopId,
      name: ws.name,
      icon: icons[index] || 'bi-building',
      current,
      limit: Math.round(limit),
      used: Math.round(used),
      percent,
      statusText,
      statusClass,
      barClass,
      valueClass
    }
  })
  
  // 更新总体统计
  stats.value.currentPower = totalPower
  stats.value.loadRate = Math.round((totalPower / stats.value.totalCapacity) * 100 * 10) / 10
  stats.value.available = stats.value.totalCapacity - totalPower
}

// 生成模拟数据
function generateMockData() {
  workshops.value = [
    { id: 1, name: '第一车间', limit: 50000, used: 12500, current: 520, percent: 25, icon: 'bi-building', statusText: '正常', statusClass: 'bg-success', barClass: 'bg-success', valueClass: 'text-primary' },
    { id: 2, name: '第二车间', limit: 45000, used: 10800, current: 850, percent: 24, icon: 'bi-building', statusText: '正常', statusClass: 'bg-success', barClass: 'bg-success', valueClass: 'text-primary' },
    { id: 3, name: '第三车间', limit: 60000, used: 18000, current: 380, percent: 30, icon: 'bi-tools', statusText: '正常', statusClass: 'bg-success', barClass: 'bg-success', valueClass: 'text-primary' },
    { id: 4, name: '第四车间', limit: 55000, used: 16500, current: 420, percent: 30, icon: 'bi-box-seam', statusText: '正常', statusClass: 'bg-success', barClass: 'bg-success', valueClass: 'text-primary' }
  ]
  
  const totalPower = workshops.value.reduce((sum, ws) => sum + ws.current, 0)
  stats.value.currentPower = totalPower
  stats.value.loadRate = Math.round((totalPower / stats.value.totalCapacity) * 100 * 10) / 10
  stats.value.available = stats.value.totalCapacity - totalPower
}

// 显示调度设置
function showSettings() {
  ElMessage.info('调度设置功能开发中')
}

// 发送限电通知
async function sendLimitNotice(ws) {
  try {
    await ElMessageBox.confirm(
      `确认向${ws.name}发送限电通知？当前功率 ${ws.current}kW 已超出限额 ${ws.limit}kW。`,
      '限电通知',
      { type: 'warning' }
    )
    ElMessage.success(`已向${ws.name}发送限电通知`)
  } catch (e) {}
}

// 调整限额
function adjustLimit(ws) {
  adjustDialog.value = {
    visible: true,
    workshop: ws,
    newLimit: ws.limit,
    reason: ''
  }
}

// 确认调整
function confirmAdjust() {
  const ws = adjustDialog.value.workshop
  if (!ws) return
  
  // 更新限额
  const idx = workshops.value.findIndex(w => w.id === ws.id)
  if (idx >= 0) {
    workshops.value[idx].limit = adjustDialog.value.newLimit
    // 重新计算百分比和状态
    const percent = Math.round((workshops.value[idx].current / adjustDialog.value.newLimit) * 100)
    workshops.value[idx].percent = percent
    
    if (percent >= 100) {
      workshops.value[idx].statusText = '超限'
      workshops.value[idx].statusClass = 'bg-danger'
      workshops.value[idx].barClass = 'bg-danger'
      workshops.value[idx].valueClass = 'text-danger'
    } else if (percent >= 80) {
      workshops.value[idx].statusText = '预警'
      workshops.value[idx].statusClass = 'bg-warning text-dark'
      workshops.value[idx].barClass = 'bg-warning'
      workshops.value[idx].valueClass = 'text-warning'
    } else {
      workshops.value[idx].statusText = '正常'
      workshops.value[idx].statusClass = 'bg-success'
      workshops.value[idx].barClass = 'bg-success'
      workshops.value[idx].valueClass = 'text-primary'
    }
  }
  
  adjustDialog.value.visible = false
  ElMessage.success(`${ws.name}配额已调整为 ${adjustDialog.value.newLimit.toLocaleString()} kWh`)
}

// 查看详情
function viewDetail(ws) {
  ElMessageBox.alert(
    `<div style="line-height: 1.8">
      <p><strong>车间名称：</strong>${ws.name}</p>
      <p><strong>当前功率：</strong>${ws.current} kW</p>
      <p><strong>分配配额：</strong>${ws.limit?.toLocaleString()} kWh</p>
      <p><strong>已用配额：</strong>${ws.used?.toLocaleString()} kWh</p>
      <p><strong>使用率：</strong>${ws.percent}%</p>
      <p><strong>状态：</strong>${ws.statusText}</p>
    </div>`,
    '车间详情',
    { dangerouslyUseHTMLString: true }
  )
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss">
@use '@/styles/dispatcher.scss';
</style>
