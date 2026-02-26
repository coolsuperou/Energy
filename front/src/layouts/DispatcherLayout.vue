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
      <router-link to="/dispatcher/order" class="nav-link">
        <i class="bi bi-card-checklist"></i>
        <span>工单管理</span>
        <span v-if="pendingTaskCount > 0" class="badge bg-danger rounded-pill">{{ pendingTaskCount }}</span>
      </router-link>
      <router-link to="/dispatcher/feedback" class="nav-link">
        <i class="bi bi-chat-dots"></i>
        <span>问题反馈</span>
        <span v-if="pendingFeedbackCount > 0" class="badge bg-danger rounded-pill">{{ pendingFeedbackCount }}</span>
      </router-link>
      <router-link to="/dispatcher/inspection-schedule" class="nav-link">
        <i class="bi bi-calendar2-week"></i>
        <span>巡检排班</span>
      </router-link>
      <router-link to="/dispatcher/message" class="nav-link">
        <i class="bi bi-bell"></i>
        <span>消息通知</span>
        <span v-if="unreadCount > 0" class="badge bg-danger rounded-pill">{{ unreadCount }}</span>
      </router-link>
      <router-link to="/dispatcher/profile" class="nav-link">
        <i class="bi bi-person-circle"></i>
        <span>个人中心</span>
      </router-link>
    </template>
  </BaseLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import BaseLayout from './BaseLayout.vue'
import { getPendingApplications } from '@/api/application'
import { getUnreadCount } from '@/api/notification'
import { getAllFeedbacks } from '@/api/feedback'
import { getTasks } from '@/api/task'

const pendingCount = ref(0)
const unreadCount = ref(0)
const pendingFeedbackCount = ref(0)
const pendingTaskCount = ref(0)

async function loadCounts() {
  try {
    const res = await getPendingApplications({ page: 1, size: 1 })
    if (res && res.code === 200 && res.data) {
      pendingCount.value = res.data.total || (res.data.records || res.data || []).length
    }
  } catch (e) {
    console.warn('加载待审批数量失败', e)
  }
  
  // 获取未读消息数量
  try {
    const res = await getUnreadCount()
    if (res && res.code === 200) {
      unreadCount.value = res.data || 0
    }
  } catch (e) {
    console.warn('加载未读消息数量失败', e)
    unreadCount.value = 0
  }
  
  // 获取待处理反馈数量
  try {
    const res = await getAllFeedbacks({ page: 1, size: 1, status: 'pending' })
    if (res && res.code === 200 && res.data) {
      pendingFeedbackCount.value = res.data.total || 0
    }
  } catch (e) {
    console.warn('加载待处理反馈数量失败', e)
  }
  
  // 获取待处理工单数量
  try {
    const res = await getTasks({ page: 1, size: 1, status: 'PENDING' })
    if (res && res.code === 200 && res.data) {
      pendingTaskCount.value = res.data.total || 0
    }
  } catch (e) {
    console.warn('加载待处理工单数量失败', e)
  }
}

onMounted(() => {
  loadCounts()
})
</script>
