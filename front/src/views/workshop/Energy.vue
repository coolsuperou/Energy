<template>
  <div class="content-wrapper">
    <!-- 能耗概览 -->
    <div class="row g-4 mb-4">
      <div class="col-md-4">
        <div class="stat-card" style="border-top: 3px solid #3b82f6;">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon blue"><i class="bi bi-lightning-charge"></i></div>
            <div>
              <div class="stat-value">{{ stats.todayEnergy }} <small class="fs-6 text-muted">kWh</small></div>
              <div class="stat-label">今日能耗</div>
              <div class="stat-trend up"><i class="bi bi-arrow-up"></i> +5.2%</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="stat-card" style="border-top: 3px solid #22c55e;">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon green"><i class="bi bi-speedometer2"></i></div>
            <div>
              <div class="stat-value">{{ stats.currentPower }} <small class="fs-6 text-muted">kW</small></div>
              <div class="stat-label">当前功率</div>
              <div class="stat-trend text-muted">实时更新</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="stat-card" style="border-top: 3px solid #f59e0b;">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon orange"><i class="bi bi-currency-yen"></i></div>
            <div>
              <div class="stat-value">¥{{ stats.todayCost }}</div>
              <div class="stat-label">今日电费</div>
              <div class="stat-trend text-muted">峰平谷综合</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 已批准的用电申请 -->
    <div v-if="currentApply" class="alert alert-success d-flex align-items-center mb-4">
      <i class="bi bi-check-circle me-2"></i>
      <div class="flex-grow-1">
        <strong>当前生效的用电申请：</strong>
        <span>{{ currentApply.equipment }} ({{ currentApply.power }}kW) · {{ currentApply.startTime }}-{{ currentApply.endTime }} · 申请编号 {{ currentApply.id }}</span>
      </div>
      <span class="badge bg-success">执行中</span>
    </div>

    <div class="row g-4 mb-4">
      <!-- 实时功率曲线 -->
      <div class="col-lg-8">
        <div class="card h-100">
          <div class="card-header d-flex justify-content-between align-items-center">
            <span>实时功率监控</span>
            <div class="d-flex align-items-center gap-3">
              <span class="small text-muted">最大允许功率: <strong class="text-danger">200 kW</strong></span>
              <div class="btn-group btn-group-sm">
                <button :class="['btn', viewType === 'today' ? 'btn-primary' : 'btn-outline-primary']" @click="switchView('today')">今日</button>
                <button :class="['btn', viewType === 'week' ? 'btn-primary' : 'btn-outline-primary']" @click="switchView('week')">本周</button>
              </div>
            </div>
          </div>
          <div class="card-body">
            <div ref="powerChartRef" style="height: 300px;"></div>
          </div>
        </div>
      </div>

      <!-- 时段能耗分布 -->
      <div class="col-lg-4">
        <div class="card h-100">
          <div class="card-header">今日时段能耗</div>
          <div class="card-body">
            <div ref="pieChartRef" style="height: 200px;"></div>
            <div class="mt-3">
              <div class="d-flex justify-content-between mb-2">
                <span><i class="bi bi-circle-fill text-danger me-2"></i>峰时 (1.2元/kWh)</span>
                <span>{{ energyData.peak }} kWh</span>
              </div>
              <div class="d-flex justify-content-between mb-2">
                <span><i class="bi bi-circle-fill text-warning me-2"></i>平时 (0.8元/kWh)</span>
                <span>{{ energyData.normal }} kWh</span>
              </div>
              <div class="d-flex justify-content-between">
                <span><i class="bi bi-circle-fill text-success me-2"></i>谷时 (0.4元/kWh)</span>
                <span>{{ energyData.valley }} kWh</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 能耗明细 -->
    <div class="card">
      <div class="card-header d-flex justify-content-between align-items-center">
        <span>能耗明细记录</span>
        <button class="btn btn-outline-primary btn-sm"><i class="bi bi-download me-1"></i>导出</button>
      </div>
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead>
            <tr>
              <th>时间</th>
              <th>关联申请</th>
              <th>设备</th>
              <th>平均功率</th>
              <th>电量</th>
              <th>电费</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in energyDetails" :key="index" :class="{ 'text-muted': item.isBase }">
              <td>{{ item.time }}</td>
              <td>
                <a v-if="item.applicationNo" href="#" class="text-primary">{{ item.applicationNo }}</a>
                <span v-else>-</span>
              </td>
              <td>{{ item.equipment }}</td>
              <td>{{ item.avgPower }} kW</td>
              <td class="fw-semibold">{{ item.energy }} kWh</td>
              <td class="text-success">¥{{ item.cost }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getMyApplications } from '@/api/application'

