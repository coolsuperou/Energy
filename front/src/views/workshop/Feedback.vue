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
                <label class="form-label">{{ form.type === 'fault' ? '故障设备' : '相关设备' }}</label>
                <select v-model="form.equipmentId" class="form-select" @change="onEquipmentChange">
                  <option :value="null">请选择</option>
                  <option v-for="eq in equipmentList" :key="eq.id" :value="eq.id">
                    {{ eq.name }}{{ eq.location ? ' (' + eq.location + ')' : '' }}
                  </option>
                  <option :value="-1">其他</option>
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
            
            <!-- 图片上传区域 -->
            <div class="mb-3">
              <label class="form-label">
                <i class="bi bi-image me-1"></i>上传图片附件
                <span class="text-muted small">（可选，最多10张，单张最大15MB，仅限图片格式）</span>
              </label>
              <el-upload
                ref="uploadRef"
                v-model:file-list="fileList"
                list-type="picture-card"
                :auto-upload="false"
                :limit="10"
                :on-exceed="handleExceed"
                :before-upload="beforeUpload"
                :on-preview="handlePreview"
                :on-remove="handleRemove"
                accept="image/*"
              >
                <i class="bi bi-plus-lg fs-4"></i>
              </el-upload>
              <div class="text-muted small mt-1">
                已选择 {{ fileList.length }} 张图片
              </div>
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
                <th>图片</th>
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
                <td>
                  <span v-if="item.imageCount > 0" class="badge bg-info">
                    <i class="bi bi-image me-1"></i>{{ item.imageCount }}张
                  </span>
                  <span v-else class="text-muted">-</span>
                </td>
                <td>{{ formatTime(item.createTime) }}</td>
                <td><span :class="['badge-status', getStatusClass(item.status)]">{{ getStatusText(item.status) }}</span></td>
                <td>
                  <button class="btn btn-sm btn-outline-primary me-1" @click="viewDetail(item)">查看</button>
                  <button v-if="item.status === 'pending'" class="btn btn-sm btn-outline-danger" @click="handleWithdraw(item)">撤回</button>
                </td>
              </tr>
              <tr v-if="feedbacks.length === 0 && !loading">
                <td colspan="7" class="text-center text-muted py-4">暂无反馈记录</td>
              </tr>
              <tr v-if="loading">
                <td colspan="7" class="text-center py-4">
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
      <div class="modal-content modal-lg">
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
          
          <!-- 图片附件区域 -->
          <div class="detail-row" v-if="detailImages.length > 0 || loadingImages">
            <span class="label">图片附件：</span>
            <div class="image-gallery">
              <div v-if="loadingImages" class="text-muted">
                <div class="spinner-border spinner-border-sm" role="status"></div>
                <span class="ms-2">加载图片中...</span>
              </div>
              <div v-else class="d-flex flex-wrap gap-2">
                <div 
                  v-for="(img, index) in detailImages" 
                  :key="img.id" 
                  class="image-thumbnail"
                  @click="previewImage(img.accessUrl)"
                >
                  <img :src="img.accessUrl" :alt="img.originalName" />
                  <div class="image-overlay">
                    <i class="bi bi-zoom-in"></i>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="!loadingImages" class="detail-row">
            <span class="label">图片附件：</span>
            <span class="text-muted">无</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览弹窗 -->
    <el-image-viewer
      v-if="showImageViewer"
      :url-list="[previewImageUrl]"
      @close="showImageViewer = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getMyFeedbacks, submitFeedback, withdrawFeedback, uploadFeedbackImages, getFeedbackImages } from '@/api/feedback'
import { getEquipments } from '@/api/equipment'

const userStore = useUserStore()
const activeTab = ref('submit')
const statusFilter = ref('')
const showDetail = ref(false)
const currentDetail = ref({})
const loading = ref(false)
const submitting = ref(false)
const uploadRef = ref(null)

