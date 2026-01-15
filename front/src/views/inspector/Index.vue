<template>
  <div class="inspector-home">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><i class="bi bi-list-task"></i></div>
        <div class="stat-info">
          <div class="stat-value">5</div>
          <div class="stat-label">待处理任务</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="bi bi-check-circle"></i></div>
        <div class="stat-info">
          <div class="stat-value">12</div>
          <div class="stat-label">今日已完成</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><i class="bi bi-gear"></i></div>
        <div class="stat-info">
          <div class="stat-value">18</div>
          <div class="stat-label">负责设备</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red"><i class="bi bi-exclamation-triangle"></i></div>
        <div class="stat-info">
          <div class="stat-value">2</div>
          <div class="stat-label">异常设备</div>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <!-- 待处理任务 -->
      <div class="card">
        <div class="card-header">
          <span>待处理任务</span>
          <router-link to="/inspector/equipment" class="card-link">查看全部 →</router-link>
        </div>
        <div class="card-body">
          <div class="task-item">
            <div class="task-info">
              <h6>数控机床-01 日常巡检</h6>
              <p>检查设备运行状态、润滑情况、温度等</p>
              <div class="task-meta">
                <span><i class="bi bi-clock"></i>截止: 今天 18:00</span>
                <span class="badge badge-medium">中优先级</span>
              </div>
            </div>
            <button class="btn btn-primary btn-sm">开始巡检</button>
          </div>
          <div class="task-item">
            <div class="task-info">
              <h6>激光切割机-02 日常巡检</h6>
              <p>检查激光头、冷却系统、气路系统</p>
              <div class="task-meta">
                <span><i class="bi bi-clock"></i>截止: 今天 18:00</span>
                <span class="badge badge-medium">中优先级</span>
              </div>
            </div>
            <button class="btn btn-primary btn-sm">开始巡检</button>
          </div>
        </div>
      </div>

      <!-- 设备状态 -->
      <div class="card">
        <div class="card-header">设备状态概览</div>
        <div class="card-body">
          <div class="status-grid">
            <div class="status-item normal">
              <i class="bi bi-check-circle-fill"></i>
              <div class="status-value">15</div>
              <div class="status-label">正常运行</div>
            </div>
            <div class="status-item warning">
              <i class="bi bi-exclamation-triangle-fill"></i>
              <div class="status-value">2</div>
              <div class="status-label">预警状态</div>
            </div>
            <div class="status-item error">
              <i class="bi bi-x-circle-fill"></i>
              <div class="status-value">1</div>
              <div class="status-label">故障停机</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style lang="scss">
@use '@/styles/inspector.scss';
</style>