const powerChartRef = ref(null)
const pieChartRef = ref(null)
let powerChart = null
let pieChart = null

const viewType = ref('today')
const currentApply = ref(null)

// 模拟已批准的用电申请
const approvedApplies = ref([
  { id: 'AP202401160001', equipment: '生产线A', power: 80, start: 8, end: 12, type: 'peak' },
  { id: 'AP202401160002', equipment: '数控机床1号', power: 45, start: 14, end: 18, type: 'normal' }
])

const stats = reactive({
  todayEnergy: 0,
  currentPower: 0,
  todayCost: 0
})

const energyData = reactive({
  peak: 0,
  normal: 0,
  valley: 0
})

const energyDetails = ref([])

// 生成功率数据
function generatePowerData() {
  const data = []
  const now = new Date()
  const currentHour = now.getHours()
  
  for (let h = 0; h < 24; h++) {
    let basePower = 15 // 基础负载
    
    approvedApplies.value.forEach(apply => {
      if (h >= apply.start && h < apply.end) {
        basePower += apply.power * (0.85 + Math.random() * 0.2)
      }
    })
    
    if (h > currentHour) {
      basePower = h >= 8 && h < 18 ? 60 + Math.random() * 20 : 15
    }
    
    data.push(Math.round(basePower * 10) / 10)
  }
  return data
}

// 计算电量
function calculateEnergy(powerData) {
  let peak = 0, normal = 0, valley = 0
  
  powerData.forEach((power, hour) => {
    if ((hour >= 8 && hour < 11) || (hour >= 18 && hour < 21)) {
      peak += power
    } else if (hour >= 23 || hour < 7) {
      valley += power
    } else {
      normal += power
    }
  })
  
  return { peak: Math.round(peak), normal: Math.round(normal), valley: Math.round(valley) }
}

// 计算电费
function calculateCost(energy) {
  return energy.peak * 1.2 + energy.normal * 0.8 + energy.valley * 0.4
}

// 生成能耗明细 - 显示所有已批准的申请
function generateTable() {
  const details = []
  const currentHour = new Date().getHours()
  
  // 显示所有已批准的申请
  approvedApplies.value.forEach(apply => {
    const startH = apply.start
    const endH = apply.end
    const hours = endH - startH
    const avgPower = apply.power * (0.9 + Math.random() * 0.1)
    const energy = Math.round(avgPower * hours)
    const price = apply.type === 'peak' ? 1.2 : (apply.type === 'valley' ? 0.4 : 0.8)
    const cost = Math.round(energy * price)
    
    // 格式化日期时间：MM-DD HH:mm-HH:mm
    const dateStr = apply.applyDate ? apply.applyDate.substring(5) : '' // 取 MM-DD
    const timeStr = `${apply.startTime || startH + ':00'}-${apply.endTime || endH + ':00'}`
    
    details.push({
      time: `${dateStr} ${timeStr}`,
      applicationNo: apply.id,
      equipment: apply.equipment,
      avgPower: avgPower.toFixed(1),
      energy: energy,
      cost: cost,
      isBase: false
    })
  })
  
  // 基础负载
  const today = new Date().toISOString().split('T')[0].substring(5) // MM-DD
  details.push({
    time: `${today} 00:00-${currentHour}:00`,
    applicationNo: null,
    equipment: '基础负载（照明/空调）',
    avgPower: '~15',
    energy: 15 * currentHour,
    cost: Math.round(15 * currentHour * 0.6),
    isBase: true
  })
  
  return details
}

function switchView(type) {
  viewType.value = type
}

