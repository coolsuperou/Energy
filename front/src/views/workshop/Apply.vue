<template>
  <div class="apply-page">
    <!-- 配额概览 -->
    <div class="quota-section">
      <div class="quota-card">
        <div class="quota-header">
          <div class="quota-info">
            <h6>本月用电配额</h6>
            <div class="quota-value">
              <span class="current">35,680</span>
              <span class="total">/ 50,000 kWh</span>
            </div>
          </div>
          <div class="quota-remain">
            <div class="label">剩余配额</div>
            <div class="value">14,320 kWh</div>
          </div>
        </div>
        <div class="quota-progress">
          <div class="quota-progress-bar" style="width: 71.4%"></div>
        </div>
        <div class="quota-footer">
          <span>已使用 71.4%</span>
          <span>本月剩余 {{ remainingDays }} 天</span>
        </div>
      </div>
      <div class="stat-card">
        <h6>申请统计</h6>
        <div class="stat-item"><span>本月申请</span><span class="fw-bold">{{ stats.total }} 次</span></div>
        <div class="stat-item"><span>已批准</span><span class="fw-bold text-success">{{ stats.approved }} 次</span></div>
        <div class="stat-item"><span>待审批</span><span class="fw-bold text-warning">{{ stats.pending }} 次</span></div>
        <div class="stat-item"><span>已拒绝</span><span class="fw-bold text-danger">{{ stats.rejected }} 次</span></div>
      </div>
    </div>

    <!-- 标签页 -->
    <div class="tabs">
      <button :class="['tab-btn', { active: activeTab === 'new' }]" @click="activeTab = 'new'">新建申请</button>
      <button :class="['tab-btn', { active: activeTab === 'list' }]" @click="activeTab = 'list'">
        申请记录 <span v-if="stats.pending > 0" class="badge">{{ stats.pending }}</span>
      </button>
    </div>

    <!-- 新建申请 -->
    <div v-show="activeTab === 'new'" class="card">
      <div class="card-header">填写用电申请</div>
      <div class="card-body">
        <form class="apply-form" @submit.prevent="submitApply">
          <!-- 用电设备 -->
          <h6 class="section-title"><i class="bi bi-cpu"></i>用电设备</h6>
          <div class="form-row">
            <div class="form-group">
              <label>设备名称 <span class="required">*</span></label>
              <select v-model="form.equipmentId" class="form-select" @change="onEquipmentChange">
                <option value="">请选择设备</option>
                <option v-for="eq in equipments" :key="eq.id" :value="eq.id"
                        :disabled="eq.status === 'fault'"
                        :style="eq.status === 'fault' ? 'color: #ef4444; background: #fef2f2' : eq.status === 'warning' ? 'color: #f59e0b' : ''">
                  {{ eq.name }} (额定功率: {{ eq.ratedPower }}kW){{ eq.status === 'fault' ? ' 🔧 报修中' : eq.status === 'warning' ? ' ⚠️ 异常' : '' }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>申请功率 (kW) <span class="required">*</span></label>
              <input v-model.number="form.power" type="number" class="form-input" placeholder="请输入申请功率" min="0.1" step="0.1" max="200" @input="calculateCost">
              <div class="form-hint">当前车间最大可用功率: <strong>200 kW</strong></div>
            </div>
          </div>

          <!-- 用电时段 -->
          <h6 class="section-title"><i class="bi bi-clock"></i>用电时段 <span class="hint">(参考电价)</span></h6>
          <div class="time-slots">
            <div class="time-slot peak">
              <div class="slot-header">
                <span class="slot-name text-danger">峰时</span>
                <span class="slot-price bg-danger">电价 1.2元/kWh</span>
              </div>
              <div class="slot-time">08:00 - 11:00<br>18:00 - 21:00</div>
            </div>
            <div class="time-slot normal">
              <div class="slot-header">
                <span class="slot-name text-warning">平时</span>
                <span class="slot-price bg-warning">电价 0.8元/kWh</span>
              </div>
              <div class="slot-time">07:00 - 08:00, 11:00 - 18:00<br>21:00 - 23:00</div>
            </div>
            <div class="time-slot valley">
              <div class="slot-header">
                <span class="slot-name text-success">谷时</span>
                <span class="slot-price bg-success">电价 0.4元/kWh</span>
              </div>
              <div class="slot-time">23:00 - 07:00</div>
            </div>
          </div>
          <div class="form-row form-row-3">
            <div class="form-group">
              <label>申请日期 <span class="required">*</span></label>
              <input v-model="form.applyDate" type="date" class="form-input" :min="minDate">
            </div>
            <div class="form-group">
              <label>开始时间 <span class="required">*</span></label>
              <input v-model="form.startTime" type="time" class="form-input" @change="calculateCost">
            </div>
            <div class="form-group">
              <label>结束时间 <span class="required">*</span></label>
              <input v-model="form.endTime" type="time" class="form-input" @change="calculateCost">
            </div>
          </div>

          <!-- 用途说明 -->
          <h6 class="section-title"><i class="bi bi-file-text"></i>用途说明</h6>
          <div class="form-row">
            <div class="form-group">
              <label>紧急程度</label>
              <select v-model="form.urgency" class="form-select">
                <option value="NORMAL">普通</option>
                <option value="URGENT">紧急</option>
                <option value="CRITICAL">非常紧急</option>
              </select>
            </div>
            <div class="form-group">
              <label>详细说明</label>
              <textarea v-model="form.purpose" class="form-textarea" rows="2" placeholder="请描述用电用途和具体需求..."></textarea>
            </div>
          </div>

          <!-- 预估费用 -->
          <div class="estimate-box">
            <div class="estimate-info">
              <i class="bi bi-calculator"></i>
              <span>预估用电量: <strong>{{ estEnergy }}</strong> kWh</span>
              <span class="divider">|</span>
              <span>预估费用: <strong>¥{{ estCost }}</strong></span>
            </div>
            <button type="button" class="btn-calc" @click="calculateCost">重新计算</button>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-outline" @click="resetForm">重置</button>
            <button type="submit" class="btn btn-primary"><i class="bi bi-send"></i>提交申请</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 申请记录 -->
    <div v-show="activeTab === 'list'" class="card">
      <div class="card-header">
        <span>申请记录</span>
        <select v-model="statusFilter" class="filter-select">
          <option value="">全部状态</option>
          <option value="pending">待审批</option>
          <option value="approved">已批准</option>
          <option value="rejected">已拒绝</option>
        </select>
      </div>
      <div class="table-responsive">
        <table class="table table-hover mb-0">
          <thead>
            <tr>
              <th>申请编号</th>
              <th>设备/用途</th>
              <th>申请时段</th>
              <th>功率</th>
              <th>预估费用</th>
              <th>状态</th>
              <th>审批意见</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filteredApplications" :key="item.id">
              <td class="fw-semibold text-primary">{{ item.applicationNo }}</td>
              <td>{{ item.equipmentName || '-' }} · {{ item.purpose || '生产用电' }}</td>
              <td>{{ formatDateTime(item.applyDate, item.startTime, item.endTime) }}</td>
              <td>{{ item.power }} kW</td>
              <td>¥{{ calculateEstCost(item) }}</td>
              <td><span :class="['badge', getBadgeClass(item.status)]">{{ getStatusText(item.status) }}</span></td>
              <td :class="getCommentClass(item.status)">{{ item.comment || '-' }}</td>
              <td>
                <template v-if="isPending(item.status)">
                  <button class="btn btn-sm btn-outline-danger" @click="cancelApplication(item.id)">撤回</button>
                </template>
                <template v-else-if="isRejected(item.status)">
                  <button class="btn btn-sm btn-outline-primary me-1" @click="viewDetail(item)">详情</button>
                  <button class="btn btn-sm btn-primary" @click="reapply(item)">重新申请</button>
                </template>
                <template v-else>
                  <button class="btn btn-sm btn-outline-primary" @click="viewDetail(item)">详情</button>
                </template>
              </td>
            </tr>
            <tr v-if="applications.length === 0">
              <td colspan="8" class="text-center text-muted py-4">暂无申请记录</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 申请详情对话框 -->
    <el-dialog v-model="detailDialog.visible" title="申请详情" width="600px" destroy-on-close>
      <div v-if="detailDialog.data" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请编号">{{ detailDialog.data.applicationNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <span :class="['badge', getBadgeClass(detailDialog.data.status)]">{{ getStatusText(detailDialog.data.status) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="设备名称">{{ detailDialog.data.equipmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请功率">{{ detailDialog.data.power }} kW</el-descriptions-item>
          <el-descriptions-item label="申请日期">{{ detailDialog.data.applyDate }}</el-descriptions-item>
          <el-descriptions-item label="用电时段">{{ formatTime(detailDialog.data.startTime) }} - {{ formatTime(detailDialog.data.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="紧急程度">{{ getUrgencyText(detailDialog.data.urgency) }}</el-descriptions-item>
          <el-descriptions-item label="预估费用">¥{{ calculateEstCost(detailDialog.data) }}</el-descriptions-item>
          <el-descriptions-item label="用途说明" :span="2">{{ detailDialog.data.purpose || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审批意见" :span="2">
            <span :class="getCommentClass(detailDialog.data.status)">{{ detailDialog.data.comment || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 评论交流组件 -->
        <CommentSection 
          related-type="application" 
          :related-id="detailDialog.data.id" 
        />
      </div>
      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getEquipments } from '@/api/equipment'
import { submitApplication, getMyApplications, cancelApplication as cancelApplicationApi } from '@/api/application'
import CommentSection from '@/components/CommentSection.vue'

const activeTab = ref('new')
const statusFilter = ref('')
const selectedSlot = ref('')
const equipments = ref([])
const applications = ref([])
const estEnergy = ref(0)
const estCost = ref(0)

const stats = ref({ total: 0, approved: 0, pending: 0, rejected: 0 })

const remainingDays = computed(() => {
  const now = new Date()
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate()
  return lastDay - now.getDate()
})

const form = ref({
  equipmentId: '',
  power: '',
  applyDate: new Date().toISOString().split('T')[0],
  startTime: (() => { const n = new Date(); return String(n.getHours()).padStart(2, '0') + ':' + String(n.getMinutes()).padStart(2, '0') })(),
  endTime: (() => { const n = new Date(); return String(Math.min(n.getHours() + 4, 23)).padStart(2, '0') + ':' + String(n.getMinutes()).padStart(2, '0') })(),
  urgency: 'NORMAL',
  purpose: ''
})

const minDate = computed(() => new Date().toISOString().split('T')[0])

const filteredApplications = computed(() => {
  if (!statusFilter.value) return applications.value
  return applications.value.filter(a => (a.status || '').toLowerCase() === statusFilter.value.toLowerCase())
})

function selectSlot(slot) {
  selectedSlot.value = slot
  const times = { peak: ['08:00', '11:00'], normal: ['11:00', '18:00'], valley: ['23:00', '07:00'] }
  form.value.startTime = times[slot][0]
  form.value.endTime = times[slot][1]
  calculateCost()
}

function onEquipmentChange() {
  const eq = equipments.value.find(e => e.id === form.value.equipmentId)
  if (eq) form.value.power = eq.ratedPower
  calculateCost()
}

function calculateCost() {
  const power = form.value.power || 0
  const start = form.value.startTime
  const end = form.value.endTime
  if (!start || !end || !power) { estEnergy.value = 0; estCost.value = 0; return }
  
  const startH = parseInt(start.split(':')[0])
  const endH = parseInt(end.split(':')[0])
  const hours = endH > startH ? endH - startH : 24 - startH + endH
  
  const energy = power * hours
  
  // 根据开始时间自动判断时段类型
  let price = 0.8 // 默认平时
  if ((startH >= 8 && startH < 11) || (startH >= 18 && startH < 21)) {
    price = 1.2 // 峰时
  } else if (startH >= 23 || startH < 7) {
    price = 0.4 // 谷时
  }
  
  estEnergy.value = energy
  estCost.value = (energy * price).toFixed(0)
}

function getStatusClass(status) {
  const s = (status || '').toLowerCase()
  return { pending: 'pending', approved: 'approved', rejected: 'rejected', adjusted: 'approved' }[s] || 'pending'
}

function getStatusText(status) {
  const s = (status || '').toLowerCase()
  return { pending: '待审批', approved: '已批准', rejected: '已拒绝', adjusted: '已调整' }[s] || status
}

function getBadgeClass(status) {
  const s = (status || '').toLowerCase()
  const map = {
    pending: 'bg-warning text-dark',
    approved: 'bg-success',
    rejected: 'bg-danger',
    adjusted: 'bg-success'
  }
  return map[s] || 'bg-secondary'
}

function getCommentClass(status) {
  const s = (status || '').toLowerCase()
  if (s === 'rejected') return 'text-danger small'
  if (s === 'approved' || s === 'adjusted') return 'text-success small'
  return 'text-muted'
}

function isPending(status) {
  return (status || '').toLowerCase() === 'pending'
}

function isRejected(status) {
  return (status || '').toLowerCase() === 'rejected'
}

function formatDateTime(date, startTime, endTime) {
  if (!date) return '-'
  const d = date.substring(5) // 去掉年份，只保留 MM-DD
  return `${d} ${formatTime(startTime)}-${formatTime(endTime)}`
}

function formatTime(time) {
  if (!time) return ''
  return time.substring(0, 5)
}

function getUrgencyClass(urgency) {
  return { NORMAL: 'normal', URGENT: 'urgent', CRITICAL: 'critical' }[urgency] || 'normal'
}

function getUrgencyText(urgency) {
  return { NORMAL: '普通', URGENT: '紧急', CRITICAL: '非常紧急' }[urgency] || urgency
}

async function loadEquipments() {
  try {
    const res = await getEquipments()
    if (res.code === 200) {
      equipments.value = (res.data || []).map(eq => ({
        ...eq,
        status: (eq.status || '').toLowerCase()
      }))
    }
  } catch (e) { console.error('加载设备失败', e) }
}

async function loadApplications() {
  try {
    const res = await getMyApplications({ page: 1, size: 100 })
    if (res.code === 200) {
      applications.value = res.data.records || res.data || []
      updateStats()
    }
  } catch (e) { console.error('加载申请失败', e) }
}

function updateStats() {
  const list = applications.value
  stats.value = {
    total: list.length,
    approved: list.filter(a => {
      const s = (a.status || '').toLowerCase()
      return s === 'approved' || s === 'adjusted'
    }).length,
    pending: list.filter(a => (a.status || '').toLowerCase() === 'pending').length,
    rejected: list.filter(a => (a.status || '').toLowerCase() === 'rejected').length
  }
}

async function submitApply() {
  if (!form.value.equipmentId) { ElMessage.warning('请选择用电设备'); return }
  if (!form.value.power || form.value.power <= 0) { ElMessage.warning('请输入有效的申请功率'); return }
  if (!form.value.applyDate) { ElMessage.warning('请选择申请日期'); return }
  
  try {
    const res = await submitApplication(form.value)
    if (res.code === 200) {
      ElMessage.success('申请提交成功')
      resetForm()
      loadApplications()
      activeTab.value = 'list'
    }
  } catch (e) { /* handled by interceptor */ }
}

function resetForm() {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  const endHour = String(Math.min(now.getHours() + 4, 23)).padStart(2, '0')
  form.value = {
    equipmentId: '', power: '',
    applyDate: now.toISOString().split('T')[0],
    startTime: hh + ':' + mm,
    endTime: endHour + ':' + mm,
    urgency: 'NORMAL', purpose: ''
  }
  selectedSlot.value = ''
  estEnergy.value = 0
  estCost.value = 0
}

function cancelApplication(id) {
  ElMessageBox.confirm('确定要撤回此申请吗？', '提示', { type: 'warning' }).then(async () => {
    try {
      const res = await cancelApplicationApi(id)
      if (res.code === 200) {
        ElMessage.success('撤回成功')
        loadApplications()
      }
    } catch (e) { /* handled by interceptor */ }
  }).catch(() => {})
}

// 详情对话框
const detailDialog = ref({
  visible: false,
  data: null
})

function viewDetail(item) {
  detailDialog.value = {
    visible: true,
    data: item
  }
}

function calculateEstCost(item) {
  const power = item.power || 0
  const start = item.startTime
  const end = item.endTime
  if (!start || !end || !power) return 0
  
  const startH = parseInt(start.split(':')[0])
  const endH = parseInt(end.split(':')[0])
  const hours = endH > startH ? endH - startH : 24 - startH + endH
  
  const energy = power * hours
  const price = 0.8 // 默认平时电价
  return (energy * price).toFixed(0)
}

function reapply(item) {
  form.value = {
    equipmentId: item.equipmentId || '',
    power: item.power,
    applyDate: item.applyDate,
    startTime: item.startTime,
    endTime: item.endTime,
    urgency: item.urgency || 'NORMAL',
    purpose: item.purpose || ''
  }
  activeTab.value = 'new'
  ElMessage.info('已填充原申请信息，请修改后重新提交')
}

onMounted(() => {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  form.value.applyDate = now.toISOString().split('T')[0]
  form.value.startTime = hh + ':' + mm
  form.value.endTime = String(Math.min(now.getHours() + 4, 23)).padStart(2, '0') + ':' + mm
  loadEquipments()
  loadApplications()
})
</script>

<style lang="scss">
@use '@/styles/workshop.scss';
</style>
