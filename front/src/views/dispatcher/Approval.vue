<template>
  <div class="approval-page">
    <!-- 筛选和操作 -->
    <div class="card filter-card">
      <div class="card-body">
        <div class="filter-row">
          <el-radio-group v-model="filter.status" @change="loadApplications">
            <el-radio-button label="PENDING">待审批 ({{ counts.pending }})</el-radio-button>
            <el-radio-button label="APPROVED">已批准</el-radio-button>
            <el-radio-button label="REJECTED">已拒绝</el-radio-button>
            <el-radio-button label="">全部</el-radio-button>
          </el-radio-group>
          <div class="filter-spacer"></div>
          <el-select v-model="filter.workshopId" placeholder="全部车间" clearable style="width: 150px" @change="loadApplications">
            <el-option v-for="ws in workshops" :key="ws.id" :label="ws.name" :value="ws.id" />
          </el-select>
          <el-button type="success" :disabled="selectedIds.length === 0" @click="batchApprove">
            <i class="bi bi-check-all"></i> 批量审批
          </el-button>
        </div>
      </div>
    </div>

    <!-- 申请列表 -->
    <div class="application-list">
      <div v-for="item in applications" :key="item.id" 
           :class="['order-card', { urgent: item.urgency === 'CRITICAL' || item.urgency === 'critical' }]">
        <div class="order-row">
          <div class="order-checkbox">
            <el-checkbox v-model="item.selected" @change="updateSelection" />
          </div>
          <div class="order-content">
            <div class="order-header">
              <div class="order-title">
                <h6>{{ item.userName || '车间用户' }} - {{ item.purpose || '用电申请' }}</h6>
                <span :class="['badge', getUrgencyClass(item.urgency)]">{{ getUrgencyText(item.urgency) }}</span>
              </div>
            </div>
            <div class="order-meta">
              <span><i class="bi bi-calendar"></i>{{ item.applyDate }} {{ formatTime(item.startTime) }}-{{ formatTime(item.endTime) }}</span>
              <span><i class="bi bi-lightning-charge"></i>{{ item.power }} kW</span>
              <span><i class="bi bi-person"></i>{{ item.userName || '未知' }}</span>
              <span><i class="bi bi-clock"></i>提交于 {{ formatCreatedAt(item.createdAt) }}</span>
            </div>
            <div v-if="item.purpose" class="order-purpose">
              <strong>用途说明：</strong>{{ item.purpose }}
            </div>
          </div>
          <div class="order-actions">
            <template v-if="item.status === 'PENDING'">
              <el-button type="success" @click="handleApprove(item)"><i class="bi bi-check-lg"></i> 批准</el-button>
              <el-button @click="handleAdjust(item)"><i class="bi bi-pencil"></i> 调整</el-button>
              <el-button type="danger" plain @click="handleReject(item)"><i class="bi bi-x-lg"></i> 拒绝</el-button>
            </template>
            <template v-else>
              <span :class="['status-result', 'status-' + item.status.toLowerCase()]">
                <i :class="['bi', item.status === 'APPROVED' ? 'bi-check-circle' : 'bi-x-circle']"></i>
                {{ item.status === 'APPROVED' ? '已批准' : '已拒绝' }}
              </span>
            </template>
          </div>
        </div>
      </div>
      
      <div v-if="applications.length === 0 && !loading" class="empty-state">
        <i class="bi bi-inbox"></i>
        <span>暂无申请记录</span>
      </div>
    </div>

    <!-- 分页 -->
    <el-pagination 
      v-model:current-page="pagination.page" 
      v-model:page-size="pagination.size"
      :total="pagination.total" 
      layout="total, prev, pager, next"
      @current-change="loadApplications"
      style="margin-top: 20px; justify-content: flex-end"
    />

    <!-- 审批对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="approvalForm" label-width="100px">
        <el-form-item label="申请编号">
          <span>{{ currentApp?.applicationNo }}</span>
        </el-form-item>
        <el-form-item label="申请人">
          <span>{{ currentApp?.userName }}</span>
        </el-form-item>
        <el-form-item label="原时间段">
          <span>{{ formatTime(currentApp?.startTime) }} - {{ formatTime(currentApp?.endTime) }}</span>
        </el-form-item>

        <!-- 调整时间（仅调整模式显示） -->
        <template v-if="dialogType === 'adjust'">
          <el-form-item label="调整开始时间">
            <el-time-picker v-model="approvalForm.adjustedStartTime" format="HH:mm" 
              value-format="HH:mm:ss" style="width: 100%"/>
          </el-form-item>
          <el-form-item label="调整结束时间">
            <el-time-picker v-model="approvalForm.adjustedEndTime" format="HH:mm" 
              value-format="HH:mm:ss" style="width: 100%"/>
          </el-form-item>
        </template>

        <el-form-item label="审批意见">
          <el-input v-model="approvalForm.comment" type="textarea" :rows="3" 
            :placeholder="dialogType === 'reject' ? '请填写拒绝原因' : '可选填写审批意见'"/>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApproval" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApplications, getPendingApplications, approveApplication, rejectApplication, adjustApplication } from '@/api/application'