// 图片相关
const fileList = ref([])
const detailImages = ref([])
const loadingImages = ref(false)
const showImageViewer = ref(false)
const previewImageUrl = ref('')

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const form = ref({
  type: 'fault',
  location: '',
  equipmentId: null,
  urgency: 'normal',
  description: ''
})

const feedbacks = ref([])
const equipmentList = ref([])

const processingCount = computed(() => {
  return feedbacks.value.filter(f => f.status === 'processing' || f.status === 'pending').length
})

// 加载设备列表
async function loadEquipments() {
  try {
    const res = await getEquipments()
    if (res && res.code === 200 && res.data) {
      equipmentList.value = res.data || []
    }
  } catch (e) {
    console.error('加载设备列表失败', e)
  }
}

// 设备选择变化时自动填充location
function onEquipmentChange() {
  const eqId = form.value.equipmentId
  if (eqId === -1) {
    form.value.location = '其他'
  } else if (eqId) {
    const eq = equipmentList.value.find(e => e.id === eqId)
    if (eq) {
      form.value.location = eq.name + (eq.location ? ' - ' + eq.location : '')
    }
  } else {
    form.value.location = ''
  }
}

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
    // 1. 先提交反馈
    const res = await submitFeedback({
      userId: userStore.user?.id,
      type: form.value.type,
      location: form.value.location,
      equipmentId: form.value.equipmentId > 0 ? form.value.equipmentId : null,
      urgency: form.value.urgency,
      description: form.value.description
    })
    
    if (res.code === 200 && res.data) {
      const feedbackId = res.data.id
      
      // 2. 如果有图片，上传图片
      if (fileList.value.length > 0) {
        try {
          const files = fileList.value.map(f => f.raw)
          await uploadFeedbackImages(feedbackId, files)
          ElMessage.success('反馈提交成功，图片已上传')
        } catch (uploadError) {
          console.error('图片上传失败', uploadError)
          ElMessage.warning('反馈已提交，但部分图片上传失败')
        }
      } else {
        ElMessage.success('反馈提交成功')
      }
      
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

// 图片上传相关方法
function handleExceed(files) {
  ElMessage.warning('最多只能上传10张图片')
}

function beforeUpload(file) {
  // 验证文件类型
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  
  // 验证文件大小（15MB）
  const isLt15M = file.size / 1024 / 1024 < 15
  if (!isLt15M) {
    ElMessage.error('图片大小不能超过15MB')
    return false
  }
  
  return true
}

function handlePreview(file) {
  previewImageUrl.value = file.url
  showImageViewer.value = true
}

function handleRemove(file, fileList) {
  // 文件移除时的处理
}

// 查看详情
async function viewDetail(item) {
  currentDetail.value = item
  showDetail.value = true
  
  // 加载图片
  loadingImages.value = true
  detailImages.value = []
  try {
    const res = await getFeedbackImages(item.id)
    if (res.code === 200 && res.data) {
      detailImages.value = res.data
    }
  } catch (e) {
    console.error('加载图片失败', e)
  } finally {
    loadingImages.value = false
  }
}

// 预览图片
function previewImage(url) {
  previewImageUrl.value = url
  showImageViewer.value = true
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
  form.value = { type: 'fault', location: '', equipmentId: null, urgency: 'normal', description: '' }
  fileList.value = []
}

onMounted(() => {
  loadFeedbacks()
  loadEquipments()
})
</script>

<style lang="scss">
@use '@/styles/workshop.scss';

// 图片上传样式
.el-upload--picture-card {
  width: 100px;
  height: 100px;
  line-height: 100px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  
  &:hover {
    border-color: #409eff;
  }
}

.el-upload-list--picture-card .el-upload-list__item {
  width: 100px;
  height: 100px;
}

// 详情弹窗图片区域
.image-gallery {
  flex: 1;
}

.image-thumbnail {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  
  .image-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.2s;
    
    i {
      color: #fff;
      font-size: 20px;
    }
  }
  
  &:hover .image-overlay {
    opacity: 1;
  }
}

// 大尺寸弹窗
.modal-lg {
  max-width: 700px;
}
</style>
