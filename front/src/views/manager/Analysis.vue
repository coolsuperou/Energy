<template>
  <div class="analysis-page">
    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-group">
        <label>车间</label>
        <select v-model="filters.workshopId" @change="loadData">
          <option :value="null">全部车间</option>
          <option v-for="ws in workshops" :key="ws.id" :value="ws.id">{{ ws.name }}</option>
        </select>
      </div>
      <div class="filter-group">
        <label>日期范围</label>
        <input type="date" v-model="filters.startDate" @change="loadData" />
        <span class="filter-sep">至</span>
        <input type="date" v-model="filters.endDate" @change="loadData" />
      </div>
      <div class="filter-group">
        <button class="btn-query" @click="loadData"><i class="bi bi-search"></i> 查询</button>
      </div>
    </div>

    <!-- 汇总卡片 -->
    <div class="summary-cards">
      <div class="summary-card">
        <div class="summary-icon blue"><i class="bi bi-lightning-charge"></i></div>
        <div class="summary-info">
          <div class="summary-value">{{ formatNum(summary.totalEnergy) }}</div>
          <div class="summary-label">总能耗 (kWh)</div>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-icon green"><i class="bi bi-currency-yen"></i></div>
        <div class="summary-info">
          <div class="summary-value">¥{{ formatNum(summary.totalCost) }}</div>
          <div class="summary-label">总电费</div>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-icon orange"><i class="bi bi-speedometer2"></i></div>
        <div class="summary-info">
          <div class="summary-value">{{ formatNum(summary.avgPower) }}</div>
          <div class="summary-label">平均功率 (kW)</div>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-icon red"><i class="bi bi-graph-up-arrow"></i></div>
        <div class="summary-info">
          <div class="summary-value">{{ formatNum(summary.peakEnergy) }}</div>
          <div class="summary-label">峰时能耗 (kWh)</div>
        </div>
      </div>
      <div class="summary-card">
        <div class="summary-icon teal"><i class="bi bi-graph-down-arrow"></i></div>
        <div class="summary-info">
          <div class="summary-value">{{ formatNum(summary.valleyEnergy) }}</div>
          <div class="summary-label">谷时能耗 (kWh)</div>
        </div>
      </div>
    </div>

    <div class="analysis-grid">
      <!-- 成本趋势图（折线图） -->
      <div class="card wide">
        <div class="card-header">
          <span>成本趋势</span>
          <div class="granularity-tabs">
            <button :class="{ active: granularity === 'daily' }" @click="changeGranularity('daily')">日</button>
            <button :class="{ active: granularity === 'weekly' }" @click="changeGranularity('weekly')">周</button>
            <button :class="{ active: granularity === 'monthly' }" @click="changeGranularity('monthly')">月</button>
          </div>
        </div>
        <div class="card-body">
          <div ref="costChartRef" style="height: 300px;" v-if="costTrendData.length"></div>
          <div class="empty-tip" v-else>暂无数据</div>
        </div>
      </div>

      <!-- 峰谷平分布 -->
      <div class="card">
        <div class="card-header">峰谷平能耗分布</div>
        <div class="card-body">
          <div class="period-dist" v-if="summary.totalEnergy > 0">
            <div class="period-bar-stack">
              <div class="period-seg peak" :style="{ width: periodPercent.peak + '%' }">
                {{ periodPercent.peak }}%
              </div>
              <div class="period-seg normal" :style="{ width: periodPercent.normal + '%' }">
                {{ periodPercent.normal }}%
              </div>
              <div class="period-seg valley" :style="{ width: periodPercent.valley + '%' }">
                {{ periodPercent.valley }}%
              </div>
            </div>
            <div class="period-details">
              <div class="period-row">
                <span class="dot peak"></span>
                <span class="period-name">峰时 (8-12, 18-22点)</span>
                <span class="period-val">{{ formatNum(summary.peakEnergy) }} kWh</span>
                <span class="period-cost">¥{{ formatNum(summary.peakCost) }}</span>
              </div>
              <div class="period-row">
                <span class="dot normal"></span>
                <span class="period-name">平时 (12-18点)</span>
                <span class="period-val">{{ formatNum(summary.normalEnergy) }} kWh</span>
                <span class="period-cost">¥{{ formatNum(summary.normalCost) }}</span>
              </div>
              <div class="period-row">
                <span class="dot valley"></span>
                <span class="period-name">谷时 (22-8点)</span>
                <span class="period-val">{{ formatNum(summary.valleyEnergy) }} kWh</span>
                <span class="period-cost">¥{{ formatNum(summary.valleyCost) }}</span>
              </div>
            </div>
          </div>
          <div class="empty-tip" v-else>暂无数据</div>
        </div>
      </div>

      <!-- 车间对比 -->
      <div class="card">
        <div class="card-header">车间能耗对比</div>
        <div class="card-body">
          <div class="ws-compare" v-if="workshopCompare.length">
            <div class="ws-row" v-for="ws in workshopCompare" :key="ws.workshopId">
              <span class="ws-name">{{ ws.workshopName }}</span>
              <div class="ws-bar-bg">
                <div class="ws-bar-fill" :style="{ width: ws.percent + '%' }"></div>
              </div>
              <span class="ws-val">{{ formatNum(ws.energy) }} kWh</span>
              <span class="ws-cost">¥{{ formatNum(ws.cost) }}</span>
            </div>
          </div>
          <div class="empty-tip" v-else>暂无数据</div>
        </div>
      </div>
    </div>

    <!-- 明细表格 -->
    <div class="card detail-table-card">
      <div class="card-header">
        <span>能耗明细数据</span>
        <span class="record-count">共 {{ tableData.length }} 条记录</span>
      </div>
      <div class="card-body">
        <div class="table-wrapper" v-if="tableData.length">
          <table class="data-table">
            <thead>
              <tr>
                <th>日期</th>
                <th>时段</th>
                <th>车间</th>
                <th>功率 (kW)</th>
                <th>能耗 (kWh)</th>
                <th>电价 (元)</th>
                <th>费用 (元)</th>
                <th>时段类型</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in paginatedData" :key="row.id">
                <td>{{ row.recordDate }}</td>
                <td>{{ row.recordHour }}:00</td>
                <td>{{ workshopNameMap[row.workshopId] || ('车间' + row.workshopId) }}</td>
                <td>{{ Number(row.power).toFixed(1) }}</td>
                <td>{{ Number(row.energy).toFixed(2) }}</td>
                <td>{{ Number(row.price).toFixed(2) }}</td>
                <td>{{ Number(row.cost).toFixed(2) }}</td>
                <td>
                  <span class="period-tag" :class="(row.periodType || '').toLowerCase()">
                    {{ periodLabel[row.periodType] || row.periodType }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="empty-tip" v-else>暂无明细数据</div>
        <!-- 分页 -->
        <div class="pagination" v-if="totalPages > 1">
          <button @click="currentPage = Math.max(1, currentPage - 1)" :disabled="currentPage === 1">上一页</button>
          <span>{{ currentPage }} / {{ totalPages }}</span>
          <button @click="currentPage = Math.min(totalPages, currentPage + 1)" :disabled="currentPage === totalPages">下一页</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { getEnergyDataByRange, getEnergySummary, getCostTrend } from '@/api/energy'

const workshopNameMap = { 1: '生产一车间', 2: '生产二车间', 3: '装配车间', 4: '仓储车间' }
const workshops = [
  { id: 1, name: '生产一车间' }, { id: 2, name: '生产二车间' },
  { id: 3, name: '装配车间' }, { id: 4, name: '仓储车间' }
]
const periodLabel = { PEAK: '峰时', NORMAL: '平时', VALLEY: '谷时' }

// 默认查最近7天
const now = new Date()
const sevenDaysAgo = new Date(now.getTime() - 6 * 86400000)
const filters = ref({
  workshopId: null,
  startDate: formatDate(sevenDaysAgo),
  endDate: formatDate(now)
})

const rawData = ref([])
const workshopCompare = ref([])
const costTrendData = ref([])
const granularity = ref('daily')
const currentPage = ref(1)
const pageSize = 20

// ECharts 图表引用
const costChartRef = ref(null)
let costChart = null

const summary = ref({
  totalEnergy: 0, totalCost: 0, avgPower: 0,
  peakEnergy: 0, peakCost: 0,
  normalEnergy: 0, normalCost: 0,
  valleyEnergy: 0, valleyCost: 0
})

// 渲染成本趋势 ECharts 图表
function renderCostChart() {
  if (!costChartRef.value || !costTrendData.value.length) return
  
  if (!costChart) {
    costChart = echarts.init(costChartRef.value)
  }
  
  const dates = costTrendData.value.map(d => d.label)
  const costs = costTrendData.value.map(d => d.cost)
  const energies = costTrendData.value.map(d => d.energy)
  
  costChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.95)',
      borderColor: '#e2e8f0',
      borderWidth: 1,
      textStyle: { color: '#334155', fontSize: 13 },
      formatter: params => {
        let html = `<div style="font-weight:600;margin-bottom:6px">${params[0].axisValue}</div>`
        params.forEach(p => {
          html += `<div style="display:flex;align-items:center;gap:6px;margin:3px 0">`
          html += `${p.marker}<span>${p.seriesName}：</span>`
          html += `<span style="font-weight:600">${p.seriesName === '费用' ? '¥' : ''}${Number(p.value).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}${p.seriesName === '能耗' ? ' kWh' : ''}</span>`
          html += `</div>`
        })
        return html
      }
    },
    legend: {
      data: ['费用', '能耗'],
      top: -5,
      right: 0,
      textStyle: { fontSize: 12, color: '#64748b' }
    },
    grid: { top: 45, right: 70, bottom: 30, left: 65 },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#94a3b8', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: [
      {
        type: 'value',
        name: '费用 (¥)',
        nameTextStyle: { color: '#94a3b8', fontSize: 11 },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#94a3b8', fontSize: 11, formatter: '¥{value}' },
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } }
      },
      {
        type: 'value',
        name: '能耗 (kWh)',
        nameTextStyle: { color: '#94a3b8', fontSize: 11 },
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#94a3b8', fontSize: 11 },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '费用',
        type: 'line',
        data: costs,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: '#10b981' },
        lineStyle: { color: '#10b981', width: 2.5 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(16, 185, 129, 0.3)' },
            { offset: 1, color: 'rgba(16, 185, 129, 0.02)' }
          ])
        },
        label: {
          show: true,
          position: 'top',
          formatter: p => '¥' + Number(p.value).toFixed(0),
          fontSize: 11,
          color: '#10b981'
        }
      },
      {
        name: '能耗',
        type: 'line',
        yAxisIndex: 1,
        data: energies,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        itemStyle: { color: '#3b82f6' },
        lineStyle: { color: '#3b82f6', width: 2, type: 'dashed' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.15)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.02)' }
          ])
        }
      }
    ]
  })
}

