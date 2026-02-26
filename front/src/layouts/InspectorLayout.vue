<template>
  <BaseLayout roleName="设备巡检员" messageRoute="/inspector/message">
    <template #menu>
      <router-link to="/inspector" class="nav-link" exact-active-class="active">
        <i class="bi bi-inbox"></i>
        <span>工作台</span>
      </router-link>
      <router-link to="/inspector/equipment" class="nav-link">
        <i class="bi bi-card-checklist"></i>
        <span>我的任务</span>
      </router-link>
      <router-link to="/inspector/weekly-inspection" class="nav-link">
        <i class="bi bi-clipboard2-check"></i>
        <span>每周巡检</span>
      </router-link>
      <router-link to="/inspector/message" class="nav-link">
        <i class="bi bi-bell"></i>
        <span>消息中心</span>
        <span v-if="unreadCount > 0" class="badge bg-danger rounded-pill">{{ unreadCount }}</span>
      </router-link>
      <router-link to="/inspector/profile" class="nav-link">
        <i class="bi bi-person"></i>
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
