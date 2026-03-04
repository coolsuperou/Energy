<template>
  <div class="feedback-manage">
    <h4 class="mb-3">问题反馈处理</h4>

    <!-- 筛选栏 -->
    <div class="filter-bar mb-3">
      <select v-model="filters.status" class="form-select form-select-sm" @change="loadFeedbacks">
        <option value="">全部状态</option>
        <option value="pending">待处理</option>
        <option value="processing">处理中</option>
        <option value="resolved">已解决</option>
        <option value="withdrawn">已撤回</option>
      </select>
      <select v-model="filters.type" class="form-select form-select-sm" @change="loadFeedbacks">
        <option value="">全部类型</option>
        <option value="fault">故障报修</option>
        <option value="suggestion">用电建议</option>
        <option value="question">咨询问题</option>
        <option value="other">其他反馈</option>
      </select>
    </div>

    <!-- 反馈列表 -->
    <div class="card">
      <div class="card-body p-0">
        <table class="table table-hover mb-0">
          <thead>
            <tr>
              <th>反馈编号</th>
              <th>类型</th>
              <th>提交人</th>
              <th>问题描述</th>
              <th>紧急程度</th>
              <th>状态</th>
              <th>提交时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in feedbacks" :key="item.id">
              <td>{{ item.feedbackNo }}</td>
              <td><span :class="['badge-type', 'type-' + item.type]">{{ typeText(item.type) }}</span></td>
              <td>{{ item.userName || '-' }}</td>
              <td class="desc-cell" :title="item.description">{{ item.description }}</td>
              <td><span :class="['badge-urgency', 'urgency-' + item.urgency]">{{ urgencyText(item.urgency) }}</span></td>
              <td><span :class="['badge-status', statusClass(item.status)]">{{ statusText(item.status) }}</span></td>
              <td>{{ formatTime(item.createTime) }}</td>
              <td>
                <div class="btn-actions">
                  <button class="btn btn-sm btn-outline-primary" @click="showDetail(item)">详情</button>
                  <button v-if="item.status === 'pending'" class="btn btn-sm btn-primary" @click="showHandleModal(item)">回复</button>
                  <button v-if="item.status === 'pending' && item.type === 'fault'" class="btn btn-sm btn-outline-warning" @click="showDispatchModal(item)">转工单</button>
                </div>
              </td>
            </tr>
            <tr v-if="feedbacks.length === 0">
              <td colspan="8" class="text-center text-muted py-4">暂无反馈记录</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-bar" v-if="total > pageSize">
      <button class="btn btn-sm btn-outline" :disabled="page <= 1" @click="page--; loadFeedbacks()">上一页</button>
      <span class="page-info">第 {{ page }} / {{ totalPages }} 页，共 {{ total }} 条</span>
      <button class="btn btn-sm btn-outline" :disabled="page >= totalPages" @click="page++; loadFeedbacks()">下一页</button>
    </div>

    <!-- 详情弹窗 -->
    <div class="modal-overlay" v-if="detailVisible" @click.self="detailVisible = false">
      <div class="modal-content" style="max-width: 640px;">
        <div class="modal-header">
          <h5><i class="bi bi-info-circle me-1"></i> 反馈详情</h5>
          <button class="btn-close" @click="detailVisible = false"></button>
        </div>
        <div class="modal-body" v-if="currentFeedback">
          <!-- 顶部状态栏 -->
          <div class="detail-status-bar">
            <span class="detail-no">{{ currentFeedback.feedbackNo }}</span>
            <span :class="['badge-status', statusClass(currentFeedback.status)]">{{ statusText(currentFeedback.status) }}</span>
          </div>

          <!-- 基本信息卡片 -->
          <div class="detail-section">
            <div class="detail-section-title"><i class="bi bi-person"></i> 基本信息</div>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">反馈类型</span>
                <span :class="['badge-type', 'type-' + currentFeedback.type]">{{ typeText(currentFeedback.type) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">紧急程度</span>
                <span :class="['badge-urgency', 'urgency-' + currentFeedback.urgency]">{{ urgencyText(currentFeedback.urgency) }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">提交人</span>
                <span class="detail-value">{{ currentFeedback.userName || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">相关位置</span>
                <span class="detail-value">{{ currentFeedback.location || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">提交时间</span>
                <span class="detail-value">{{ formatTime(currentFeedback.createTime) }}</span>
              </div>
            </div>
          </div>

          <!-- 问题描述 -->
          <div class="detail-section">
            <div class="detail-section-title"><i class="bi bi-chat-left-text"></i> 问题描述</div>
            <div class="detail-desc-box">{{ currentFeedback.description }}</div>
          </div>

          <!-- 处理信息（已处理时显示） -->
          <div class="detail-section" v-if="currentFeedback.reply || currentFeedback.handlerName">
            <div class="detail-section-title"><i class="bi bi-check-circle"></i> 处理信息</div>
            <div class="detail-grid">
              <div class="detail-item" v-if="currentFeedback.handlerName">
                <span class="detail-label">处理人</span>
                <span class="detail-value">{{ currentFeedback.handlerName }}</span>
              </div>
              <div class="detail-item" v-if="currentFeedback.handledAt">
                <span class="detail-label">处理时间</span>
                <span class="detail-value">{{ formatTime(currentFeedback.handledAt) }}</span>
              </div>
            </div>
            <div class="detail-reply-box" v-if="currentFeedback.reply">
              <div class="reply-label">处理回复</div>
              <div class="reply-content">{{ currentFeedback.reply }}</div>
            </div>
          </div>

          <!-- 图片附件 -->
          <div class="detail-section">
            <div class="detail-section-title"><i class="bi bi-image"></i> 图片附件</div>
            <div v-if="loadingImages" class="text-muted small">
              <div class="spinner-border spinner-border-sm" role="status"></div>
              <span class="ms-2">加载图片中...</span>
            </div>
            <div v-else-if="detailImages.length > 0" class="d-flex flex-wrap gap-2">
              <div 
                v-for="img in detailImages" 
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
            <div v-else class="text-muted small">无</div>
          </div>

        </div>
        <!-- 底部操作（固定在弹窗底部） -->
        <div class="detail-footer" v-if="currentFeedback && currentFeedback.status === 'pending'">
          <button class="btn btn-primary btn-sm" @click="detailVisible = false; showHandleModal(currentFeedback)">
            <i class="bi bi-reply"></i> 处理反馈
          </button>
          <button class="btn btn-outline-warning btn-sm" v-if="currentFeedback.type === 'fault'" @click="detailVisible = false; showDispatchModal(currentFeedback)">
            <i class="bi bi-arrow-right-circle"></i> 转工单
          </button>
        </div>
      </div>
    </div>

    <!-- 处理弹窗 -->
    <div class="modal-overlay" v-if="handleVisible" @click.self="handleVisible = false">
      <div class="modal-content" style="max-width: 500px;">
        <div class="modal-header">
          <h5>处理反馈</h5>
          <button class="btn-close" @click="handleVisible = false"></button>
        </div>
        <div class="modal-body">
          <div class="form-group mb-3">
            <label>处理回复 <span class="text-danger">*</span></label>
            <textarea v-model="handleForm.reply" class="form-control" rows="4" placeholder="请输入处理回复..."></textarea>
          </div>
          <div class="form-actions">
            <button class="btn btn-outline-secondary btn-sm" @click="handleVisible = false">取消</button>
            <button class="btn btn-primary btn-sm" @click="doHandle" :disabled="!handleForm.reply.trim()">确认处理</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 图片预览 -->
    <div class="image-preview-overlay" v-if="showImageViewer" @click="showImageViewer = false">
      <img :src="previewImageUrl" @click.stop />
      <button class="preview-close" @click="showImageViewer = false">&times;</button>
    </div>

    <!-- 转工单弹窗 -->
    <div class="modal-overlay" v-if="dispatchVisible" @click.self="dispatchVisible = false">
      <div class="modal-content" style="max-width: 560px;">
        <div class="modal-header">
          <h5><i class="bi bi-arrow-right-circle me-1"></i> 反馈转工单</h5>
          <button class="btn-close" @click="dispatchVisible = false"></button>
        </div>
        <div class="modal-body">
          <p class="text-muted small mb-3">将此故障反馈转为维修工单，选择一位巡检员处理</p>
          
          <div class="inspector-list">
            <div v-if="inspectors.length === 0" class="text-center text-muted py-4">
              <i class="bi bi-person-x d-block mb-2" style="font-size: 32px;"></i>
              <span>当前没有在班的巡检员</span>
            </div>
            <div 
              v-for="ins in inspectors" :key="ins.id"
              :class="['inspector-card', { selected: dispatchForm.assigneeId === ins.id }]"
              @click="dispatchForm.assigneeId = ins.id"
            >
              <div class="inspector-avatar">{{ ins.name?.charAt(0) }}</div>
              <div class="inspector-info">
                <div class="inspector-name">{{ ins.name }}</div>
                <div class="inspector-dept">{{ ins.department }}</div>
              </div>
              <div class="inspector-tags">
                <span :class="['shift-tag', 'shift-' + (ins.shiftType || 'none')]">
                  {{ shiftText(ins.shiftType) }}
                </span>
                <span class="task-tag" v-if="ins.inProgressTasks > 0">
                  {{ ins.inProgressTasks }}个任务
                </span>
                <span class="task-tag idle" v-else>空闲</span>
              </div>
              <div class="inspector-check" v-if="dispatchForm.assigneeId === ins.id">
                <i class="bi bi-check-circle-fill"></i>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button class="btn btn-outline-secondary btn-sm" @click="dispatchVisible = false">取消</button>
            <button class="btn btn-primary btn-sm" @click="doDispatch" :disabled="!dispatchForm.assigneeId">确认派单</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getAllFeedbacks, handleFeedback, getFeedbackImages } from '@/api/feedback'
import request from '@/api/request'

const feedbacks = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const filters = ref({ status: '', type: '' })
const totalPages = computed(() => Math.ceil(total.value / pageSize))

const currentFeedback = ref(null)
const detailVisible = ref(false)
const handleVisible = ref(false)
const dispatchVisible = ref(false)
const handleForm = ref({ reply: '' })
const dispatchForm = ref({ assigneeId: '' })
const inspectors = ref([])
const detailImages = ref([])
const loadingImages = ref(false)
const showImageViewer = ref(false)
const previewImageUrl = ref('')

function typeText(type) {
  const map = { fault: '故障报修', suggestion: '用电建议', question: '咨询问题', other: '其他' }
  return map[type] || type
}

function urgencyText(u) {
  const map = { normal: '一般', urgent: '紧急', critical: '严重' }
  return map[u] || u
}

function statusText(s) {
  const map = { pending: '待处理', processing: '处理中', resolved: '已解决', withdrawn: '已撤回' }
  return map[s] || s
}

function shiftText(type) {
  const map = { day: '白班 ☀️', night: '夜班 🌙', rest: '休息' }
  return map[type] || '未排班'
}

function statusClass(s) {
  const map = { pending: 'badge-pending', processing: 'badge-processing', resolved: 'badge-approved', withdrawn: 'badge-rejected' }
  return map[s] || ''
}

function formatTime(t) {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 16)
}

async function loadFeedbacks() {
  try {
    const params = { page: page.value, size: pageSize }
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.type) params.type = filters.value.type
    const res = await getAllFeedbacks(params)
    if (res.code === 200 && res.data) {
      feedbacks.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    console.error('加载反馈列表失败', e)
  }
}

async function loadInspectors() {
  try {
    const res = await request.get('/dispatcher/inspectors')
    if (res.code === 200) {
      inspectors.value = res.data || []
    }
  } catch (e) {
    console.warn('加载巡检员列表失败', e)
  }
}

async function showDetail(item) {
  currentFeedback.value = item
  detailVisible.value = true
  
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

function previewImage(url) {
  previewImageUrl.value = url
  showImageViewer.value = true
}

function showHandleModal(item) {
  currentFeedback.value = item
  handleForm.value.reply = ''
  handleVisible.value = true
}

function showDispatchModal(item) {
  currentFeedback.value = item
  dispatchForm.value.assigneeId = ''
  dispatchVisible.value = true
}

async function doHandle() {
  try {
    const res = await handleFeedback(currentFeedback.value.id, {
      reply: handleForm.value.reply,
      status: 'resolved'
    })
    if (res.code === 200) {
      handleVisible.value = false
      loadFeedbacks()
    }
  } catch (e) {
    console.error('处理反馈失败', e)
  }
}

async function doDispatch() {
  try {
    const res = await request.post(`/feedback/${currentFeedback.value.id}/dispatch`, {
      assigneeId: dispatchForm.value.assigneeId
    })
    if (res.code === 200) {
      dispatchVisible.value = false
      alert('已成功转为工单')
      loadFeedbacks()
    }
  } catch (e) {
    console.error('转工单失败', e)
  }
}

onMounted(() => {
  loadFeedbacks()
  loadInspectors()
})
</script>

<style lang="scss">
@use '@/styles/dispatcher.scss';

.feedback-manage {
  .filter-bar {
    display: flex;
    gap: 12px;
    .form-select-sm { max-width: 160px; }
  }
  .desc-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .btn-actions { display: flex; gap: 6px; flex-wrap: nowrap; }
  .badge-type { padding: 3px 8px; border-radius: 4px; font-size: 12px; white-space: nowrap; }
  .type-fault { background: #fee2e2; color: #dc2626; }
  .type-suggestion { background: #dbeafe; color: #2563eb; }
  .type-question { background: #fef3c7; color: #d97706; }
  .type-other { background: #f1f5f9; color: #64748b; }
  .badge-urgency { padding: 3px 8px; border-radius: 4px; font-size: 12px; white-space: nowrap; }
  .urgency-normal { background: #f1f5f9; color: #64748b; }
  .urgency-urgent { background: #fef3c7; color: #d97706; }
  .urgency-critical { background: #fee2e2; color: #dc2626; }
  .badge-status { padding: 4px 10px; border-radius: 6px; font-size: 12px; font-weight: 500; white-space: nowrap; display: inline-block; }
  .badge-pending { background: #fef3c7; color: #d97706; }
  .badge-approved { background: #dcfce7; color: #16a34a; }
  .badge-rejected { background: #fee2e2; color: #dc2626; }
  .badge-processing { background: #dbeafe; color: #2563eb; }
  .pagination-bar { display: flex; align-items: center; justify-content: center; gap: 16px; margin-top: 16px; }
  .page-info { font-size: 14px; color: #64748b; }

  // 详情弹窗美化
  .detail-status-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: #f8fafc;
    border-radius: 8px;
    margin-bottom: 20px;
  }
  .detail-no { font-size: 15px; font-weight: 600; color: #334155; font-family: monospace; }

  .detail-section {
    margin-bottom: 20px;
  }
  .detail-section-title {
    font-size: 14px;
    font-weight: 600;
    color: #475569;
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    gap: 6px;
    padding-bottom: 8px;
    border-bottom: 1px solid #f1f5f9;
    i { color: #3b82f6; }
  }
  .detail-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }
  .detail-item {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .detail-label {
    font-size: 12px;
    color: #94a3b8;
  }
  .detail-value {
    font-size: 14px;
    color: #1e293b;
  }
  .detail-desc-box {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 14px 16px;
    font-size: 14px;
    color: #334155;
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-all;
  }
  .detail-reply-box {
    margin-top: 12px;
    background: #f0fdf4;
    border: 1px solid #bbf7d0;
    border-radius: 8px;
    padding: 14px 16px;
    .reply-label { font-size: 12px; color: #16a34a; margin-bottom: 6px; font-weight: 500; }
    .reply-content { font-size: 14px; color: #334155; line-height: 1.6; }
  }
  .detail-footer {
    display: flex;
    gap: 10px;
    justify-content: flex-end;
    padding: 12px 20px;
    border-top: 1px solid #e2e8f0;
    background: #fff;
    border-radius: 0 0 8px 8px;
    flex-shrink: 0;
  }
}

// 弹窗样式
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1050;
}

.modal-content {
  background: #fff;
  border-radius: 8px;
  width: 90%;
  max-height: 95vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;

  h5 { margin: 0; font-weight: 600; font-size: 16px; }

  .btn-close {
    background: transparent;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    opacity: 0.5;
    line-height: 1;
    &:hover { opacity: 1; }
    &::before { content: '×'; }
  }
}

.modal-body {
  padding: 16px 20px;
  overflow-y: auto;
  flex: 1;
}

.detail-row {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px solid #f1f5f9;
  line-height: 1.6;

  .label {
    width: 100px;
    flex-shrink: 0;
    color: #64748b;
    font-size: 14px;
  }

  span:last-child {
    flex: 1;
    color: #1e293b;
    font-size: 14px;
    word-break: break-all;
  }
}

.form-group {
  label {
    display: block;
    margin-bottom: 6px;
    font-weight: 500;
    font-size: 14px;
  }
}

.form-control {
  display: block;
  width: 100%;
  padding: 8px 12px;
  font-size: 14px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  &:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

// 巡检员卡片列表
.inspector-list {
  max-height: 320px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  padding-right: 4px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
}

.inspector-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;

  &:hover { border-color: #93c5fd; background: #f8fafc; }
  &.selected { border-color: #3b82f6; background: #eff6ff; }
}

.inspector-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}

.inspector-info {
  flex: 1;
  min-width: 0;
}

.inspector-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.inspector-dept {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

.inspector-tags {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.shift-tag {
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}

.shift-day { background: #fef3c7; color: #d97706; }
.shift-night { background: #e0e7ff; color: #4338ca; }
.shift-rest { background: #f1f5f9; color: #94a3b8; }
.shift-none { background: #f1f5f9; color: #cbd5e1; }

.task-tag {
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  background: #fee2e2;
  color: #dc2626;
  white-space: nowrap;

  &.idle { background: #dcfce7; color: #16a34a; }
}

// 图片缩略图
.image-thumbnail {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  border: 1px solid #e2e8f0;
  
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
    background: rgba(0, 0, 0, 0.4);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.2s;
    color: #fff;
    font-size: 20px;
  }
  
  &:hover .image-overlay {
    opacity: 1;
  }
}

// 图片预览
.image-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  
  img {
    max-width: 90%;
    max-height: 90%;
    object-fit: contain;
    border-radius: 4px;
  }
  
  .preview-close {
    position: absolute;
    top: 20px;
    right: 30px;
    background: none;
    border: none;
    color: #fff;
    font-size: 36px;
    cursor: pointer;
    opacity: 0.7;
    &:hover { opacity: 1; }
  }
}

.inspector-check {
  position: absolute;
  top: 8px;
  right: 10px;
  color: #3b82f6;
  font-size: 18px;
}
</style>
