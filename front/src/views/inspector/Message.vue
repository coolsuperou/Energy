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
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const activeTab = ref('all')

const messages = ref([
  {
    id: 1,
    type: 'urgent',
    priority: 'urgent',
    category: 'order',
    title: '【紧急】新工单分配',
    content: '生产二车间配电柜异常发热，需要紧急处理。当前温度65°C，已超过警戒值。',
    time: '5分钟前',
    read: false,
    action: true,
    actionText: '立即查看'
  },
  {
    id: 2,
    type: 'reminder',
    priority: 'normal',
    category: 'order',
    title: '巡检任务提醒',
    content: '装配车间巡检任务将于14:00开始，请提前做好准备。检查点共10项。',
    time: '30分钟前',
    read: false,
    action: false
  },
  {
    id: 3,
    type: 'chat',
    priority: 'normal',
    category: 'team',
    title: '李四（能源调度员）',
    content: '王工，生产二车间的工单处理得怎么样了？车间那边比较着急。',
    time: '1小时前',
    read: false,
    action: false
  },
  {
    id: 4,
    type: 'success',
    priority: 'normal',
    category: 'order',
    title: '工单完成确认',
    content: '您处理的工单 WO202401140002 已确认完成，获得5.0分好评！',
    time: '昨天 16:30',
    read: true,
    action: false
  },
  {
    id: 5,
    type: 'info',
    priority: 'normal',
    category: 'system',
    title: '排班变更通知',
    content: '您本周三的班次已调整为夜班（17:00-02:00），请注意查看。',
    time: '昨天 09:00',
    read: true,
    action: false
  },
  {
    id: 6,
    type: 'info',
    priority: 'normal',
    category: 'system',
    title: '系统维护通知',
    content: '系统将于本周六凌晨2:00-4:00进行维护升级，期间可能无法访问。',
    time: '2天前',
    read: true,
    action: false
  }
])

const unreadCount = computed(() => {
  return messages.value.filter(m => !m.read).length
})

const filteredMessages = computed(() => {
  if (activeTab.value === 'all') {
    return messages.value
  }
  return messages.value.filter(m => m.category === activeTab.value)
})

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

function markAsRead(msg) {
  if (!msg.read) {
    msg.read = true
    ElMessage.success('消息已标记为已读')
  }
}

function handleAction(msg) {
  if (msg.category === 'order') {
    router.push('/inspector/equipment')
    ElMessage.info('跳转到我的工单')
  }
}
</script>

<style lang="scss" scoped>
.opacity-75 {
  opacity: 0.75;
}
</style>
