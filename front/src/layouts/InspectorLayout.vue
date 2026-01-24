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
      <router-link to="/inspector/message" class="nav-link">
        <i class="bi bi-bell"></i>
        <span>消息中心</span>
      </router-link>
      <router-link to="/inspector/profile" class="nav-link">
        <i class="bi bi-person"></i>
        <span>个人中心</span>
      </router-link>
    </template>
  </BaseLayout>
</template>

<script setup>
import BaseLayout from './BaseLayout.vue'
</script>
