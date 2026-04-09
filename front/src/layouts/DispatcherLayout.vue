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
        <span v-if="pendingApplicationCount > 0" class="badge bg-danger rounded-pill">{{ pendingApplicationCount }}</span>
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
import { onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import BaseLayout from './BaseLayout.vue'
import { useDispatcherNavStore } from '@/stores/dispatcherNav'

const dispatcherNav = useDispatcherNavStore()
const { pendingApplicationCount, unreadCount, pendingFeedbackCount, pendingTaskCount } =
  storeToRefs(dispatcherNav)

onMounted(() => {
  dispatcherNav.refreshAll()
})
</script>
