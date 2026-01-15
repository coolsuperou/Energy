<template>
  <div class="feedback-page">
    <!-- 标签页 -->
    <ul class="nav nav-tabs mb-4">
      <li class="nav-item">
        <button :class="['nav-link', { active: activeTab === 'submit' }]" @click="activeTab = 'submit'">提交反馈</button>
      </li>
      <li class="nav-item">
        <button :class="['nav-link', { active: activeTab === 'history' }]" @click="activeTab = 'history'; loadFeedbacks()">
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
          <form class="feedback-form" @submit.prevent="submitFeedbackForm">
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
            <div class="d-flex gap-2 justify-content-end">
              <button type="button" class="btn btn-outline-secondary" @click="resetForm">取消</button>
              <button type="submit" class="btn btn-primary" :disabled="submitting">
                <i class="bi bi-send me-1"></i>{{ submitting ? '提交中...' : '提交反馈' }}
              </button>
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
          <select v-model="statusFilter" class="form-select form-select-sm" style="width: auto;" @change="loadFeedbacks">
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
              <tr v-for="item in feedbacks" :key="item.id">
                <td class="fw-semibold">{{ item.feedbackNo }}</td>
                <td><span :class="['badge', getTypeBadgeClass(item.type)]">{{ getTypeText(item.type) }}</span></td>
                <td class="text-truncate" style="max-width: 200px;">{{ item.description }}</td>
                <td>{{ formatTime(item.createTime) }}</td>
                <td><span :class="['badge-status', getStatusClass(item.status)]">{{ getStatusText(item.status) }}</span></td>
                <td>
                  <button class="btn btn-sm btn-outline-primary me-1" @click="viewDetail(item)">查看</button>
                  <button v-if="item.status === 'pending'" class="btn btn-sm btn-outline-danger" @click="handleWithdraw(item)">撤回</button>
                </td>
              </tr>
              <tr v-if="feedbacks.length === 0 && !loading">
                <td colspan="6" class="text-center text-muted py-4">暂无反馈记录</td>
              </tr>
              <tr v-if="loading">
                <td colspan="6" class="text-center py-4">
                  <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
                  <span class="ms-2">加载中...</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <!-- 分页 -->
        <div v-if="total > pageSize" class="card-footer d-flex justify-content-end">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="loadFeedbacks"
          />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getMyFeedbacks, submitFeedback, withdrawFeedback } from '@/api/feedback'

const userStore = useUserStore()
const activeTab = ref('submit')
const statusFilter = ref('')
const showDetail = ref(false)
const currentDetail = ref({})
const loading = ref(false)
const submitting = ref(false)

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const form = ref({
  type: 'fault',
  location: '',
  urgency: 'normal',
  description: ''
})

const feedbacks = ref([])

const processingCount = computed(() => {
  return feedbacks.value.filter(f => f.status === 'processing' || f.status === 'pending').length
})

// 加载反馈列表
async function loadFeedbacks() {
  loading.value = true
  try {
    const res = await getMyFeedbacks({
      status: statusFilter.value,
      page: currentPage.value,
      size: pageSize.value
    })
    if (res.code === 200 && res.data) {
      feedbacks.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('加载反馈列表失败', e)
  } finally {
    loading.value = false
  }
}

// 提交反馈
async function submitFeedbackForm() {
  if (!form.value.description.trim()) {
    ElMessage.warning('请填写问题描述')
    return
  }
  
  submitting.value = true
  try {
    const res = await submitFeedback({
      userId: userStore.user?.id,
      type: form.value.type,
      location: form.value.location,
      urgency: form.value.urgency,
      description: form.value.description
    })
    if (res.code === 200) {
      ElMessage.success('反馈提交成功')
      resetForm()
      activeTab.value = 'history'
      loadFeedbacks()
    }
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

// 撤回反馈
async function handleWithdraw(item) {
  try {
    await ElMessageBox.confirm('确定要撤回该反馈吗？撤回后将无法恢复。', '撤回确认', {
      confirmButtonText: '确定撤回',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await withdrawFeedback(item.id, userStore.user?.id)
    if (res.code === 200) {
      ElMessage.success('反馈已撤回')
      loadFeedbacks()
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '撤回失败')
    }
  }
}

function getTypeText(type) {
  const map = { fault: '故障报修', suggestion: '用电建议', question: '咨询问题', other: '其他反馈' }
  return map[type] || type
}

function getTypeBadgeClass(type) {
  const map = { fault: 'bg-danger', suggestion: 'bg-warning text-dark', question: 'bg-info', other: 'bg-secondary' }
  return map[type] || 'bg-secondary'
}

function getStatusText(status) {
  const map = { pending: '待处理', processing: '处理中', resolved: '已处理', withdrawn: '已撤回' }
  return map[status] || status
}

function getStatusClass(status) {
  const map = { pending: 'badge-pending', processing: 'badge-processing', resolved: 'badge-approved', withdrawn: 'badge-rejected' }
  return map[status] || ''
}

function getUrgencyText(urgency) {
  const map = { normal: '一般', urgent: '紧急', critical: '非常紧急' }
  return map[urgency] || urgency
}

function formatTime(time) {
  if (!time) return '-'
  return time.substring(0, 16).replace('T', ' ')
}

function resetForm() {
  form.value = { type: 'fault', location: '', urgency: 'normal', description: '' }
}

function viewDetail(item) {
  currentDetail.value = item
  showDetail.value = true
}

onMounted(() => {
  loadFeedbacks()
})
</script>

<style lang="scss">
@use '@/styles/workshop.scss';
</style>
