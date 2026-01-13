<template>
  <BaseLayout roleName="车间用户" messageRoute="/workshop/message">
    <template #menu>
      <router-link to="/workshop" class="nav-link">
        <i class="bi bi-house-door"></i>
        <span>首页概览</span>
      </router-link>
      <router-link to="/workshop/apply" class="nav-link">
        <i class="bi bi-file-earmark-text"></i>
        <span>用电申请</span>
      </router-link>
      <router-link to="/workshop/energy" class="nav-link">
        <i class="bi bi-bar-chart-line"></i>
        <span>能耗查看</span>
      </router-link>
      <router-link to="/workshop/feedback" class="nav-link">
        <i class="bi bi-chat-dots"></i>
        <span>问题反馈</span>
      </router-link>
      <router-link to="/workshop/message" class="nav-link">
        <i class="bi bi-bell"></i>
        <span>消息中心</span>
      </router-link>
    </template>
  </BaseLayout>
</template>

<script setup>
import BaseLayout from './BaseLayout.vue'
</script>
