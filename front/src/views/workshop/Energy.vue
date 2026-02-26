<template>
  <div class="content-wrapper">
    <!-- 日期范围选择器 -->
    <div class="card mb-4">
      <div class="card-body">
        <div class="row align-items-center">
          <div class="col-auto">
            <label class="form-label mb-0 me-2">查询日期：</label>
          </div>
          <div class="col-auto">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :shortcuts="dateShortcuts"
              @change="handleDateChange"
            />
          </div>
          <div class="col-auto">
            <button class="btn btn-primary" @click="loadEnergyData">
              <i class="bi bi-search me-1"></i>查询
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 能耗概览 -->
    <div class="row g-4 mb-4">
      <div class="col-md-4">
        <div class="stat-card h-100" style="border-top: 3px solid #3b82f6;">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon blue"><i class="bi bi-lightning-charge"></i></div>
            <div>
              <div class="stat-value">{{ stats.totalEnergy }} <small class="fs-6 text-muted">kWh</small></div>
              <div class="stat-label">{{ dateRangeLabel }}能耗</div>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-4">
        <div class="stat-card h-100" style="border-top: 3px solid #22c55e;">
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
        <div class="stat-card h-100" style="border-top: 3px solid #f59e0b;">
          <div class="d-flex align-items-center gap-3">
            <div class="stat-icon orange"><i class="bi bi-currency-yen"></i></div>
            <div>
              <div class="stat-value">¥{{ stats.totalCost }}</div>
              <div class="stat-label">{{ dateRangeLabel }}电费</div>
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
            <span>功率监控曲线</span>
            <div class="d-flex align-items-center gap-3">
              <span class="small text-muted">最大允许功率: <strong class="text-danger">200 kW</strong></span>
            </div>
          </div>
          <div class="card-body">
            <div v-show="loading" class="text-center py-5">
              <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">加载中...</span>
              </div>
            </div>
            <div v-show="!loading" ref="powerChartRef" style="height: 300px;"></div>
          </div>
        </div>
      </div>

      <!-- 时段能耗分布 -->
      <div class="col-lg-4">
        <div class="card h-100">
          <div class="card-header">时段能耗分布</div>
          <div class="card-body">
            <div v-show="loading" class="text-center py-5">
              <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">加载中...</span>
              </div>
            </div>
            <div v-show="!loading">
              <div ref="pieChartRef" style="height: 200px;"></div>
              <div class="mt-3">
                <div class="d-flex justify-content-between mb-2">
                  <span><i class="bi bi-circle-fill text-danger me-2"></i>峰时 (1.2元/kWh)</span>
                  <span>{{ costStats.peakEnergy }} kWh / ¥{{ costStats.peakCost }}</span>
                </div>
                <div class="d-flex justify-content-between mb-2">
                  <span><i class="bi bi-circle-fill text-warning me-2"></i>平时 (0.8元/kWh)</span>
                  <span>{{ costStats.normalEnergy }} kWh / ¥{{ costStats.normalCost }}</span>
                </div>
                <div class="d-flex justify-content-between">
                  <span><i class="bi bi-circle-fill text-success me-2"></i>谷时 (0.4元/kWh)</span>
                  <span>{{ costStats.valleyEnergy }} kWh / ¥{{ costStats.valleyCost }}</span>
                </div>
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
              <th>日期</th>
              <th>时间</th>
              <th>设备ID</th>
              <th>时段类型</th>
              <th>功率(kW)</th>
              <th>电量(kWh)</th>
              <th>电价(元/kWh)</th>
              <th>费用(元)</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="text-center py-4">
                <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
                <span class="ms-2">加载中...</span>
              </td>
            </tr>
            <tr v-else-if="energyDetails.length === 0">
              <td colspan="8" class="text-center py-4 text-muted">
                <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                暂无能耗数据
              </td>
            </tr>
            <tr v-for="item in energyDetails" :key="item.id">
              <td>{{ item.recordDate }}</td>
              <td>{{ item.recordHour }}:00</td>
              <td>{{ item.equipmentId }}</td>
              <td>
                <span :class="getPeriodBadgeClass(item.periodType)">{{ getPeriodLabel(item.periodType) }}</span>
              </td>
              <td>{{ item.power }}</td>
              <td class="fw-semibold">{{ item.energy }}</td>
              <td>{{ item.price }}</td>
              <td class="text-success">¥{{ item.cost }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>


<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getMyWorkshopEnergy } from '@/api/workshop'
import { getMyApplications } from '@/api/application'

