<template>
  <div class="content-wrapper my-tasks-page">
    <!-- 页面标题 -->
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h4 class="mb-0">我的任务</h4>
      <div class="task-summary">
        <span class="badge bg-primary me-2">进行中: {{ processingTasks.length }}</span>
        <span class="badge bg-success">已完成: {{ completedTasks.length }}</span>
      </div>
    </div>

    <!-- 标签页 -->
    <ul class="nav nav-tabs mb-4">
      <li class="nav-item">
        <button class="nav-link" :class="{ active: activeTab === 'processing' }" @click="activeTab = 'processing'">
          <i class="bi bi-hourglass-split me-1"></i>进行中 ({{ processingTasks.length }})
        </button>
      </li>
      <li class="nav-item">
        <button class="nav-link" :class="{ active: activeTab === 'completed' }" @click="activeTab = 'completed'">
          <i class="bi bi-check-circle me-1"></i>已完成 ({{ completedTasks.length }})
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
            <div class="order-card" :style="{ borderLeftColor: getPriorityColor(task.priority) }">
              <div class="row align-items-center">
                <div class="col-auto">
                  <div class="rounded-circle bg-opacity-10 p-3" :class="getTaskBgClass(task.taskType)">
                    <i :class="[getTaskIcon(task.taskType), getTaskIconColor(task.taskType), 'fs-4']"></i>
                  </div>
                </div>
                <div class="col">
                  <div class="d-flex align-items-center gap-2 mb-2">
                    <h6 class="mb-0">{{ task.title }}</h6>
                    <span class="badge" :class="getTypeBadge(task.taskType)">{{ getTypeText(task.taskType) }}</span>
                    <span class="badge" :class="getPriorityBadge(task.priority)">{{ getPriorityText(task.priority) }}</span>
                  </div>
                  <div class="row text-muted small">
                    <div class="col-auto" v-if="task.equipmentName">
                      <i class="bi bi-gear me-1"></i>{{ task.equipmentName }}
                    </div>
                    <div class="col-auto" v-if="task.dueDate">
                      <i class="bi bi-calendar-event me-1"></i>截止: {{ formatDate(task.dueDate) }}
                    </div>
                    <div class="col-auto">
                      <i class="bi bi-clock me-1"></i>已用时 {{ calculateDuration(task.createdAt) }}
                    </div>
                  </div>
                  <div class="mt-2 text-muted small" v-if="task.description">
                    <i class="bi bi-info-circle me-1"></i>{{ task.description }}
                  </div>
                </div>
                <div class="col-auto">
                  <div class="d-flex gap-2">
                    <button class="btn btn-success btn-sm" @click="handleComplete(task)">
                      <i class="bi bi-check-lg me-1"></i>完成
                    </button>
                    <button class="btn btn-outline-secondary btn-sm" @click="handleView(task)">
                      <i class="bi bi-eye me-1"></i>详情
                    </button>
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
                  <th>任务标题</th>
                  <th>类型</th>
                  <th>优先级</th>
                  <th>设备</th>
                  <th>完成时间</th>
                  <th>用时</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="completedTasks.length === 0">
                  <td colspan="8" class="text-center text-muted py-4">暂无已完成的工单</td>
                </tr>
                <tr v-else v-for="task in completedTasks" :key="task.id">
                  <td class="fw-semibold">WO{{ String(task.id).padStart(6, '0') }}</td>
                  <td>{{ task.title }}</td>
                  <td><span class="badge" :class="getTypeBadge(task.taskType)">{{ getTypeText(task.taskType) }}</span></td>
                  <td><span class="badge" :class="getPriorityBadge(task.priority)">{{ getPriorityText(task.priority) }}</span></td>
                  <td>{{ task.equipmentName || '-' }}</td>
                  <td>{{ formatDateTime(task.completedAt) }}</td>
                  <td>{{ calculateTaskDuration(task.createdAt, task.completedAt) }}</td>
                  <td><button class="btn btn-sm btn-outline-primary" @click="handleView(task)">查看</button></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 完成工单对话框 -->
    <el-dialog v-model="completeDialogVisible" title="完成工单" width="500px">
      <el-form :model="completeForm" label-width="80px">
        <el-form-item label="任务标题">
          <span>{{ currentTask?.title }}</span>
        </el-form-item>
        <el-form-item label="巡检报告" required>
          <el-input
            v-model="completeForm.report"
            type="textarea"
            :rows="5"
            placeholder="请填写巡检报告内容，描述任务完成情况..."
          />
        </el-form-item>
        <el-form-item label="上传图片">
          <el-upload
            :auto-upload="false"
            :show-file-list="true"
            list-type="picture-card"
            accept="image/*"
            :limit="5"
            :on-change="handleImageChange"
            :on-remove="handleImageRemove"
            :on-exceed="() => ElMessage.warning('最多上传5张图片')"
          >
            <i class="bi bi-plus" style="font-size: 24px;"></i>
          </el-upload>
          <div class="text-muted small mt-1">可选，最多5张图片，支持jpg/png格式</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitComplete" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>

    <!-- 任务详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="任务详情" width="600px">
      <div class="task-detail" v-if="currentTask">
        <div class="detail-row">
          <span class="detail-label">工单号：</span>
          <span>WO{{ String(currentTask.id).padStart(6, '0') }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">任务标题：</span>
          <span>{{ currentTask.title }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">任务类型：</span>
          <span class="badge" :class="getTypeBadge(currentTask.taskType)">{{ getTypeText(currentTask.taskType) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">优先级：</span>
          <span class="badge" :class="getPriorityBadge(currentTask.priority)">{{ getPriorityText(currentTask.priority) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态：</span>
          <span class="badge" :class="getStatusBadge(currentTask.status)">{{ getStatusText(currentTask.status) }}</span>
        </div>
        <div class="detail-row" v-if="currentTask.equipmentName">
          <span class="detail-label">关联设备：</span>
          <span>{{ currentTask.equipmentName }}</span>
        </div>
        <div class="detail-row" v-if="currentTask.dueDate">
          <span class="detail-label">截止日期：</span>
          <span>{{ formatDate(currentTask.dueDate) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">创建时间：</span>
          <span>{{ formatDateTime(currentTask.createdAt) }}</span>
        </div>
        <div class="detail-row" v-if="currentTask.completedAt">
          <span class="detail-label">完成时间：</span>
          <span>{{ formatDateTime(currentTask.completedAt) }}</span>
        </div>
        <div class="detail-row" v-if="currentTask.assignerName">
          <span class="detail-label">分配人：</span>
          <span>{{ currentTask.assignerName }}</span>
        </div>
        <div class="detail-row" v-if="currentTask.description">
          <span class="detail-label">任务描述：</span>
          <span>{{ currentTask.description }}</span>
        </div>
        <div class="detail-row" v-if="currentTask.report">
          <span class="detail-label">巡检报告：</span>
          <span>{{ currentTask.report }}</span>
        </div>
        <div class="detail-row" v-if="currentTask.reportImages">
          <span class="detail-label">完成图片：</span>
          <div class="feedback-images">
            <el-image
              v-for="(url, idx) in currentTask.reportImages.split(',')"
              :key="idx"
              :src="url"
              :preview-src-list="currentTask.reportImages.split(',')"
              fit="cover"
              class="feedback-img"
            />
          </div>
        </div>
        <div class="detail-row" v-if="currentTask.feedbackId && feedbackImages.length > 0">
          <span class="detail-label">问题图片：</span>
          <div class="feedback-images">
            <el-image
              v-for="img in feedbackImages"
              :key="img.id"
              :src="img.accessUrl || img.imageUrl"
              :preview-src-list="feedbackImages.map(i => i.accessUrl || i.imageUrl)"
              fit="cover"
              class="feedback-img"
            />
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getInspectorMyTasks, inspectorCompleteTask } from '@/api/task'
import { getFeedbackImages } from '@/api/feedback'

const activeTab = ref('processing')
const allTasks = ref([])
const loading = ref(false)
const submitting = ref(false)

// 完成工单对话框
const completeDialogVisible = ref(false)
const completeForm = ref({ report: '', images: [] })
const currentTask = ref(null)

// 详情对话框
const detailDialogVisible = ref(false)

// 反馈图片
const feedbackImages = ref([])

// 进行中任务
const processingTasks = computed(() => {
  return allTasks.value.filter(t => t.status === 'IN_PROGRESS')
})

// 已完成任务
const completedTasks = computed(() => {
  return allTasks.value.filter(t => t.status === 'COMPLETED')
})

// 加载任务列表
async function loadTasks() {
  loading.value = true
  try {
    const res = await getInspectorMyTasks({})
    if (res && res.code === 200 && res.data) {
      allTasks.value = res.data || []
    }
  } catch (e) {
    console.error('加载任务失败', e)
    ElMessage.error('加载任务失败')
  } finally {
    loading.value = false
  }
}

// 打开完成工单对话框
function handleComplete(task) {
  currentTask.value = task
  completeForm.value = { report: '', images: [] }
  completeDialogVisible.value = true
}

// 图片上传相关
function handleImageChange(file) {
  completeForm.value.images.push(file.raw)
}

function handleImageRemove(file) {
  const idx = completeForm.value.images.findIndex(f => f.name === file.name && f.size === file.size)
  if (idx > -1) completeForm.value.images.splice(idx, 1)
}

// 提交完成工单
async function submitComplete() {
  if (!completeForm.value.report.trim()) {
    ElMessage.warning('请填写巡检报告')
    return
  }
  
  submitting.value = true
  try {
    const res = await inspectorCompleteTask(
      currentTask.value.id,
      completeForm.value.report,
      completeForm.value.images
    )
    if (res && res.code === 200) {
      ElMessage.success('工单已完成')
      completeDialogVisible.value = false
      loadTasks()
    } else {
      ElMessage.error(res?.message || '操作失败')
    }
  } catch (e) {
    console.error('完成任务失败', e)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

// 查看详情
function handleView(task) {
  currentTask.value = task
  feedbackImages.value = []
  detailDialogVisible.value = true
  
  // 如果任务关联了反馈，加载反馈图片
  if (task.feedbackId) {
    loadFeedbackImages(task.feedbackId)
  }
}

// 加载反馈图片
async function loadFeedbackImages(feedbackId) {
  try {
    const res = await getFeedbackImages(feedbackId)
    if (res && res.code === 200 && res.data) {
      feedbackImages.value = res.data || []
    }
  } catch (e) {
    console.warn('加载反馈图片失败', e)
  }
}

// 获取任务类型图标
function getTaskIcon(type) {
  const map = {
    REPAIR: 'bi bi-tools',
    INSPECTION: 'bi bi-search',
    MAINTENANCE: 'bi bi-gear'
  }
  return map[type] || 'bi bi-tools'
}

// 获取任务类型背景色
function getTaskBgClass(type) {
  const map = {
    REPAIR: 'bg-danger',
    INSPECTION: 'bg-success',
    MAINTENANCE: 'bg-info'
  }
  return map[type] || 'bg-secondary'
}

// 获取任务类型图标颜色
function getTaskIconColor(type) {
  const map = {
    REPAIR: 'text-danger',
    INSPECTION: 'text-success',
    MAINTENANCE: 'text-info'
  }
  return map[type] || 'text-secondary'
}

// 获取任务类型文本
function getTypeText(type) {
  const map = {
    REPAIR: '故障维修',
    INSPECTION: '设备巡检',
    MAINTENANCE: '预防维护'
  }
  return map[type] || type
}

// 获取任务类型徽章样式
function getTypeBadge(type) {
  const map = {
    REPAIR: 'bg-danger',
    INSPECTION: 'bg-success',
    MAINTENANCE: 'bg-info'
  }
  return map[type] || 'bg-secondary'
}

// 获取优先级文本
function getPriorityText(priority) {
  const map = {
    LOW: '低',
    NORMAL: '普通',
    HIGH: '高',
    URGENT: '紧急'
  }
  return map[priority] || priority
}

// 获取优先级徽章样式
function getPriorityBadge(priority) {
  const map = {
    LOW: 'bg-secondary',
    NORMAL: 'bg-primary',
    HIGH: 'bg-warning',
    URGENT: 'bg-danger'
  }
  return map[priority] || 'bg-secondary'
}

// 获取优先级颜色
function getPriorityColor(priority) {
  const map = {
    LOW: '#6c757d',
    NORMAL: '#0d6efd',
    HIGH: '#ffc107',
    URGENT: '#dc3545'
  }
  return map[priority] || '#6c757d'
}

// 获取状态文本
function getStatusText(status) {
  const map = {
    PENDING: '待派单',
    IN_PROGRESS: '进行中',
    COMPLETED: '已完成'
  }
  return map[status] || status
}

// 获取状态徽章样式
function getStatusBadge(status) {
  const map = {
    PENDING: 'bg-warning',
    IN_PROGRESS: 'bg-primary',
    COMPLETED: 'bg-success'
  }
  return map[status] || 'bg-secondary'
}

// 格式化日期
function formatDate(date) {
  if (!date) return ''
  return date.substring(0, 10)
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
  if (hours < 24) return `${hours}小时${mins}分钟`
  const days = Math.floor(hours / 24)
  const remainHours = hours % 24
  return `${days}天${remainHours}小时`
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
  if (hours < 24) return `${hours}小时${mins}分钟`
  const days = Math.floor(hours / 24)
  const remainHours = hours % 24
  return `${days}天${remainHours}小时`
}

onMounted(() => {
  loadTasks()
})
</script>

<style lang="scss" scoped>
.my-tasks-page {
  .task-summary {
    .badge {
      font-size: 0.85rem;
      padding: 0.5em 0.8em;
    }
  }

  .order-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    border-left: 4px solid #0d6efd;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    transition: box-shadow 0.2s;
    
    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
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
        font-weight: 500;
      }
    }
    
    .feedback-images {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      
      .feedback-img {
        width: 100px;
        height: 100px;
        border-radius: 6px;
        cursor: pointer;
        border: 1px solid #e2e8f0;
      }
    }
  }
}
</style>
