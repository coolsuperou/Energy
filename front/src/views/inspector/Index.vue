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

<style lang="scss" scoped>
.inspector-home { color: #1e293b; }

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  display: flex;
  align-items: center;
  gap: 16px;
  
  .stat-icon {
    width: 56px;
    height: 56px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: #fff;
    
    &.blue { background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%); }
    &.green { background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%); }
    &.orange { background: linear-gradient(135deg, #f97316 0%, #ea580c 100%); }
    &.red { background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%); }
  }
  
  .stat-value { font-size: 28px; font-weight: 700; }
  .stat-label { color: #64748b; font-size: 14px; }
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  font-weight: 600;
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .card-link { font-size: 14px; color: #3b82f6; text-decoration: none; }
}

.card-body { padding: 20px; }

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 12px;
  
  &:last-child { margin-bottom: 0; }
  
  h6 { margin: 0 0 4px; font-size: 14px; }
  p { margin: 0 0 8px; font-size: 13px; color: #64748b; }
  
  .task-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 12px;
    color: #94a3b8;
    
    i { margin-right: 4px; }
  }
  
  .badge {
    padding: 4px 8px;
    border-radius: 4px;
    font-size: 11px;
    
    &.badge-medium { background: #fef3c7; color: #d97706; }
    &.badge-high { background: #fee2e2; color: #dc2626; }
  }
}

.btn {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  border: none;
  cursor: pointer;
  
  &.btn-sm { padding: 4px 10px; font-size: 12px; }
  &.btn-primary { background: #3b82f6; color: #fff; }
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.status-item {
  text-align: center;
  padding: 20px;
  border-radius: 12px;
  
  i { font-size: 32px; margin-bottom: 8px; }
  .status-value { font-size: 24px; font-weight: 700; }
  .status-label { font-size: 13px; color: #64748b; }
  
  &.normal { background: #f0fdf4; i { color: #22c55e; } }
  &.warning { background: #fffbeb; i { color: #f59e0b; } }
  &.error { background: #fef2f2; i { color: #ef4444; } }
}

@media (max-width: 1200px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 1fr; }
}
</style>
