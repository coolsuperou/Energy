<template>
  <BaseLayout roleName="能源经理" messageRoute="/manager/message">
    <template #menu>
      <router-link to="/manager" class="nav-link" exact-active-class="active">
        <i class="bi bi-house-door"></i>
        <span>首页概览</span>
      </router-link>
      <router-link to="/manager/analysis" class="nav-link">
        <i class="bi bi-bar-chart-line"></i>
        <span>数据分析</span>
      </router-link>
      <router-link to="/manager/ai" class="nav-link">
        <i class="bi bi-robot"></i>
        <span>AI分析</span>
      </router-link>
      <router-link to="/manager/users" class="nav-link">
        <i class="bi bi-people"></i>
        <span>用户管理</span>
      </router-link>
      <router-link to="/manager/schedule" class="nav-link">
        <i class="bi bi-calendar-week"></i>
        <span>排班考勤</span>
      </router-link>
      <router-link to="/manager/message" class="nav-link">
        <i class="bi bi-bell"></i>
        <span>消息中心</span>
        <span v-if="unreadCount > 0" class="badge bg-danger rounded-pill">{{ unreadCount }}</span>
      </router-link>
      <router-link to="/manager/profile" class="nav-link">
        <i class="bi bi-person-circle"></i>
        <span>个人中心</span>
      </router-link>
    </template>
  </BaseLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import BaseLayout from './BaseLayout.vue'
import { getUnreadCount } from '@/api/notification'

const unreadCount = ref(0)

async function loadUnreadCount() {
  try {
    const res = await getUnreadCount()
    if (res && res.code === 200) {
      unreadCount.value = res.data || 0
    }
  } catch (e) {
    console.warn('加载未读消息数量失败', e)
  }
}

onMounted(() => {
  loadUnreadCount()
})
</script>
