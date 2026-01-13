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
          <div class="chart-placeholder">
            <i class="bi bi-graph-up"></i>
            <span>功率趋势图表</span>
          </div>
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
import { ref, onMounted } from 'vue'
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

async function loadData() {
  try {
    const res = await getMyApplications({ page: 1, size: 5 })
    if (res && res.code === 200 && res.data) {
      recentApplications.value = res.data.records || res.data || []
    }
  } catch (e) {
    // 接口暂时不可用，使用空数据
    console.warn('加载申请列表失败，使用空数据', e)
    recentApplications.value = []
  }
}

onMounted(() => {
  const now = new Date()
  currentTime.value = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}`
  loadData()
})
</script>

<style lang="scss">
@import '@/styles/workshop.scss';
</style>