const loading = ref(false)
const submitting = ref(false)
const applications = ref([])
const dialogVisible = ref(false)
const dialogType = ref('approve')
const dialogTitle = ref('')
const currentApp = ref(null)

// 筛选条件
const filter = ref({
  status: 'PENDING',
  workshopId: ''
})

// 统计数量
const counts = ref({
  pending: 0,
  approved: 0,
  rejected: 0
})

// 车间列表
const workshops = ref([
  { id: 1, name: '生产一车间' },
  { id: 2, name: '生产二车间' },
  { id: 3, name: '装配车间' },
  { id: 4, name: '仓储车间' }
])

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 审批表单
const approvalForm = reactive({
  comment: '',
  adjustedStartTime: null,
  adjustedEndTime: null
})

// 选中的ID列表
const selectedIds = computed(() => {
  return applications.value.filter(a => a.selected && a.status === 'PENDING').map(a => a.id)
})

// 格式化时间
function formatTime(time) {
  if (!time) return ''
  return time.substring(0, 5)
}

// 格式化创建时间
function formatCreatedAt(createdAt) {
  if (!createdAt) return ''
  const date = new Date(createdAt)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return createdAt.split('T')[0]
}

// 获取紧急程度样式
function getUrgencyClass(urgency) {
  const u = (urgency || '').toUpperCase()
  const map = { 'CRITICAL': 'bg-danger', 'URGENT': 'bg-warning', 'NORMAL': 'bg-secondary' }
  return map[u] || 'bg-secondary'
}

// 获取紧急程度文本
function getUrgencyText(urgency) {
  const u = (urgency || '').toUpperCase()
  const map = { 'CRITICAL': '紧急', 'URGENT': '加急', 'NORMAL': '普通' }
  return map[u] || '待审批'
}

// 加载申请列表
async function loadApplications() {
  loading.value = true
  try {
    const params = { 
      page: pagination.page, 
      size: pagination.size 
    }
    
    if (filter.value.status) {
      params.status = filter.value.status
    }
    if (filter.value.workshopId) {
      params.workshopId = filter.value.workshopId
    }
    
    const res = await getApplications(params)
    if (res && res.code === 200 && res.data) {
      applications.value = (res.data.records || res.data || []).map(a => ({ ...a, selected: false }))
      pagination.total = res.data.total || applications.value.length
    }
    
    // 加载待审批数量
    const pendingRes = await getPendingApplications({ page: 1, size: 1 })
    if (pendingRes && pendingRes.code === 200 && pendingRes.data) {
      counts.value.pending = pendingRes.data.total || (pendingRes.data.records || pendingRes.data || []).length
    }
  } catch (e) {
    console.warn('加载申请列表失败', e)
    applications.value = []
  }
  loading.value = false
}

// 更新选择状态
function updateSelection() {
  // 触发计算属性更新
}

// 批准申请
function handleApprove(row) {
  currentApp.value = row
  dialogType.value = 'approve'
  dialogTitle.value = '批准申请'
  resetApprovalForm()
  dialogVisible.value = true
}

// 调整申请
function handleAdjust(row) {
  currentApp.value = row
  dialogType.value = 'adjust'
  dialogTitle.value = '调整并批准'
  resetApprovalForm()
  approvalForm.adjustedStartTime = row.startTime
  approvalForm.adjustedEndTime = row.endTime
  dialogVisible.value = true
}

// 拒绝申请
function handleReject(row) {
  currentApp.value = row
  dialogType.value = 'reject'
  dialogTitle.value = '拒绝申请'
  resetApprovalForm()
  dialogVisible.value = true
}

// 重置表单
function resetApprovalForm() {
  approvalForm.comment = ''
  approvalForm.adjustedStartTime = null
  approvalForm.adjustedEndTime = null
}

// 提交审批
async function submitApproval() {
  if (dialogType.value === 'reject' && !approvalForm.comment) {
    ElMessage.warning('请填写拒绝原因')
    return
  }

  submitting.value = true
  try {
    const id = currentApp.value.id
    if (dialogType.value === 'approve') {
      await approveApplication(id, { comment: approvalForm.comment })
      ElMessage.success('已批准，已通知申请人')
    } else if (dialogType.value === 'adjust') {
      await adjustApplication(id, approvalForm)
      ElMessage.success('已调整并批准，已通知申请人')
    } else {
      await rejectApplication(id, { comment: approvalForm.comment })
      ElMessage.success('已拒绝，已通知申请人')
    }
    dialogVisible.value = false
    loadApplications()
  } catch (e) {
    console.error('审批失败', e)
  }
  submitting.value = false
}

// 批量审批
async function batchApprove() {
  try {
    await ElMessageBox.confirm(`确认批量批准选中的 ${selectedIds.value.length} 个申请？`, '批量审批', { type: 'info' })
    
    for (const id of selectedIds.value) {
      await approveApplication(id, { comment: '批量批准' })
    }
    
    ElMessage.success(`已批准 ${selectedIds.value.length} 个申请`)
    loadApplications()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('批量审批失败', e)
    }
  }
}

onMounted(() => {
  loadApplications()
})
</script>

<style lang="scss">
@use '@/styles/dispatcher.scss';
</style>
