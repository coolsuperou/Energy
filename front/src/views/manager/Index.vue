<template>
  <div class="manager-home">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><i class="bi bi-lightning-charge"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(indicators.todayEnergy) }}</div>
          <div class="stat-label">今日用电量 (kWh)</div>
          <div class="stat-trend" :class="indicators.dailyChangeRate <= 0 ? 'up' : 'down'">
            <i :class="indicators.dailyChangeRate <= 0 ? 'bi bi-arrow-down' : 'bi bi-arrow-up'"></i>
            较昨日 {{ indicators.dailyChangeRate > 0 ? '+' : '' }}{{ indicators.dailyChangeRate }}%
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="bi bi-calendar-month"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ formatNumber(indicators.monthEnergy) }}</div>
          <div class="stat-label">本月用电量 (kWh)</div>
          <div class="stat-trend" :class="indicators.energyChangeRate <= 0 ? 'up' : 'down'">
            <i :class="indicators.energyChangeRate <= 0 ? 'bi bi-arrow-down' : 'bi bi-arrow-up'"></i>
            同比 {{ indicators.energyChangeRate > 0 ? '+' : '' }}{{ indicators.energyChangeRate }}%
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><i class="bi bi-currency-yen"></i></div>
        <div class="stat-info">
          <div class="stat-value">¥{{ formatNumber(indicators.monthCost) }}</div>
          <div class="stat-label">本月电费</div>
          <div class="stat-trend" :class="indicators.costChangeRate <= 0 ? 'up' : 'down'">
            <i :class="indicators.costChangeRate <= 0 ? 'bi bi-arrow-down' : 'bi bi-arrow-up'"></i>
            同比 {{ indicators.costChangeRate > 0 ? '+' : '' }}{{ indicators.costChangeRate }}%
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><i class="bi bi-clipboard-check"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ pendingItems.total || 0 }}</div>
          <div class="stat-label">待处理事项</div>
          <div class="stat-trend muted">
            申请 {{ pendingItems.pendingApplications || 0 }} · 认证 {{ pendingItems.pendingCertifications || 0 }}
          </div>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <!-- 用电趋势图表 -->
      <div class="card chart-card">
        <div class="card-header">
          <span>近7天用电趋势</span>
        </div>
        <div class="card-body">
          <div ref="trendChartRef" class="trend-chart"></div>
        </div>
      </div>

      <!-- 车间排名 -->
      <div class="card">
        <div class="card-header">车间用电排名（今日）</div>
        <div class="card-body">
          <div class="ranking-list" v-if="workshopRanking.length">
            <div class="ranking-item" v-for="(item, index) in workshopRanking" :key="item.workshopId">
              <span class="ranking-badge" :class="getRankClass(index)">{{ index + 1 }}</span>
              <span class="ranking-name">{{ item.workshopName }}</span>
              <div class="ranking-bar-wrapper">
                <div class="ranking-bar" :style="{ width: getBarWidth(item.energy) + '%' }"></div>
              </div>
              <span class="ranking-value">{{ formatNumber(item.energy) }} kWh</span>
            </div>
          </div>
          <div class="empty-tip" v-else>暂无车间数据</div>
        </div>
      </div>

      <!-- 待处理事项详情 -->
      <div class="card">
        <div class="card-header">
          <span>待处理事项</span>
        </div>
        <div class="card-body">
          <div class="pending-list">
            <div class="pending-item" @click="goToApproval" v-if="pendingItems.pendingApplications > 0">
              <div class="pending-icon approval"><i class="bi bi-file-earmark-text"></i></div>
              <div class="pending-info">
                <div class="pending-title">待审批申请</div>
                <div class="pending-desc">{{ pendingItems.pendingApplications }} 个用电申请等待审批</div>
              </div>
              <i class="bi bi-chevron-right"></i>
            </div>
            <div class="pending-item" @click="goToUsers" v-if="pendingItems.pendingCertifications > 0">
              <div class="pending-icon cert"><i class="bi bi-award"></i></div>
              <div class="pending-info">
                <div class="pending-title">待审核认证</div>
                <div class="pending-desc">{{ pendingItems.pendingCertifications }} 个技能认证待审核</div>
              </div>
              <i class="bi bi-chevron-right"></i>
            </div>
            <div class="empty-tip" v-if="pendingItems.total === 0">
              <i class="bi bi-check-circle text-success"></i> 暂无待处理事项
            </div>
          </div>
        </div>
      </div>

      <!-- AI 建议 -->
      <div class="card ai-card">
        <div class="card-header">
          <span><i class="bi bi-robot me-2"></i>AI 节能建议</span>
          <router-link to="/manager/ai" class="card-link">查看详情 →</router-link>
        </div>
        <div class="card-body">
          <div class="ai-suggestion" v-if="workshopRanking.length">
            <i class="bi bi-lightbulb"></i>
            <div class="suggestion-content">
              <h6>关注高能耗车间</h6>
              <p>{{ workshopRanking[0]?.workshopName }} 今日能耗最高（{{ formatNumber(workshopRanking[0]?.energy) }} kWh），建议优化用电时段分配</p>
            </div>
          </div>
          <div class="ai-suggestion">
            <i class="bi bi-gear"></i>
            <div class="suggestion-content">
              <h6>峰谷电价优化</h6>
              <p>将部分生产任务调整至谷时段运行，可有效降低电费成本</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getManagerDashboard } from '@/api/manager'
import * as echarts from 'echarts'

const router = useRouter()

const indicators = ref({
  todayEnergy: 0,
  todayCost: 0,
  monthEnergy: 0,
  monthCost: 0,
  energyChangeRate: 0,
  costChangeRate: 0,
  dailyChangeRate: 0
})

