<template>
  <div class="admin-home">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><i class="bi bi-people"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ userStats.totalUsers }}</div>
          <div class="stat-label">系统用户</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="bi bi-person-check"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ userStats.activeUsers }}</div>
          <div class="stat-label">活跃用户</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><i class="bi bi-activity"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ todayActiveUsers }}</div>
          <div class="stat-label">今日活跃</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><i class="bi bi-person-plus"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ userStats.newUsersThisMonth }}</div>
          <div class="stat-label">本月新增</div>
        </div>
      </div>
    </div>

    <div class="content-grid">
      <!-- 用户角色分布 -->
      <div class="card">
        <div class="card-header">用户角色分布</div>
        <div class="card-body">
          <div class="role-list">
            <div class="role-item" v-for="item in roleDistribution" :key="item.role">
              <span class="role-tag" :class="item.role">{{ item.roleName }}</span>
              <span class="role-count">{{ item.count }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 系统状态 -->
      <div class="card">
        <div class="card-header">
          <span>系统状态</span>
          <span class="uptime-badge">运行时间: {{ systemStatus.uptime }}</span>
        </div>
        <div class="card-body">
          <div class="system-status">
            <div class="status-item">
              <i :class="getStatusIcon(systemStatus.database?.status)"></i>
              <div class="status-info">
                <span class="status-name">数据库</span>
                <span class="status-value" :class="getStatusClass(systemStatus.database?.status)">
                  {{ systemStatus.database?.message || '检测中...' }}
                </span>
              </div>
            </div>
            <div class="status-item">
              <i :class="getStatusIcon(systemStatus.minio?.status)"></i>
              <div class="status-info">
                <span class="status-name">MinIO存储</span>
                <span class="status-value" :class="getStatusClass(systemStatus.minio?.status)">
                  {{ systemStatus.minio?.message || '检测中...' }}
                </span>
              </div>
            </div>
            <div class="status-item">
              <i :class="getStatusIcon(systemStatus.api?.status)"></i>
              <div class="status-info">
                <span class="status-name">API服务</span>
                <span class="status-value" :class="getStatusClass(systemStatus.api?.status)">
                  {{ systemStatus.api?.message || '检测中...' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 最近操作日志 -->
      <div class="card">
        <div class="card-header">
          <span>最近操作日志</span>
          <router-link to="/admin/logs" class="card-link">查看全部 →</router-link>
        </div>
        <div class="card-body p-0">
          <table class="data-table" v-if="recentLogs.length">
            <thead>
              <tr>
                <th>操作人</th>
                <th>操作类型</th>
                <th>操作内容</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in recentLogs" :key="log.id">
                <td>{{ log.userName }}</td>
                <td><span class="log-type" :class="getLogTypeClass(log.operationType)">{{ log.operationType }}</span></td>
                <td>{{ log.operationDesc }}</td>
                <td>{{ log.relativeTime }}</td>
              </tr>
            </tbody>
          </table>
          <div class="empty-tip" v-else>
            <i class="bi bi-journal-text"></i>
            <span>暂无操作日志</span>
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
              <span>用户管理</span>
            </router-link>
            <router-link to="/admin/config" class="quick-action">
              <div class="action-icon green"><i class="bi bi-gear"></i></div>
              <span>系统配置</span>
            </router-link>
            <router-link to="/admin/logs" class="quick-action">
              <div class="action-icon orange"><i class="bi bi-journal-text"></i></div>
              <span>查看日志</span>
            </router-link>
            <router-link to="/admin/profile" class="quick-action">
              <div class="action-icon purple"><i class="bi bi-person-circle"></i></div>
              <span>个人中心</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminDashboard } from '@/api/admin'

const userStats = ref({
  totalUsers: 0,
  activeUsers: 0,
  disabledUsers: 0,
  newUsersThisMonth: 0
})

const roleDistribution = ref([])
const todayActiveUsers = ref(0)
const systemStatus = ref({
  database: null,
  minio: null,
  api: null,
  uptime: '加载中...'
})
const recentLogs = ref([])

// 获取状态图标
function getStatusIcon(status) {
  const icons = {
    normal: 'bi bi-check-circle-fill text-success',
    warning: 'bi bi-exclamation-triangle-fill text-warning',
    error: 'bi bi-x-circle-fill text-danger'
  }
  return icons[status] || 'bi bi-question-circle-fill text-secondary'
}

// 获取状态样式类
function getStatusClass(status) {
  const classes = {
    normal: 'text-success',
    warning: 'text-warning',
    error: 'text-danger'
  }
  return classes[status] || 'text-secondary'
}

// 获取日志类型样式
function getLogTypeClass(type) {
  const typeMap = {
    '登录': 'info',
    '登出': 'info',
    '审批': 'success',
    '创建': 'success',
    '修改': 'warning',
    '删除': 'warning',
    '巡检': 'success'
  }
  return typeMap[type] || 'info'
}

// 加载数据
async function loadData() {
  try {
    const res = await getAdminDashboard()
    if (res.code === 200 && res.data) {
      // 用户统计
      if (res.data.userStats) {
        userStats.value = res.data.userStats
      }
      
      // 角色分布
      if (res.data.roleDistribution) {
        roleDistribution.value = res.data.roleDistribution
      }
      
      // 今日活跃用户
      if (res.data.todayActiveUsers !== undefined) {
        todayActiveUsers.value = res.data.todayActiveUsers
      }
      
      // 系统状态
      if (res.data.systemStatus) {
        systemStatus.value = res.data.systemStatus
      }
      
      // 最近日志
      if (res.data.recentLogs) {
        recentLogs.value = res.data.recentLogs
      }
    }
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss">
@use '@/styles/admin.scss';

.admin-home {
  .uptime-badge {
    font-size: 12px;
    color: #64748b;
    background: #f1f5f9;
    padding: 4px 10px;
    border-radius: 6px;
  }
  
  .empty-tip {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px;
    color: #94a3b8;
    
    i {
      font-size: 32px;
      margin-bottom: 8px;
    }
  }
  
  .text-warning {
    color: #f59e0b !important;
  }
  
  .text-danger {
    color: #ef4444 !important;
  }
  
  .text-secondary {
    color: #94a3b8 !important;
  }
}
</style>