// 渲染图表
function renderCharts() {
  const powerData = generatePowerData()
  const energy = calculateEnergy(powerData)
  const totalEnergy = energy.peak + energy.normal + energy.valley
  const cost = calculateCost(energy)
  const currentHour = new Date().getHours()

  // 更新统计
  stats.todayEnergy = totalEnergy
  stats.currentPower = powerData[currentHour]
  stats.todayCost = Math.round(cost)
  
  energyData.peak = energy.peak
  energyData.normal = energy.normal
  energyData.valley = energy.valley

  // 更新明细
  energyDetails.value = generateTable()

  // 更新当前生效申请
  approvedApplies.value.forEach(apply => {
    if (currentHour >= apply.start && currentHour < apply.end) {
      currentApply.value = {
        id: apply.id,
        equipment: apply.equipment,
        power: apply.power,
        startTime: `${apply.start.toString().padStart(2, '0')}:00`,
        endTime: `${apply.end.toString().padStart(2, '0')}:00`
      }
    }
  })

  // 功率曲线图
  const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
  
  if (powerChart) {
    powerChart.setOption({
      tooltip: { trigger: 'axis', formatter: '{b}<br/>功率: {c} kW' },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
      xAxis: { type: 'category', data: hours, axisLabel: { interval: 1 } },
      yAxis: { type: 'value', name: 'kW', max: 220 },
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
          markLine: {
            data: [{ yAxis: 200, name: '最大功率', lineStyle: { color: '#ef4444', type: 'dashed' } }],
            label: { formatter: '限额 200kW' }
          },
          markArea: {
            silent: true,
            data: [
              [{ xAxis: '8:00', itemStyle: { color: 'rgba(239,68,68,0.1)' } }, { xAxis: '11:00' }],
              [{ xAxis: '18:00', itemStyle: { color: 'rgba(239,68,68,0.1)' } }, { xAxis: '21:00' }]
            ]
          }
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

  // 饼图
  if (pieChart) {
    pieChart.setOption({
      tooltip: { formatter: '{b}: {c} kWh ({d}%)' },
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        data: [
          { value: energy.peak, name: '峰时', itemStyle: { color: '#ef4444' } },
          { value: energy.normal, name: '平时', itemStyle: { color: '#f59e0b' } },
          { value: energy.valley, name: '谷时', itemStyle: { color: '#22c55e' } }
        ],
        label: { show: false }
      }]
    })
  }
}

// 加载真实申请数据
async function loadApprovedApplications() {
  try {
    const res = await getMyApplications({ page: 1, size: 100 })
    if (res.code === 200) {
      const list = res.data.records || res.data || []
      const approved = list.filter(a => (a.status || '').toLowerCase() === 'approved')
      
      if (approved.length > 0) {
        approvedApplies.value = approved.map(a => {
          const startH = parseInt(a.startTime.split(':')[0])
          const endH = parseInt(a.endTime.split(':')[0])
          // 判断时段类型
          let type = 'normal'
          if ((startH >= 8 && startH < 11) || (startH >= 18 && startH < 21)) {
            type = 'peak'
          } else if (startH >= 23 || startH < 7) {
            type = 'valley'
          }
          
          return {
            id: a.applicationNo,
            equipment: a.equipmentName || '设备',
            power: a.power,
            start: startH,
            end: endH,
            type: type,
            applyDate: a.applyDate,
            startTime: a.startTime,
            endTime: a.endTime
          }
        })
      }
    }
  } catch (e) {
    console.error('加载申请失败', e)
  }
  renderCharts()
}

let refreshTimer = null

onMounted(() => {
  powerChart = echarts.init(powerChartRef.value)
  pieChart = echarts.init(pieChartRef.value)
  
  loadApprovedApplications()
  
  refreshTimer = setInterval(renderCharts, 30000)
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('resize', handleResize)
  if (powerChart) powerChart.dispose()
  if (pieChart) pieChart.dispose()
})

function handleResize() {
  powerChart?.resize()
  pieChart?.resize()
}
</script>

<style lang="scss">
@import '@/styles/workshop.scss';
</style>
