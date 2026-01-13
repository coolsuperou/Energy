<template>
  <BaseLayout roleName="能源调度员" messageRoute="/dispatcher/message">
    <template #menu>
      <router-link to="/dispatcher" class="nav-link" exact-active-class="active">
        <i class="bi bi-house-door"></i>
        <span>首页概览</span>
      </router-link>
      <router-link to="/dispatcher/approval" class="nav-link">
        <i class="bi bi-check-circle"></i>
        <span>申请审批</span>
      </router-link>
      <router-link to="/dispatcher/tasks" class="nav-link">
        <i class="bi bi-list-task"></i>
        <span>任务管理</span>
      </router-link>
      <router-link to="/dispatcher/message" class="nav-link">
        <i class="bi bi-bell"></i>
        <span>消息中心</span>
      </router-link>
    </template>
  </BaseLayout>
</template>

<script setup>
import BaseLayout from './BaseLayout.vue'
</script>
