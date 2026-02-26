<template>
  <div class="message-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h4>消息中心</h4>
      <el-button type="primary" link @click="handleMarkAllRead" :disabled="unreadCount === 0">
        <i class="bi bi-check-all me-1"></i>全部已读
      </el-button>
    </div>

    <!-- 消息分类标签 -->
    <el-tabs v-model="activeTab" class="message-tabs" @tab-change="handleTabChange">
      <el-tab-pane name="all">
        <template #label>
          <span>全部消息</span>
          <el-badge v-if="unreadCount > 0" :value="unreadCount" class="ms-2" />
        </template>
      </el-tab-pane>
      <el-tab-pane label="审批通知" name="approval" />
      <el-tab-pane label="报表通知" name="report" />
      <el-tab-pane label="预警提醒" name="alert" />
      <el-tab-pane label="系统消息" name="system" />
    </el-tabs>

    <!-- 消息列表 -->
    <div class="message-list" v-loading="loading">
      <div v-for="msg in filteredMessages" :key="msg.id" 
           :class="['message-item', { unread: !msg.read, urgent: msg.priority === 'urgent' }]"
           @click="handleClick(msg)">
        <div class="message-icon" :class="msg.iconClass">
          <i :class="msg.icon"></i>
        </div>
        <div class="message-content">
          <div class="message-header">
            <h6 class="message-title">{{ msg.title }}</h6>
            <el-tag v-if="!msg.read" type="danger" size="small">未读</el-tag>
            <el-tag v-else type="info" size="small">已读</el-tag>
          </div>
          <p class="message-desc">{{ msg.content }}</p>
          <div class="message-footer">
            <span class="message-time"><i class="bi bi-clock me-1"></i>{{ msg.time }}</span>
            <el-button v-if="msg.actionText" type="primary" size="small" @click.stop="handleAction(msg)">
              {{ msg.actionText }}
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="!loading && filteredMessages.length === 0" description="暂无消息" />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadMessages"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyNotifications, markRead, markAllRead } from '@/api/notification'

const router = useRouter()
const activeTab = ref('all')
const loading = ref(false)
const messages = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 加载消息列表
async function loadMessages() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    // 根据标签筛选消息类型
    if (activeTab.value !== 'all') {
      params.type = activeTab.value
    }
    const res = await getMyNotifications(params)
    if (res.code === 200 && res.data) {
      const records = res.data.records || res.data || []
      messages.value = records.map(formatMessage)
      total.value = res.data.total || records.length
    }
  } catch (e) {
    console.error('加载消息失败', e)
    ElMessage.error('加载消息失败')
  } finally {
    loading.value = false
  }
}

// 格式化消息数据
function formatMessage(msg) {
  const typeConfig = getTypeConfig(msg.type)
  return {
    id: msg.id,
    type: msg.type,
    category: msg.type,
    title: msg.title,
    content: msg.content,
    time: formatTime(msg.createdAt),
    read: msg.isRead,
    relatedId: msg.relatedId,
    priority: getPriority(msg),
    actionText: getActionText(msg.type),
    ...typeConfig
  }
}

// 获取消息类型配置
function getTypeConfig(type) {
  const configMap = {
    approval: { icon: 'bi bi-check-circle', iconClass: 'success' },
    report: { icon: 'bi bi-file-earmark-bar-graph', iconClass: 'primary' },
    alert: { icon: 'bi bi-exclamation-triangle', iconClass: 'warning' },
    system: { icon: 'bi bi-info-circle', iconClass: 'info' },
    task: { icon: 'bi bi-list-task', iconClass: 'primary' }
  }
  return configMap[type] || configMap.system
}

// 获取优先级
function getPriority(msg) {
  if (msg.type === 'alert' || (msg.title && msg.title.includes('紧急'))) {
    return 'urgent'
  }
  return 'normal'
}

// 获取操作按钮文本
function getActionText(type) {
  const textMap = {
    approval: '查看详情',
    report: '查看报表',
    alert: '查看预警',
    system: null,
    task: '查看详情'
  }
  return textMap[type] || null
}

// 格式化时间
function formatTime(time) {
  if (!time) return ''
  const date = new Date(time.replace ? time.replace('T', ' ') : time)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  const timeStr = typeof time === 'string' ? time : time.toISOString()
  return timeStr.substring(0, 16).replace('T', ' ')
}

// 计算属性
const filteredMessages = computed(() => messages.value)

const unreadCount = computed(() => messages.value.filter(m => !m.read).length)

// 标签切换
function handleTabChange() {
  currentPage.value = 1
  loadMessages()
}

// 点击消息
async function handleClick(msg) {
  if (!msg.read) {
    try {
      const res = await markRead(msg.id)
      if (res.code === 200) {
        msg.read = true
      }
    } catch (e) {
      console.error('标记已读失败', e)
    }
  }
}

// 操作按钮点击
async function handleAction(msg) {
  await handleClick(msg)
  // 根据消息类型跳转到管理层相关页面
  const routeMap = {
    approval: '/manager/users',
    report: '/manager/analysis',
    alert: '/manager/analysis',
    task: '/manager'
  }
  const route = routeMap[msg.type]
  if (route) router.push(route)
}

// 全部已读
async function handleMarkAllRead() {
  try {
    const res = await markAllRead()
    if (res.code === 200) {
      messages.value.forEach(m => m.read = true)
      ElMessage.success('已全部标记为已读')
    }
  } catch (e) {
    console.error('全部已读失败', e)
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadMessages()
})
</script>

<style lang="scss" scoped>
.message-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  h4 {
    margin: 0;
    font-weight: 600;
    color: #1e293b;
  }
}

.message-tabs {
  margin-bottom: 20px;
}

.message-list {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  min-height: 200px;
}

.message-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f8fafc;
  }

  &.unread {
    background: #f0f9ff;

    .message-title {
      font-weight: 600;
    }
  }

  &.urgent {
    background: linear-gradient(135deg, #fef2f2 0%, #fff 100%);
    border-left: 4px solid #ef4444;
  }
}

.message-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;

  &.success {
    background: #dcfce7;
    color: #22c55e;
  }

  &.warning {
    background: #fef3c7;
    color: #f59e0b;
  }

  &.danger {
    background: #fee2e2;
    color: #ef4444;
  }

  &.info {
    background: #dbeafe;
    color: #3b82f6;
  }

  &.primary {
    background: #e0e7ff;
    color: #6366f1;
  }
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.message-title {
  margin: 0;
  font-size: 15px;
  color: #1e293b;
}

.message-desc {
  margin: 0 0 12px;
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-time {
  font-size: 13px;
  color: #94a3b8;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

@media (max-width: 768px) {
  .message-page {
    padding: 16px;
  }

  .message-item {
    padding: 16px;
  }

  .message-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }
}
</style>
