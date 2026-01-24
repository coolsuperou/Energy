<template>
  <div class="content-wrapper">
    <!-- 标签页 -->
    <ul class="nav nav-tabs mb-4">
      <li class="nav-item">
        <button class="nav-link" :class="{ active: activeTab === 'processing' }" @click="activeTab = 'processing'">
          进行中 ({{ processingTasks.length }})
        </button>
      </li>
      <li class="nav-item">
        <button class="nav-link" :class="{ active: activeTab === 'completed' }" @click="activeTab = 'completed'">
          已完成
        </button>
      </li>
      <li class="nav-item">
        <button class="nav-link" :class="{ active: activeTab === 'all' }" @click="activeTab = 'all'">
          全部工单
        </button>
      </li>
    </ul>

    <div class="tab-content">
      <!-- 进行中标签页 -->
      <div class="tab-pane fade" :class="{ 'show active': activeTab === 'processing' }">
        <div class="row g-4">
          <div v-if="processingTasks.length === 0" class="col-12">
            <div class="card">
              <div class="card-body text-center text-muted py-5">
                <i class="bi bi-inbox fs-1 d-block mb-2"></i>
                <span>暂无进行中的工单</span>
              </div>
            </div>
          </div>
          <div v-else v-for="task in processingTasks" :key="task.id" class="col-12">
            <div class="order-card" :style="{ borderLeftColor: getTaskColor(task.status) }">
              <div class="row align-items-center">
                <div class="col-auto">
                  <div class="rounded-circle bg-opacity-10 p-3" :class="getTaskBgClass(task.status)">
                    <i :class="[getTaskIcon(task.taskType), getTaskIconColor(task.status), 'fs-4']"></i>
                  </div>
                </div>
                <div class="col">
                  <div class="d-flex align-items-center gap-2 mb-2">
                    <h6 class="mb-0">{{ task.title }}</h6>
                    <span class="badge-status" :class="getStatusClass(task.status)">{{ getStatusText(task.status) }}</span>
                  </div>
                  <div class="row text-muted small">
                    <div class="col-auto"><i class="bi bi-geo-alt me-1"></i>{{ task.description || '位置信息' }}</div>
                    <div class="col-auto"><i class="bi bi-clock me-1"></i>接单于 {{ formatTime(task.createdAt) }}</div>
                    <div v-if="task.status === 'IN_PROGRESS'" class="col-auto">
                      <i class="bi bi-hourglass-split me-1"></i>已用时 {{ calculateDuration(task.createdAt) }}
                    </div>
                  </div>
                </div>
                <div class="col-auto">
                  <div class="d-flex gap-2">
                    <button v-if="task.status === 'IN_PROGRESS'" class="btn btn-primary btn-sm" @click="handleContinue(task)">
                      继续处理
                    </button>
                    <button v-else class="btn btn-primary btn-sm" @click="handleStart(task)">
                      开始处理
                    </button>
                    <button class="btn btn-success btn-sm" @click="handleComplete(task)">
                      <i class="bi bi-check-lg me-1"></i>完成工单
                    </button>
                    <button class="btn btn-outline-secondary btn-sm" @click="handleView(task)">查看详情</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 已完成标签页 -->
      <div class="tab-pane fade" :class="{ 'show active': activeTab === 'completed' }">
        <div class="card">
          <div class="table-responsive">
            <table class="table table-hover mb-0">
              <thead>
                <tr>
                  <th>工单号</th>
                  <th>类型</th>
                  <th>位置</th>
                  <th>完成时间</th>
                  <th>用时</th>
                  <th>评分</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="completedTasks.length === 0">
                  <td colspan="7" class="text-center text-muted py-4">暂无已完成的工单</td>
                </tr>
                <tr v-else v-for="task in completedTasks" :key="task.id">
                  <td class="fw-semibold">WO{{ String(task.id).padStart(10, '0') }}</td>
                  <td><span class="badge" :class="getTypeBadge(task.taskType)">{{ getTypeText(task.taskType) }}</span></td>
                  <td>{{ task.description || '-' }}</td>
                  <td>{{ formatDateTime(task.completedAt) }}</td>
                  <td>{{ calculateTaskDuration(task.createdAt, task.completedAt) }}</td>
                  <td><i class="bi bi-star-fill text-warning"></i> {{ task.rating || '5.0' }}</td>
                  <td><button class="btn btn-sm btn-outline-primary" @click="handleView(task)">查看</button></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- 全部工单标签页 -->
      <div class="tab-pane fade" :class="{ 'show active': activeTab === 'all' }">
        <div class="card">
          <div class="card-body text-center text-muted py-5">
            <i class="bi bi-card-checklist fs-1 d-block mb-2"></i>
            <span>全部工单列表</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyTasks, completeTask as completeTaskApi } from '@/api/task'