// 监听数据变化重新渲染图表
watch(costTrendData, () => {
  nextTick(renderCostChart)
}, { deep: true })

// 峰谷平百分比
const periodPercent = computed(() => {
  const total = summary.value.totalEnergy || 1
  return {
    peak: ((summary.value.peakEnergy / total) * 100).toFixed(0),
    normal: ((summary.value.normalEnergy / total) * 100).toFixed(0),
    valley: ((summary.value.valleyEnergy / total) * 100).toFixed(0)
  }
})

// 表格数据（按筛选）
const tableData = computed(() => {
  let data = rawData.value
  if (filters.value.workshopId) {
    data = data.filter(d => d.workshopId === filters.value.workshopId)
  }
  return data
})

const totalPages = computed(() => Math.ceil(tableData.value.length / pageSize) || 1)
const paginatedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return tableData.value.slice(start, start + pageSize)
})

function formatDate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function formatNum(num) {
  if (!num) return '0'
  return Number(num).toLocaleString('zh-CN', { maximumFractionDigits: 1 })
}

async function loadData() {
  currentPage.value = 1
  try {
    const params = {
      startDate: filters.value.startDate,
      endDate: filters.value.endDate
    }
    if (filters.value.workshopId) params.workshopId = filters.value.workshopId

    // 加载汇总数据
    const summaryRes = await getEnergySummary(params)
    if (summaryRes.code === 200 && summaryRes.data) {
      const data = summaryRes.data
      
      // 设置概览数据
      if (data.overview) {
        summary.value.totalEnergy = Number(data.overview.totalEnergy || 0)
        summary.value.totalCost = Number(data.overview.totalCost || 0)
        summary.value.avgPower = Number(data.overview.avgPower || 0)
      }
      
      // 设置时段分析数据
      if (data.periodAnalysis && data.periodAnalysis.periods) {
        const periods = data.periodAnalysis.periods
        const peak = periods.find(p => p.type === 'PEAK') || {}
        const normal = periods.find(p => p.type === 'NORMAL') || {}
        const valley = periods.find(p => p.type === 'VALLEY') || {}
        
        summary.value.peakEnergy = Number(peak.energy || 0)
        summary.value.peakCost = Number(peak.cost || 0)
        summary.value.normalEnergy = Number(normal.energy || 0)
        summary.value.normalCost = Number(normal.cost || 0)
        summary.value.valleyEnergy = Number(valley.energy || 0)
        summary.value.valleyCost = Number(valley.cost || 0)
      }
      
      // 设置车间对比数据
      if (data.workshopCompare && data.workshopCompare.workshops) {
        workshopCompare.value = data.workshopCompare.workshops.map(ws => ({
          workshopId: ws.workshopId,
          workshopName: ws.workshopName,
          energy: Number(ws.energy || 0),
          cost: Number(ws.cost || 0),
          percent: Number(ws.percent || 0)
        }))
      }
      
      // 设置成本趋势数据
      if (data.costTrend && data.costTrend.trend) {
        costTrendData.value = data.costTrend.trend.map(t => ({
          date: t.date,
          label: t.label,
          energy: Number(t.energy || 0),
          cost: Number(t.cost || 0)
        }))
      }
    }

    // 加载明细数据
    const detailRes = await getEnergyDataByRange(params)
    if (detailRes.code === 200) {
      rawData.value = detailRes.data || []
    }
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

async function changeGranularity(newGranularity) {
  granularity.value = newGranularity
  try {
    const params = {
      startDate: filters.value.startDate,
      endDate: filters.value.endDate,
      granularity: newGranularity
    }
    if (filters.value.workshopId) params.workshopId = filters.value.workshopId
    
    const res = await getCostTrend(params)
    if (res.code === 200 && res.data && res.data.trend) {
      costTrendData.value = res.data.trend.map(t => ({
        date: t.date,
        label: t.label,
        energy: Number(t.energy || 0),
        cost: Number(t.cost || 0)
      }))
    }
  } catch (e) {
    console.error('加载成本趋势失败', e)
  }
}

function handleResize() {
  costChart && costChart.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (costChart) {
    costChart.dispose()
    costChart = null
  }
})
</script>

<style lang="scss">
@use '@/styles/manager.scss';

.granularity-tabs {
  display: flex;
  gap: 4px;
  
  button {
    padding: 4px 12px;
    border: 1px solid #e2e8f0;
    background: #fff;
    border-radius: 4px;
    cursor: pointer;
    font-size: 12px;
    color: #64748b;
    transition: all 0.2s;
    
    &:hover {
      border-color: #3b82f6;
      color: #3b82f6;
    }
    
    &.active {
      background: #3b82f6;
      border-color: #3b82f6;
      color: #fff;
    }
  }
}

.ws-cost {
  font-size: 12px;
  color: #10b981;
  margin-left: 8px;
  min-width: 70px;
  text-align: right;
}
</style>