const powerChartRef = ref(null)
const pieChartRef = ref(null)
let powerChart = null
let pieChart = null

const loading = ref(false)
const currentApply = ref(null)

// 日期范围
const dateRange = ref([])
const today = new Date().toISOString().split('T')[0]

// 日期快捷选项
const dateShortcuts = [
  { text: '今天', value: () => [today, today] },
  { text: '昨天', value: () => {
    const d = new Date()
    d.setDate(d.getDate() - 1)
    const yesterday = d.toISOString().split('T')[0]
    return [yesterday, yesterday]
  }},
  { text: '最近7天', value: () => {
    const end = new Date()
    const start = new Date()
    start.setDate(start.getDate() - 6)
    return [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
  }},
  { text: '本月', value: () => {
    const now = new Date()
    const start = new Date(now.getFullYear(), now.getMonth(), 1)
    return [start.toISOString().split('T')[0], now.toISOString().split('T')[0]]
  }}
]

// 统计数据
const stats = reactive({
  totalEnergy: 0,
  currentPower: 0,
  totalCost: 0
})

// 费用统计（来自后端）
const costStats = reactive({
  peakEnergy: 0, peakCost: 0,
  normalEnergy: 0, normalCost: 0,
  valleyEnergy: 0, valleyCost: 0,
  totalEnergy: 0, totalCost: 0
})

// 能耗明细列表
const energyDetails = ref([])

// 日期范围标签
const dateRangeLabel = computed(() => {
  if (!dateRange.value || dateRange.value.length !== 2) return '今日'
  const [start, end] = dateRange.value
  if (start === end && start === today) return '今日'
  if (start === end) return start
  return `${start} 至 ${end}`
})


// 时段类型标签
function getPeriodLabel(periodType) {
  const labels = { PEAK: '峰时', NORMAL: '平时', VALLEY: '谷时' }
  return labels[periodType] || periodType
}

// 时段类型样式
function getPeriodBadgeClass(periodType) {
  const classes = {
    PEAK: 'badge bg-danger',
    NORMAL: 'badge bg-warning text-dark',
    VALLEY: 'badge bg-success'
  }
  return classes[periodType] || 'badge bg-secondary'
}

// 日期变化处理
function handleDateChange(val) {
  if (val) {
    loadEnergyData()
  }
}

// 加载能耗数据
async function loadEnergyData() {
  loading.value = true
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    
    const res = await getMyWorkshopEnergy(params)
    if (res.code === 200) {
      const data = res.data
      
      // 更新能耗明细
      energyDetails.value = data.energyData || []
      
      // 更新费用统计
      const cs = data.costStats || {}
      costStats.peakEnergy = parseFloat(cs.peakEnergy || 0).toFixed(2)
      costStats.peakCost = parseFloat(cs.peakCost || 0).toFixed(2)
      costStats.normalEnergy = parseFloat(cs.normalEnergy || 0).toFixed(2)
      costStats.normalCost = parseFloat(cs.normalCost || 0).toFixed(2)
      costStats.valleyEnergy = parseFloat(cs.valleyEnergy || 0).toFixed(2)
      costStats.valleyCost = parseFloat(cs.valleyCost || 0).toFixed(2)
      costStats.totalEnergy = parseFloat(cs.totalEnergy || 0).toFixed(2)
      costStats.totalCost = parseFloat(cs.totalCost || 0).toFixed(2)
      
      // 更新统计卡片
      stats.totalEnergy = costStats.totalEnergy
      stats.totalCost = costStats.totalCost
      
      // 计算当前功率（取最新一条记录的功率）
      if (energyDetails.value.length > 0) {
        const latest = energyDetails.value[energyDetails.value.length - 1]
        stats.currentPower = parseFloat(latest.power || 0).toFixed(1)
      } else {
        stats.currentPower = 0
      }
    }
  } catch (e) {
    console.error('加载能耗数据失败', e)
  } finally {
    loading.value = false
    // 等 DOM 更新后再渲染图表（loading=false 后 v-if 才会显示图表容器）
    await nextTick()
    renderCharts()
  }
}