const workshopRanking = ref([])
const pendingItems = ref({
  pendingApplications: 0,
  pendingCertifications: 0,
  total: 0
})
const energyTrend = ref([])

const trendChartRef = ref(null)
let trendChart = null

// 计算排名最大值用于进度条
const maxEnergy = ref(0)

function formatNumber(num) {
  if (!num) return '0'
  return Number(num).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
}

function getRankClass(index) {
  const classes = ['gold', 'silver', 'bronze', 'normal']
  return classes[index] || 'normal'
}

function getBarWidth(energy) {
  if (!maxEnergy.value || !energy) return 0
  return (energy / maxEnergy.value) * 100
}

function goToApproval() {
  // 跳转到申请审批页面（如果有的话）
  router.push('/manager/analysis')
}

function goToUsers() {
  // 跳转到用户管理页面（如果有的话）
  router.push('/manager/users')
}

// 初始化趋势图表
function initTrendChart() {
  if (!trendChartRef.value) return
  
  trendChart = echarts.init(trendChartRef.value)
  
  const dates = energyTrend.value.map(item => {
    const date = new Date(item.date)
    return `${date.getMonth() + 1}/${date.getDate()}`
  })
  const energyData = energyTrend.value.map(item => Number(item.energy) || 0)
  const costData = energyTrend.value.map(item => Number(item.cost) || 0)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['用电量(kWh)', '电费(元)'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLine: { lineStyle: { color: '#ddd' } },
      axisLabel: { color: '#666' }
    },
    yAxis: [
      {
        type: 'value',
        name: '用电量(kWh)',
        position: 'left',
        axisLine: { lineStyle: { color: '#5470c6' } },
        axisLabel: { color: '#666' }
      },
      {
        type: 'value',
        name: '电费(元)',
        position: 'right',
        axisLine: { lineStyle: { color: '#91cc75' } },
        axisLabel: { color: '#666' }
      }
    ],
    series: [
      {
        name: '用电量(kWh)',
        type: 'line',
        smooth: true,
        data: energyData,
        yAxisIndex: 0,
        itemStyle: { color: '#5470c6' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(84, 112, 198, 0.3)' },
            { offset: 1, color: 'rgba(84, 112, 198, 0.05)' }
          ])
        }
      },
      {
        name: '电费(元)',
        type: 'line',
        smooth: true,
        data: costData,
        yAxisIndex: 1,
        itemStyle: { color: '#91cc75' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(145, 204, 117, 0.3)' },
            { offset: 1, color: 'rgba(145, 204, 117, 0.05)' }
          ])
        }
      }
    ]
  }
  
  trendChart.setOption(option)
}

// 窗口大小变化时重绘图表
function handleResize() {
  trendChart?.resize()
}

async function loadData() {
  try {
    const res = await getManagerDashboard()
    if (res.code === 200 && res.data) {
      // 用电指标
      if (res.data.energyIndicators) {
        indicators.value = res.data.energyIndicators
      }
      
      // 车间排名
      if (res.data.workshopRanking) {
        workshopRanking.value = res.data.workshopRanking
        maxEnergy.value = workshopRanking.value.length > 0 
          ? Math.max(...workshopRanking.value.map(w => Number(w.energy) || 0))
          : 0
      }
      
      // 待处理事项
      if (res.data.pendingItems) {
        pendingItems.value = res.data.pendingItems
      }
      
      // 用电趋势
      if (res.data.energyTrend) {
        energyTrend.value = res.data.energyTrend
        await nextTick()
        initTrendChart()
      }
    }
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style lang="scss">
@use '@/styles/manager.scss';

.manager-home {
  .trend-chart {
    width: 100%;
    height: 280px;
  }
  
  .ranking-item {
    display: flex;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid #f0f0f0;
    gap: 10px;
    
    &:last-child {
      border-bottom: none;
    }
    
    .ranking-badge {
      width: 24px;
      height: 24px;
      min-width: 24px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 600;
    }
    
    .ranking-name {
      width: 80px;
      min-width: 80px;
      font-size: 14px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    
    .ranking-bar-wrapper {
      flex: 1;
      height: 8px;
      background: #f5f5f5;
      border-radius: 4px;
      overflow: hidden;
      min-width: 60px;
      
      .ranking-bar {
        height: 100%;
        background: linear-gradient(90deg, #5470c6, #91cc75);
        border-radius: 4px;
        transition: width 0.3s ease;
      }
    }
    
    .ranking-value {
      min-width: 90px;
      text-align: right;
      font-weight: 500;
      color: #333;
      font-size: 13px;
      white-space: nowrap;
    }
  }
  
  .pending-list {
    .pending-item {
      display: flex;
      align-items: center;
      padding: 14px;
      margin-bottom: 10px;
      background: #fafafa;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
      
      &:hover {
        background: #f0f0f0;
      }
      
      &:last-child {
        margin-bottom: 0;
      }
      
      .pending-icon {
        width: 40px;
        height: 40px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 14px;
        
        i {
          font-size: 18px;
          color: #fff;
        }
        
        &.approval {
          background: linear-gradient(135deg, #5470c6, #73a0fa);
        }
        
        &.cert {
          background: linear-gradient(135deg, #91cc75, #a8e063);
        }
      }
      
      .pending-info {
        flex: 1;
        
        .pending-title {
          font-weight: 500;
          color: #333;
          margin-bottom: 4px;
        }
        
        .pending-desc {
          font-size: 13px;
          color: #999;
        }
      }
      
      > i {
        color: #ccc;
      }
    }
  }
}
</style>
