<template>
  <div class="feedback-page">
    <!-- 标签页 -->
    <ul class="nav nav-tabs mb-4">
      <li class="nav-item">
        <button :class="['nav-link', { active: activeTab === 'submit' }]" @click="activeTab = 'submit'">提交反馈</button>
      </li>
      <li class="nav-item">
        <button :class="['nav-link', { active: activeTab === 'history' }]" @click="activeTab = 'history'">
          我的反馈
          <span v-if="processingCount > 0" class="badge bg-warning ms-1">{{ processingCount }}</span>
        </button>
      </li>
    </ul>

    <!-- 提交反馈 -->
    <div v-show="activeTab === 'submit'">
      <!-- 反馈类型选择 -->
      <div class="card mb-4">
        <div class="card-header">选择反馈类型</div>
        <div class="card-body">
          <div class="row g-3">
            <div class="col-6 col-md-3">
              <div :class="['type-card', { active: form.type === 'fault' }]" @click="form.type = 'fault'">
                <i class="bi bi-tools fs-2 text-primary d-block mb-2"></i>
                <span class="fw-semibold">故障报修</span>
              </div>
            </div>
            <div class="col-6 col-md-3">
              <div :class="['type-card', { active: form.type === 'suggestion' }]" @click="form.type = 'suggestion'">
                <i class="bi bi-lightbulb fs-2 text-warning d-block mb-2"></i>
                <span>用电建议</span>
              </div>
            </div>
            <div class="col-6 col-md-3">
              <div :class="['type-card', { active: form.type === 'question' }]" @click="form.type = 'question'">
                <i class="bi bi-question-circle fs-2 text-info d-block mb-2"></i>
                <span>咨询问题</span>
              </div>
            </div>
            <div class="col-6 col-md-3">
              <div :class="['type-card', { active: form.type === 'other' }]" @click="form.type = 'other'">
                <i class="bi bi-megaphone fs-2 text-secondary d-block mb-2"></i>
                <span>其他反馈</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 反馈内容表单 -->
      <div class="card">
        <div class="card-header">填写反馈内容</div>
        <div class="card-body">
          <form class="feedback-form" @submit.prevent="submitFeedback">
            <div class="row g-3 mb-3">
              <div class="col-md-6">
                <label class="form-label">{{ form.type === 'fault' ? '故障位置' : '相关位置' }}</label>
                <select v-model="form.location" class="form-select">
                  <option value="">请选择</option>
                  <option value="生产线A">生产线A</option>
                  <option value="生产线B">生产线B</option>
                  <option value="配电室">配电室</option>
                  <option value="其他">其他</option>
                </select>
              </div>
              <div class="col-md-6">
                <label class="form-label">紧急程度</label>
                <select v-model="form.urgency" class="form-select">
                  <option value="normal">一般</option>
                  <option value="urgent">紧急</option>
                  <option value="critical">非常紧急</option>
                </select>
              </div>
            </div>
            <div class="mb-3">
              <label class="form-label">问题描述 <span class="text-danger">*</span></label>
              <textarea v-model="form.description" class="form-control" rows="4" placeholder="请详细描述问题情况..."></textarea>
            </div>
            <div class="mb-4">
              <label class="form-label">上传图片</label>
              <div class="upload-area" @click="triggerUpload" @dragover.prevent @drop.prevent="handleDrop">
                <input ref="fileInput" type="file" accept="image/*" multiple hidden @change="handleFileChange">
                <i class="bi bi-cloud-arrow-up fs-1 text-muted d-block mb-2"></i>
                <span class="text-muted">点击或拖拽上传图片</span>
              </div>
              <div v-if="form.images.length > 0" class="image-preview mt-2">
                <div v-for="(img, index) in form.images" :key="index" class="preview-item">
                  <img :src="img.url" :alt="img.name">
                  <button type="button" class="remove-btn" @click="removeImage(index)">
                    <i class="bi bi-x"></i>
                  </button>
                </div>
              </div>
            </div>
            <div class="d-flex gap-2 justify-content-end">
              <button type="button" class="btn btn-outline-secondary" @click="resetForm">取消</button>
              <button type="submit" class="btn btn-primary"><i class="bi bi-send me-1"></i>提交反馈</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 我的反馈记录 -->
    <div v-show="activeTab === 'history'">
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <span>我的反馈记录</span>
          <select v-model="statusFilter" class="form-select form-select-sm" style="width: auto;">
            <option value="">全部状态</option>
            <option value="pending">待处理</option>
            <option value="processing">处理中</option>
            <option value="resolved">已处理</option>
          </select>
        </div>
        <div class="table-responsive">
          <table class="table table-hover mb-0">
            <thead>
              <tr>
                <th>编号</th>
                <th>类型</th>
                <th>描述</th>
                <th>提交时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredFeedbacks" :key="item.id">
                <td class="fw-semibold">{{ item.feedbackNo }}</td>
                <td><span :class="['badge', getTypeBadgeClass(item.type)]">{{ getTypeText(item.type) }}</span></td>
                <td class="text-truncate" style="max-width: 200px;">{{ item.description }}</td>
                <td>{{ formatTime(item.createTime) }}</td>
                <td><span :class="['badge-status', getStatusClass(item.status)]">{{ getStatusText(item.status) }}</span></td>
                <td><button class="btn btn-sm btn-outline-primary" @click="viewDetail(item)">查看</button></td>
              </tr>
              <tr v-if="feedbacks.length === 0">
                <td colspan="6" class="text-center text-muted py-4">暂无反馈记录</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div v-if="showDetail" class="modal-overlay" @click.self="showDetail = false">
      <div class="modal-content">
        <div class="modal-header">
          <h5>反馈详情</h5>
          <button class="btn-close" @click="showDetail = false"></button>
        </div>
        <div class="modal-body">
          <div class="detail-row">
            <span class="label">反馈编号：</span>
            <span>{{ currentDetail.feedbackNo }}</span>
          </div>
          <div class="detail-row">
            <span class="label">反馈类型：</span>
            <span :class="['badge', getTypeBadgeClass(currentDetail.type)]">{{ getTypeText(currentDetail.type) }}</span>
          </div>
          <div class="detail-row">
            <span class="label">相关位置：</span>
            <span>{{ currentDetail.location || '-' }}</span>
          </div>
          <div class="detail-row">
            <span class="label">紧急程度：</span>
            <span>{{ getUrgencyText(currentDetail.urgency) }}</span>
          </div>
          <div class="detail-row">
            <span class="label">问题描述：</span>
            <span>{{ currentDetail.description }}</span>
          </div>
          <div class="detail-row">
            <span class="label">提交时间：</span>
            <span>{{ formatTime(currentDetail.createTime) }}</span>
          </div>
          <div class="detail-row">
            <span class="label">当前状态：</span>
            <span :class="['badge-status', getStatusClass(currentDetail.status)]">{{ getStatusText(currentDetail.status) }}</span>
          </div>
          <div v-if="currentDetail.reply" class="detail-row">
            <span class="label">处理回复：</span>
            <span class="text-success">{{ currentDetail.reply }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const activeTab = ref('submit')
const statusFilter = ref('')
const showDetail = ref(false)
const currentDetail = ref({})
const fileInput = ref(null)

const form = ref({
  type: 'fault',
  location: '',
  urgency: 'normal',
  description: '',
  images: []
})

// 模拟反馈数据
const feedbacks = ref([
  {
    id: 1,
    feedbackNo: 'FB202401150001',
    type: 'fault',
    location: '生产线A',
    urgency: 'urgent',
    description: '生产线A电机异常响声，运行时有明显震动',
    createTime: '2024-01-15 10:30:00',
    status: 'processing',
    reply: '已派维修人员前往检查'
  },
  {
    id: 2,
    feedbackNo: 'FB202401100002',
    type: 'suggestion',
    location: '配电室',
    urgency: 'normal',
    description: '建议增加配电室空调，夏季温度过高影响设备运行',
    createTime: '2024-01-10 14:20:00',
    status: 'resolved',
    reply: '已采纳建议，计划下月安装空调设备'
  },
  {
    id: 3,
    feedbackNo: 'FB202401080003',
    type: 'question',
    location: '',
    urgency: 'normal',
    description: '请问如何查看历史用电数据？',
    createTime: '2024-01-08 09:15:00',
    status: 'resolved',
    reply: '可在能耗查看页面查询历史数据'
  }
])

const processingCount = computed(() => {
  return feedbacks.value.filter(f => f.status === 'processing' || f.status === 'pending').length
})

const filteredFeedbacks = computed(() => {
  if (!statusFilter.value) return feedbacks.value
  return feedbacks.value.filter(f => f.status === statusFilter.value)
})

function getTypeText(type) {
  const map = { fault: '故障报修', suggestion: '用电建议', question: '咨询问题', other: '其他反馈' }
  return map[type] || type
}

function getTypeBadgeClass(type) {
  const map = { fault: 'bg-danger', suggestion: 'bg-warning text-dark', question: 'bg-info', other: 'bg-secondary' }
  return map[type] || 'bg-secondary'
}

function getStatusText(status) {
  const map = { pending: '待处理', processing: '处理中', resolved: '已处理' }
  return map[status] || status
}

function getStatusClass(status) {
  const map = { pending: 'badge-pending', processing: 'badge-processing', resolved: 'badge-approved' }
  return map[status] || ''
}

function getUrgencyText(urgency) {
  const map = { normal: '一般', urgent: '紧急', critical: '非常紧急' }
  return map[urgency] || urgency
}

function formatTime(time) {
  if (!time) return '-'
  return time.substring(0, 16)
}

function triggerUpload() {
  fileInput.value?.click()
}

function handleFileChange(e) {
  const files = e.target.files
  if (files) {
    Array.from(files).forEach(file => {
      if (file.type.startsWith('image/')) {
        const url = URL.createObjectURL(file)
        form.value.images.push({ name: file.name, url, file })
      }
    })
  }
}

function handleDrop(e) {
  const files = e.dataTransfer.files
  if (files) {
    Array.from(files).forEach(file => {
      if (file.type.startsWith('image/')) {
        const url = URL.createObjectURL(file)
        form.value.images.push({ name: file.name, url, file })
      }
    })
  }
}

function removeImage(index) {
  URL.revokeObjectURL(form.value.images[index].url)
  form.value.images.splice(index, 1)
}

function resetForm() {
  form.value = { type: 'fault', location: '', urgency: 'normal', description: '', images: [] }
}

function submitFeedback() {
  if (!form.value.description.trim()) {
    ElMessage.warning('请填写问题描述')
    return
  }
  
  // 生成反馈编号
  const now = new Date()
  const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '')
  const no = `FB${dateStr}${String(feedbacks.value.length + 1).padStart(4, '0')}`
  
  feedbacks.value.unshift({
    id: Date.now(),
    feedbackNo: no,
    type: form.value.type,
    location: form.value.location,
    urgency: form.value.urgency,
    description: form.value.description,
    createTime: now.toISOString().slice(0, 19).replace('T', ' '),
    status: 'pending',
    reply: ''
  })
  
  ElMessage.success('反馈提交成功')
  resetForm()
  activeTab.value = 'history'
}

function viewDetail(item) {
  currentDetail.value = item
  showDetail.value = true
}
</script>

<style lang="scss">
@import '@/styles/workshop.scss';
</style>