// 加载当前生效的申请
async function loadCurrentApply() {
  try {
    const res = await getMyApplications({ page: 1, size: 100 })
    if (res.code === 200) {
      const list = res.data.records || res.data || []
      const approved = list.filter(a => (a.status || '').toLowerCase() === 'approved')
      
      const currentHour = new Date().getHours()
      const todayStr = today
      
      // 查找当前时间正在执行的申请
      for (const a of approved) {
        if (a.applyDate === todayStr) {
          const startH = parseInt(a.startTime.split(':')[0])
          const endH = parseInt(a.endTime.split(':')[0])
          if (currentHour >= startH && currentHour < endH) {
            currentApply.value = {
              id: a.applicationNo,
              equipment: a.equipmentName || '设备',
              power: a.power,
              startTime: a.startTime,
              endTime: a.endTime
            }
            break
          }
        }
      }
    }
  } catch (e) {
    console.error('加载申请失败', e)
  }
}

// 渲染图表
function renderCharts() {
  renderPowerChart()
  renderPieChart()
}

// 渲染功率曲线图
function renderPowerChart() {
  if (!powerChartRef.value) return
  
  if (!powerChart) {
    powerChart = echarts.init(powerChartRef.value)
  }
  
  // 按小时聚合功率数据
  const hourlyPower = new Array(24).fill(0)
  const hourlyCount = new Array(24).fill(0)
  
  energyDetails.value.forEach(item => {
    const hour = item.recordHour
    if (hour >= 0 && hour < 24) {
      hourlyPower[hour] += parseFloat(item.power || 0)
      hourlyCount[hour]++
    }
  })
  
  // 计算平均功率
  const powerData = hourlyPower.map((total, i) => 
    hourlyCount[i] > 0 ? Math.round(total / hourlyCount[i] * 10) / 10 : null
  )
  
  const hours = Array.from({ length: 24 }, (_, i) => i + ':00')
  
  powerChart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}<br/>功率: {c} kW' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: { type: 'category', data: hours, axisLabel: { interval: 1 } },
    yAxis: { type: 'value', name: 'kW', max: 220 },
    series: [{
      name: '功率',
      type: 'line',
      smooth: true,
      data: powerData,
      connectNulls: false,
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
          [{ xAxis: '8:00', itemStyle: { color: 'rgba(239,68,68,0.1)' } }, { xAxis: '12:00' }],
          [{ xAxis: '18:00', itemStyle: { color: 'rgba(239,68,68,0.1)' } }, { xAxis: '22:00' }]
        ]
      }
    }]
  })
}


// 渲染饼图
function renderPieChart() {
  if (!pieChartRef.value) return
  
  if (!pieChart) {
    pieChart = echarts.init(pieChartRef.value)
  }
  
  const peakVal = parseFloat(costStats.peakEnergy) || 0
  const normalVal = parseFloat(costStats.normalEnergy) || 0
  const valleyVal = parseFloat(costStats.valleyEnergy) || 0
  
  pieChart.setOption({
    tooltip: { formatter: '{b}: {c} kWh ({d}%)' },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      data: [
        { value: peakVal, name: '峰时', itemStyle: { color: '#ef4444' } },
        { value: normalVal, name: '平时', itemStyle: { color: '#f59e0b' } },
        { value: valleyVal, name: '谷时', itemStyle: { color: '#22c55e' } }
      ],
      label: { show: false }
    }]
  })
}

// 窗口大小变化处理
function handleResize() {
  powerChart?.resize()
  pieChart?.resize()
}

let refreshTimer = null

onMounted(() => {
  // 默认查询今天
  dateRange.value = [today, today]
  
  // 加载数据
  loadEnergyData()
  loadCurrentApply()
  
  // 定时刷新（30秒）
  refreshTimer = setInterval(() => {
    loadEnergyData()
  }, 30000)
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('resize', handleResize)
  if (powerChart) powerChart.dispose()
  if (pieChart) pieChart.dispose()
})
</script>

<style lang="scss">
@use '@/styles/workshop.scss';
</style>