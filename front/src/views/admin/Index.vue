<template>
  <div class="admin-home">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><i class="bi bi-people"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalUsers }}</div>
          <div class="stat-label">系统用户</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="bi bi-person-check"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.activeUsers }}</div>
          <div class="stat-label">活跃用户</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><i class="bi bi-gear"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalEquipments }}</div>
          <div class="stat-label">设备总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><i class="bi bi-journal-text"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayLogs }}</div>
          <div class="stat-label">今日操作</div>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <!-- 用户分布 -->
      <div class="card">
        <div class="card-header">用户角色分布</div>
        <div class="card-body">
          <div class="role-list">
            <div class="role-item">
              <span class="role-tag admin">管理员</span>
              <span class="role-count">1</span>
            </div>
            <div class="role-item">
              <span class="role-tag dispatcher">调度员</span>
              <span class="role-count">2</span>
            </div>
            <div class="role-item">
              <span class="role-tag inspector">巡检员</span>
              <span class="role-count">3</span>
            </div>
            <div class="role-item">
              <span class="role-tag manager">经理</span>
              <span class="role-count">1</span>
            </div>
            <div class="role-item">
              <span class="role-tag workshop">车间用户</span>
              <span class="role-count">4</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 最近操作 -->
      <div class="card">
        <div class="card-header">
          <span>最近操作日志</span>
          <router-link to="/admin/logs" class="card-link">查看全部 →</router-link>
        </div>
        <div class="card-body p-0">
          <table class="data-table">
            <thead>
              <tr>
                <th>操作人</th>
                <th>操作类型</th>
                <th>操作内容</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>张管理</td>
                <td><span class="log-type info">登录</span></td>
                <td>系统登录</td>
                <td>10分钟前</td>
              </tr>
              <tr>
                <td>李调度</td>
                <td><span class="log-type success">审批</span></td>
                <td>批准用电申请 APP202501050001</td>
                <td>30分钟前</td>
              </tr>
              <tr>
                <td>赵巡检</td>
                <td><span class="log-type warning">巡检</span></td>
                <td>完成设备巡检任务</td>
                <td>1小时前</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 系统状态 -->
      <div class="card">
        <div class="card-header">系统状态</div>
        <div class="card-body">
          <div class="system-status">
            <div class="status-item">
              <i class="bi bi-database-fill-check text-success"></i>
              <div class="status-info">
                <span class="status-name">数据库</span>
                <span class="status-value text-success">正常运行</span>
              </div>
            </div>
            <div class="status-item">
              <i class="bi bi-hdd-fill text-success"></i>
              <div class="status-info">
                <span class="status-name">服务器</span>
                <span class="status-value text-success">正常运行</span>
              </div>
            </div>
            <div class="status-item">
              <i class="bi bi-cloud-fill text-success"></i>
              <div class="status-info">
                <span class="status-name">API服务</span>
                <span class="status-value text-success">正常运行</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="card">
        <div class="card-header">快捷操作</div>
        <div class="card-body">
          <div class="action-grid">
            <router-link to="/admin/users" class="quick-action">
              <div class="action-icon blue"><i class="bi bi-person-plus"></i></div>
              <span>添加用户</span>
            </router-link>
            <router-link to="/admin/config" class="quick-action">
              <div class="action-icon green"><i class="bi bi-gear"></i></div>
              <span>系统配置</span>
            </router-link>
            <router-link to="/admin/logs" class="quick-action">
              <div class="action-icon orange"><i class="bi bi-journal-text"></i></div>
              <span>查看日志</span>
            </router-link>
            <a href="#" class="quick-action">
              <div class="action-icon purple"><i class="bi bi-download"></i></div>
              <span>数据备份</span>
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const stats = ref({
  totalUsers: 11,
  activeUsers: 11,
  totalEquipments: 18,
  todayLogs: 156
})
</script>

<style lang="scss" scoped>
.admin-home { color: #1e293b; }

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
    &.purple { background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%); }
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

.card-body { padding: 20px; &.p-0 { padding: 0; } }

.role-list {
  .role-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #e2e8f0;
    
    &:last-child { border-bottom: none; }
  }
  
  .role-tag {
    padding: 4px 10px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 500;
    
    &.admin { background: #f3e8ff; color: #7c3aed; }
    &.manager { background: #dbeafe; color: #2563eb; }
    &.dispatcher { background: #fef3c7; color: #d97706; }
    &.inspector { background: #dcfce7; color: #16a34a; }
    &.workshop { background: #f1f5f9; color: #475569; }
  }
  
  .role-count { font-weight: 600; color: #3b82f6; }
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  
  th, td { padding: 12px 16px; text-align: left; border-bottom: 1px solid #e2e8f0; }
  th { background: #f8fafc; font-weight: 600; color: #475569; font-size: 13px; }
  
  .log-type {
    padding: 4px 10px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 500;
    
    &.info { background: #dbeafe; color: #2563eb; }
    &.success { background: #dcfce7; color: #16a34a; }
    &.warning { background: #fef3c7; color: #d97706; }
  }
}

.system-status {
  .status-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #e2e8f0;
    
    &:last-child { border-bottom: none; }
    
    > i { font-size: 24px; }
    
    .status-info {
      display: flex;
      flex-direction: column;
      
      .status-name { font-size: 14px; }
      .status-value { font-size: 12px; }
    }
  }
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.quick-action {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  background: #f8fafc;
  text-decoration: none;
  color: #1e293b;
  transition: all 0.3s;
  
  &:hover { background: #e0e7ff; transform: translateY(-4px); }
  
  .action-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    margin-bottom: 8px;
    
    &.blue { background: #dbeafe; color: #3b82f6; }
    &.green { background: #dcfce7; color: #22c55e; }
    &.orange { background: #fef3c7; color: #f97316; }
    &.purple { background: #f3e8ff; color: #8b5cf6; }
  }
  
  span { font-size: 14px; }
}

.text-success { color: #22c55e !important; }

@media (max-width: 1200px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .content-grid { grid-template-columns: 1fr; }
  .action-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
