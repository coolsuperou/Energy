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
      <router-link to="/manager/message" class="nav-link">
        <i class="bi bi-bell"></i>
        <span>消息中心</span>
      </router-link>
    </template>
  </BaseLayout>
</template>

<script setup>
import BaseLayout from './BaseLayout.vue'
</script>
