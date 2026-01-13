<template>
  <BaseLayout roleName="系统管理员" messageRoute="/admin">
    <template #menu>
      <router-link to="/admin" class="nav-link" exact-active-class="active">
        <i class="bi bi-house-door"></i>
        <span>首页概览</span>
      </router-link>
      <router-link to="/admin/users" class="nav-link">
        <i class="bi bi-people"></i>
        <span>用户管理</span>
      </router-link>
      <router-link to="/admin/config" class="nav-link">
        <i class="bi bi-gear"></i>
        <span>系统配置</span>
      </router-link>
      <router-link to="/admin/logs" class="nav-link">
        <i class="bi bi-journal-text"></i>
        <span>操作日志</span>
      </router-link>
    </template>
  </BaseLayout>
</template>

<script setup>
import BaseLayout from './BaseLayout.vue'
</script>
