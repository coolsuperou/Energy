<template>
  <div class="approval-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>待审批申请</span>
          <el-button type="primary" link @click="loadApplications">刷新</el-button>
        </div>
      </template>

      <el-table :data="applications" v-loading="loading" stripe>
        <el-table-column prop="applicationNo" label="申请编号" width="160"/>
        <el-table-column prop="userName" label="申请人" width="100"/>
        <el-table-column prop="equipmentName" label="设备" width="120"/>
        <el-table-column prop="power" label="功率(kW)" width="90"/>
        <el-table-column prop="applyDate" label="用电日期" width="110"/>
        <el-table-column label="时间段" width="130">
          <template #default="{ row }">
            {{ row.startTime?.substring(0,5) }} - {{ row.endTime?.substring(0,5) }}
          </template>
        </el-table-column>
        <el-table-column prop="urgency" label="紧急程度" width="100">
          <template #default="{ row }">
            <el-tag :type="urgencyType(row.urgency)">{{ urgencyText(row.urgency) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="purpose" label="用途" show-overflow-tooltip/>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleApprove(row)">批准</el-button>
            <el-button type="warning" size="small" @click="handleAdjust(row)">调整</el-button>
            <el-button type="danger" size="small" @click="handleReject(row)">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pagination.page" v-model:page-size="pagination.size"
        :total="pagination.total" layout="total, prev, pager, next" @current-change="loadApplications"
        style="margin-top: 16px; justify-content: flex-end"/>
    </el-card>

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
          <span>{{ currentApp?.startTime?.substring(0,5) }} - {{ currentApp?.endTime?.substring(0,5) }}</span>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getApplications, approveApplication, rejectApplication, adjustApplication } from '@/api/application'

const loading = ref(false)
const submitting = ref(false)
const applications = ref([])
const dialogVisible = ref(false)
const dialogType = ref('approve')
const dialogTitle = ref('')
const currentApp = ref(null)

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const approvalForm = reactive({
  comment: '',
  adjustedStartTime: null,
  adjustedEndTime: null
})

const loadApplications = async () => {
  loading.value = true
  try {
    const res = await getApplications({ page: pagination.page, size: pagination.size })
    applications.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (e) {}
  loading.value = false
}

const handleApprove = (row) => {
  currentApp.value = row
  dialogType.value = 'approve'
  dialogTitle.value = '批准申请'
  resetApprovalForm()
  dialogVisible.value = true
}

const handleAdjust = (row) => {
  currentApp.value = row
  dialogType.value = 'adjust'
  dialogTitle.value = '调整并批准'
  resetApprovalForm()
  approvalForm.adjustedStartTime = row.startTime
  approvalForm.adjustedEndTime = row.endTime
  dialogVisible.value = true
}

const handleReject = (row) => {
  currentApp.value = row
  dialogType.value = 'reject'
  dialogTitle.value = '拒绝申请'
  resetApprovalForm()
  dialogVisible.value = true
}

const resetApprovalForm = () => {
  approvalForm.comment = ''
  approvalForm.adjustedStartTime = null
  approvalForm.adjustedEndTime = null
}

const submitApproval = async () => {
  if (dialogType.value === 'reject' && !approvalForm.comment) {
    ElMessage.warning('请填写拒绝原因')
    return
  }

  submitting.value = true
  try {
    const id = currentApp.value.id
    if (dialogType.value === 'approve') {
      await approveApplication(id, { comment: approvalForm.comment })
      ElMessage.success('已批准')
    } else if (dialogType.value === 'adjust') {
      await adjustApplication(id, approvalForm)
      ElMessage.success('已调整并批准')
    } else {
      await rejectApplication(id, { comment: approvalForm.comment })
      ElMessage.success('已拒绝')
    }
    dialogVisible.value = false
    loadApplications()
  } catch (e) {}
  submitting.value = false
}

const urgencyType = (urgency) => {
  const map = { normal: 'info', urgent: 'warning', critical: 'danger' }
  return map[urgency] || 'info'
}

const urgencyText = (urgency) => {
  const map = { normal: '普通', urgent: '紧急', critical: '非常紧急' }
  return map[urgency] || urgency
}

onMounted(() => {
  loadApplications()
})
</script>

<style scoped>
.approval-page {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
