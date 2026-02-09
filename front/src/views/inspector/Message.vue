<template>
  <div class="content-wrapper">
    <!-- 消息分类标签 -->
    <ul class="nav nav-tabs mb-4">
      <li class="nav-item">
        <button class="nav-link" :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">
          全部消息 <span v-if="unreadCount > 0" class="badge bg-danger">{{ unreadCount }}</span>
        </button>
      </li>
      <li class="nav-item">
        <button class="nav-link" :class="{ active: activeTab === 'order' }" @click="activeTab = 'order'">
          工单通知
        </button>
      </li>
      <li class="nav-item">
        <button class="nav-link" :class="{ active: activeTab === 'system' }" @click="activeTab = 'system'">
          系统消息
        </button>
      </li>
      <li class="nav-item">
        <button class="nav-link" :class="{ active: activeTab === 'team' }" @click="activeTab = 'team'">
          团队协作
        </button>
      </li>
    </ul>

    <div class="tab-content">
      <div class="tab-pane fade" :class="{ 'show active': activeTab === 'all' }">
        <div class="card">
          <div class="card-body p-0">
            <div v-for="msg in filteredMessages" :key="msg.id" 
                 class="p-3 border-bottom" 
                 :class="{ 'bg-danger bg-opacity-10': msg.priority === 'urgent', 'opacity-75': msg.read }"
                 style="cursor: pointer;"
                 @click="markAsRead(msg)">
              <div class="d-flex gap-3">
                <div class="rounded-circle bg-opacity-25 p-2 d-flex align-items-center justify-content-center" 
                     style="width: 48px; height: 48px;"
                     :class="getIconBgClass(msg.type)">
                  <i :class="getIcon(msg.type)" class="fs-5"></i>
                </div>
                <div class="flex-grow-1">
                  <div class="d-flex justify-content-between align-items-start">
                    <h6 class="mb-1" :class="{ 'fw-semibold': !msg.read }">{{ msg.title }}</h6>
                    <span class="badge" :class="msg.read ? 'bg-secondary' : 'bg-danger'">
                      {{ msg.read ? '已读' : '未读' }}
                    </span>
                  </div>
                  <p class="text-muted mb-1 small">{{ msg.content }}</p>
                  <div class="d-flex justify-content-between align-items-center">
                    <small class="text-muted">
                      <i class="bi bi-clock me-1"></i>{{ msg.time }}
                    </small>
                    <button v-if="msg.action" class="btn btn-sm" :class="getActionBtnClass(msg.priority)" @click.stop="handleAction(msg)">
                      {{ msg.actionText }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
            
            <div v-if="filteredMessages.length === 0" class="p-5 text-center text-muted">
              <i class="bi bi-inbox fs-1 d-block mb-2"></i>
              <span>暂无消息</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getMyNotifications, markRead, getUnreadCount } from '@/api/notification'

const router = useRouter()
const activeTab = ref('all')
const messages = ref([])
const loading = ref(false)

const unreadCount = computed(() => {
  return messages.value.filter(m => !m.read).length
})

const filteredMessages = computed(() => {
  if (activeTab.value === 'all') {
    return messages.value
  }
  return messages.value.filter(m => m.category === activeTab.value)
})

// 加载消息列表
async function loadMessages() {
  loading.value = true
  try {
    const res = await getMyNotifications({ pageSize: 50 })
    if (res && res.code === 200 && res.data) {
      messages.value = (res.data.records || res.data || []).map(item => ({
        id: item.id,
        type: getMessageType(item.type),
        priority: item.priority === 'HIGH' ? 'urgent' : 'normal',
        category: getCategoryFromType(item.type),
        title: item.title,
        content: item.content || item.message,
        time: formatTime(item.createdAt),
        read: item.read || false,
        action: item.type === 'TASK_ASSIGNED' || item.type === 'URGENT_TASK',
        actionText: '立即查看'
      }))
    }
  } catch (e) {
    console.error('加载消息失败', e)
    ElMessage.error('加载消息失败')
  }
  loading.value = false
}

// 获取消息类型
function getMessageType(type) {
  const map = {
    URGENT_TASK: 'urgent',
    TASK_ASSIGNED: 'urgent',
    INSPECTION_REMINDER: 'reminder',
    TEAM_MESSAGE: 'chat',
    TASK_COMPLETED: 'success',
    SYSTEM_NOTICE: 'info'
  }
  return map[type] || 'info'
}

// 获取分类
function getCategoryFromType(type) {
  if (type.includes('TASK') || type.includes('INSPECTION')) return 'order'
  if (type.includes('TEAM') || type.includes('MESSAGE')) return 'team'
  return 'system'
}

// 格式化时间
function formatTime(timestamp) {
  if (!timestamp) return ''
  const now = new Date()
  const time = new Date(timestamp)
  const diff = Math.floor((now - time) / 1000)
  
  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)}分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)}小时前`
  if (diff < 172800) return '昨天 ' + time.toTimeString().substring(0, 5)
  if (diff < 604800) return `${Math.floor(diff / 86400)}天前`
  return time.toLocaleDateString()
}

function getIcon(type) {
  const map = {
    urgent: 'bi bi-exclamation-triangle text-danger',
    reminder: 'bi bi-clock text-warning',
    chat: 'bi bi-chat-dots text-primary',
    success: 'bi bi-check-circle text-success',
    info: 'bi bi-info-circle text-info'
  }
  return map[type] || 'bi bi-bell text-secondary'
}

function getIconBgClass(type) {
  const map = {
    urgent: 'bg-danger',
    reminder: 'bg-warning',
    chat: 'bg-primary',
    success: 'bg-success',
    info: 'bg-info'
  }
  return map[type] || 'bg-secondary'
}

function getActionBtnClass(priority) {
  return priority === 'urgent' ? 'btn-danger' : 'btn-primary'
}

async function markAsRead(msg) {
  if (!msg.read) {
    try {
      const res = await markRead(msg.id)
      if (res && res.code === 200) {
        msg.read = true
        ElMessage.success('消息已标记为已读')
      }
    } catch (e) {
      console.error('标记已读失败', e)
    }
  }
}

function handleAction(msg) {
  if (msg.category === 'order') {
    router.push('/inspector/equipment')
    ElMessage.info('跳转到我的工单')
  }
}

onMounted(() => {
  loadMessages()
})
</script>

<style lang="scss" scoped>
.opacity-75 {
  opacity: 0.75;
}
</style>
