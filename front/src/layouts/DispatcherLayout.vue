<template>
  <BaseLayout roleName="能源调度员" messageRoute="/dispatcher/message">
    <template #menu>
      <router-link to="/dispatcher" class="nav-link" exact-active-class="active">
        <i class="bi bi-speedometer2"></i>
        <span>工作台</span>
      </router-link>
      <router-link to="/dispatcher/approval" class="nav-link">
        <i class="bi bi-clipboard-check"></i>
        <span>申请审批</span>
        <span v-if="pendingCount > 0" class="badge bg-danger rounded-pill">{{ pendingCount }}</span>
      </router-link>
      <router-link to="/dispatcher/dispatch" class="nav-link">
        <i class="bi bi-diagram-3"></i>
        <span>电力调度</span>
      </router-link>
      <router-link to="/dispatcher/alert" class="nav-link">
        <i class="bi bi-exclamation-triangle"></i>
        <span>预警处理</span>
        <span v-if="alertCount > 0" class="badge bg-danger rounded-pill">{{ alertCount }}</span>
      </router-link>
      <router-link to="/dispatcher/order" class="nav-link">
        <i class="bi bi-card-checklist"></i>
        <span>工单管理</span>
      </router-link>
      <router-link to="/dispatcher/message" class="nav-link">
        <i class="bi bi-bell"></i>
        <span>消息通知</span>
      </router-link>
    </template>
  </BaseLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import BaseLayout from './BaseLayout.vue'
import { getPendingApplications } from '@/api/application'

const pendingCount = ref(0)
const alertCount = ref(0)

// 加载待审批数量
async function loadCounts() {
  try {
    const res = await getPendingApplications({ page: 1, size: 1 })
    if (res && res.code === 200 && res.data) {
      pendingCount.value = res.data.total || (res.data.records || res.data || []).length
    }
  } catch (e) {
    console.warn('加载待审批数量失败', e)
  }
  
  // 预警数量暂时模拟
  alertCount.value = Math.floor(Math.random() * 5)
}

onMounted(() => {
  loadCounts()
})
</script>