const activeTab = ref('processing')
const allTasks = ref([])

const processingTasks = computed(() => {
  return allTasks.value.filter(t => 
    t.status === 'ASSIGNED' || t.status === 'IN_PROGRESS'
  )
})

const completedTasks = computed(() => {
  return allTasks.value.filter(t => t.status === 'COMPLETED')
})

async function loadTasks() {
  try {
    const res = await getMyTasks({})
    if (res && res.code === 200 && res.data) {
      allTasks.value = res.data.records || res.data || []
    }
  } catch (e) {
    console.error('加载任务失败', e)
    ElMessage.error('加载任务失败')
  }
}

function getTaskColor(status) {
  return status === 'IN_PROGRESS' ? '#3b82f6' : '#f59e0b'
}

function getTaskBgClass(status) {
  return status === 'IN_PROGRESS' ? 'bg-primary' : 'bg-warning'
}

function getTaskIconColor(status) {
  return status === 'IN_PROGRESS' ? 'text-primary' : 'text-warning'
}

function getTaskIcon(type) {
  const map = {
    REPAIR: 'bi bi-tools',
    INSPECTION: 'bi bi-search',
    MAINTENANCE: 'bi bi-gear'
  }
  return map[type] || 'bi bi-tools'
}

function getStatusClass(status) {
  const map = {
    'IN_PROGRESS': 'badge-processing',
    'ASSIGNED': 'badge-pending',
    'COMPLETED': 'badge-approved'
  }
  return map[status] || 'badge-pending'
}

function getTypeText(type) {
  const map = {
    REPAIR: '故障维修',
    INSPECTION: '设备巡检',
    MAINTENANCE: '预防维护'
  }
  return map[type] || type
}

function getTypeBadge(type) {
  const map = {
    REPAIR: 'bg-danger',
    INSPECTION: 'bg-success',
    MAINTENANCE: 'bg-info'
  }
  return map[type] || 'bg-secondary'
}

function handleStart(task) {
  ElMessage.info('开始处理任务')
}

function handleContinue(task) {
  ElMessage.info('继续处理任务')
}

async function handleComplete(task) {
  try {
    await ElMessageBox.confirm('确认完成此工单？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })
    const res = await completeTaskApi(task.id, '任务已完成')
    if (res && res.code === 200) {
      ElMessage.success('工单已完成')
      loadTasks()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('完成任务失败', e)
      ElMessage.error('操作失败')
    }
  }
}

function handleView(task) {
  ElMessage.info('查看详情功能待实现')
}

function getStatusText(status) {
  const map = {
    ASSIGNED: '待处理',
    IN_PROGRESS: '处理中',
    COMPLETED: '已完成',
    PENDING: '待派单'
  }
  return map[status] || status
}

// 格式化时间
function formatTime(time) {
  if (!time) return ''
  return time.substring(11, 16)
}

// 格式化日期时间
function formatDateTime(time) {
  if (!time) return ''
  return time.substring(0, 16).replace('T', ' ')
}

// 计算任务用时
function calculateDuration(startTime) {
  if (!startTime) return ''
  const start = new Date(startTime)
  const now = new Date()
  const diff = Math.floor((now - start) / 1000 / 60)
  
  if (diff < 60) return `${diff}分钟`
  const hours = Math.floor(diff / 60)
  const mins = diff % 60
  return `${hours}小时${mins}分钟`
}

// 计算任务总用时
function calculateTaskDuration(startTime, endTime) {
  if (!startTime || !endTime) return '-'
  const start = new Date(startTime)
  const end = new Date(endTime)
  const diff = Math.floor((end - start) / 1000 / 60)
  
  if (diff < 60) return `${diff}分钟`
  const hours = Math.floor(diff / 60)
  const mins = diff % 60
  return `${hours}小时${mins}分钟`
}

onMounted(() => {
  loadTasks()
})
</script>

<style lang="scss" scoped>
.my-tasks-page {
  .empty-state {
    text-align: center;
    padding: 60px 20px;
    color: #999;
    
    i {
      font-size: 64px;
      margin-bottom: 16px;
      display: block;
    }
  }

  .task-cards {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .task-item {
    .task-icon-wrapper {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      
      i {
        font-size: 24px;
      }
    }

    .task-meta {
      display: flex;
      gap: 16px;
      color: #666;
      font-size: 14px;
      margin-bottom: 16px;
      
      i {
        margin-right: 4px;
      }
    }

    .task-actions {
      display: flex;
      gap: 8px;
    }
  }

  .task-detail {
    .detail-row {
      display: flex;
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;
      
      &:last-child {
        border-bottom: none;
      }
      
      .detail-label {
        width: 100px;
        color: #666;
        flex-shrink: 0;
      }
    }
  }
}
</style>
