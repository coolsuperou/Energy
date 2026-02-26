<template>
  <div class="inspection-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h5 class="mb-1"><i class="bi bi-clipboard2-check me-2"></i>每周巡检</h5>
        <span class="text-muted small">{{ weekLabel }}</span>
      </div>
    </div>

    <!-- 概览卡片 -->
    <div class="overview-cards">
      <div class="overview-card">
        <div class="overview-icon blue"><i class="bi bi-cpu"></i></div>
        <div><div class="overview-value">{{ totalEquipments }}</div><div class="overview-label">待巡检设备</div></div>
      </div>
      <div class="overview-card">
        <div class="overview-icon green"><i class="bi bi-check-circle"></i></div>
        <div><div class="overview-value">{{ checkedEquipments }}</div><div class="overview-label">已完成</div></div>
      </div>
      <div class="overview-card">
        <div class="overview-icon orange"><i class="bi bi-exclamation-triangle"></i></div>
        <div><div class="overview-value">{{ abnormalCount }}</div><div class="overview-label">发现异常</div></div>
      </div>
      <div class="overview-card">
        <div class="overview-icon red"><i class="bi bi-percent"></i></div>
        <div><div class="overview-value">{{ progressPercent }}%</div><div class="overview-label">完成进度</div></div>
      </div>
    </div>

    <!-- 车间计划选择 -->
    <div class="workshop-tabs" v-if="plans.length > 0">
      <div v-for="plan in plans" :key="plan.id"
           class="workshop-tab" :class="{ active: selectedPlanId === plan.id }"
           @click="selectPlan(plan)">
        {{ plan.workshopName || ('第' + plan.workshopId + '车间') }}
        <span class="badge bg-light text-dark ms-1">{{ plan.checkedCount || 0 }}/{{ plan.totalCount || 0 }}</span>
      </div>
    </div>
    <div v-if="plans.length === 0 && !loading" class="text-center text-muted py-5">
      <i class="bi bi-calendar-x" style="font-size: 48px;"></i>
      <p class="mt-2">本周暂无分配的巡检任务</p>
    </div>

    <!-- 设备巡检卡片 -->
    <div class="equipment-grid" v-if="equipmentList.length > 0">
      <div v-for="eq in equipmentList" :key="eq.equipmentId" class="equip-card"
           :style="eq.hasAbnormal ? 'border-left: 3px solid #f59e0b' : (eq.repaired ? 'border-left: 3px solid #ef4444; opacity: 0.7' : '')">
        <div class="equip-card-header">
          <div>
            <div class="equip-name">
              <i class="bi bi-gear" :class="eq.repaired ? 'text-danger' : (eq.allChecked ? 'text-primary' : 'text-secondary')"></i>
              {{ eq.equipmentName }}
            </div>
            <div class="equip-location">{{ eq.equipmentLocation || '' }}</div>
          </div>
          <span v-if="eq.repaired" class="badge bg-danger">报修中</span>
          <span v-else-if="eq.allChecked && !eq.hasAbnormal" class="badge bg-success">已完成</span>
          <span v-else-if="eq.hasAbnormal" class="badge bg-warning text-dark">有异常</span>
          <span v-else class="badge bg-secondary">待巡检</span>
        </div>
        <div class="equip-card-body">
          <div v-for="record in eq.records" :key="record.id" class="check-item">
            <div class="check-label">
              <i :class="checkTypeIcon[record.checkType]"></i>
              {{ checkTypeMap[record.checkType] }}
            </div>
            <div class="check-btns">
              <span class="check-btn ok" :class="{ selected: record.result === 'normal', disabled: eq.repaired || eq.equipmentStatus === 'fault' }"
                    @click="setResult(record, 'normal', eq)">正常</span>
              <span class="check-btn warn" :class="{ selected: record.result === 'abnormal', disabled: eq.repaired || eq.equipmentStatus === 'fault' }"
                    @click="setResult(record, 'abnormal', eq)">异常</span>
              <span class="check-btn bad" :class="{ selected: record.result === 'fault', disabled: eq.repaired || eq.equipmentStatus === 'fault' }"
                    @click="setResult(record, 'fault', eq)">故障</span>
            </div>
          </div>
          <!-- 备注输入（有异常时显示） -->
          <div v-if="remarkRecordId && eq.records.some(r => r.id === remarkRecordId)" class="mt-2">
            <textarea class="remark-input" rows="2" v-model="remarkText" placeholder="请描述异常情况（必填）..."></textarea>
            <div class="d-flex justify-content-end gap-2 mt-1">
              <button class="btn btn-sm btn-secondary" @click="cancelRemark">取消</button>
              <button class="btn btn-sm btn-primary" @click="confirmRemark">确认</button>
            </div>
          </div>
        </div>
        <div class="equip-card-footer">
          <div class="equip-status">
            <span class="status-dot" :class="eq.repaired ? '' : (eq.allChecked ? 'done' : 'normal')"
                  :style="eq.repaired ? 'background:#ef4444' : ''"></span>
            {{ eq.repaired ? '已报修' : (eq.allChecked ? '巡检完成' : '待巡检') }}
          </div>
          <button v-if="eq.hasAbnormal && !eq.repaired" class="btn btn-outline-warning btn-sm"
                  @click="handleRepair(eq)">
            <i class="bi bi-send me-1"></i>转报修
          </button>
          <button v-if="eq.repaired" class="btn btn-outline-success btn-sm"
                  @click="handleRestore(eq)">
            <i class="bi bi-arrow-counterclockwise me-1"></i>恢复正常
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyInspectionPlans, getMyPlanRecords, updateInspectionRecord, repairFromInspection, restoreFromRepair } from '@/api/inspection'

const checkTypeMap = {
  appearance: '外观检查', running: '运行状态', temperature: '温度检测',
  noise: '噪音振动', electrical: '电气安全'
}
const checkTypeIcon = {
  appearance: 'bi bi-eye', running: 'bi bi-activity', temperature: 'bi bi-thermometer-half',
  noise: 'bi bi-soundwave', electrical: 'bi bi-lightning'
}

const loading = ref(false)
const plans = ref([])
const selectedPlanId = ref(null)
const equipmentList = ref([])
const remarkRecordId = ref(null)
const remarkText = ref('')
const pendingResult = ref(null)

// 周标签
const weekLabel = computed(() => {
  const now = new Date()
  const day = now.getDay()
  const monday = new Date(now)
  monday.setDate(now.getDate() - day + (day === 0 ? -6 : 1))
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  const fmt = d => `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')}`
  return `${fmt(monday)} ~ ${fmt(sunday)}`
})

// 统计
const totalEquipments = computed(() => plans.value.reduce((s, p) => s + (p.totalCount || 0), 0))
const checkedEquipments = computed(() => plans.value.reduce((s, p) => s + (p.checkedCount || 0), 0))
const abnormalCount = computed(() => equipmentList.value.filter(e => e.hasAbnormal).length)
const progressPercent = computed(() => totalEquipments.value > 0 ? Math.round(checkedEquipments.value / totalEquipments.value * 100) : 0)

async function loadPlans() {
  loading.value = true
  try {
    const res = await getMyInspectionPlans()
    if (res && res.code === 200) {
      plans.value = res.data || []
      if (plans.value.length > 0 && !selectedPlanId.value) {
        selectPlan(plans.value[0])
      }
    }
  } catch (e) {
    console.error('加载巡检计划失败', e)
    ElMessage.error('加载巡检计划失败')
  }
  loading.value = false
}

async function selectPlan(plan) {
  selectedPlanId.value = plan.id
  try {
    const res = await getMyPlanRecords(plan.id)
    if (res && res.code === 200) {
      equipmentList.value = res.data || []
    }
  } catch (e) {
    console.error('加载巡检记录失败', e)
  }
}

function setResult(record, result, eq) {
  if (eq.repaired || eq.equipmentStatus === 'fault') return
  if (result === 'abnormal' || result === 'fault') {
    remarkRecordId.value = record.id
    remarkText.value = record.remark || ''
    pendingResult.value = { record, result, eq }
  } else {
    doUpdate(record.id, result, '')
  }
}

function cancelRemark() {
  remarkRecordId.value = null
  remarkText.value = ''
  pendingResult.value = null
}

async function confirmRemark() {
  if (!remarkText.value.trim()) {
    ElMessage.warning('异常或故障必须填写备注')
    return
  }
  if (pendingResult.value) {
    await doUpdate(pendingResult.value.record.id, pendingResult.value.result, remarkText.value)
    remarkRecordId.value = null
    remarkText.value = ''
    pendingResult.value = null
  }
}

async function doUpdate(recordId, result, remark) {
  try {
    const res = await updateInspectionRecord(recordId, { result, remark })
    if (res && res.code === 200) {
      ElMessage.success('检查结果已更新')
      // 刷新当前计划数据
      if (selectedPlanId.value) {
        const plan = plans.value.find(p => p.id === selectedPlanId.value)
        if (plan) await selectPlan(plan)
      }
      // 刷新计划列表（更新进度）
      await loadPlans()
    }
  } catch (e) {
    console.error('更新失败', e)
    ElMessage.error(e.response?.data?.message || '更新检查结果失败')
  }
}

async function handleRepair(eq) {
  try {
    await ElMessageBox.confirm(`确定将 ${eq.equipmentName} 转为报修？转报修后该设备将标记为故障状态。`, '确认转报修', { type: 'warning' })
    // 找到第一条异常/故障记录
    const abnormalRecord = eq.records.find(r => r.result === 'abnormal' || r.result === 'fault')
    if (!abnormalRecord) return
    const res = await repairFromInspection(abnormalRecord.id)
    if (res && res.code === 200) {
      ElMessage.success('已转报修')
      if (selectedPlanId.value) {
        const plan = plans.value.find(p => p.id === selectedPlanId.value)
        if (plan) await selectPlan(plan)
      }
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('转报修失败', e)
      ElMessage.error('转报修失败')
    }
  }
}

async function handleRestore(eq) {
  try {
    await ElMessageBox.confirm(`确定将 ${eq.equipmentName} 恢复为正常状态？`, '确认恢复', { type: 'info' })
    const res = await restoreFromRepair(selectedPlanId.value, eq.equipmentId)
    if (res && res.code === 200) {
      ElMessage.success('已恢复正常')
      if (selectedPlanId.value) {
        const plan = plans.value.find(p => p.id === selectedPlanId.value)
        if (plan) await selectPlan(plan)
      }
      await loadPlans()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('恢复失败', e)
      ElMessage.error('恢复失败')
    }
  }
}

onMounted(() => { loadPlans() })
</script>

<style lang="scss" scoped>
.inspection-page { padding: 24px; background: #f1f5f9; min-height: 100vh; }

.page-header {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;
  h5 { color: #1e293b; margin: 0; }
  .text-muted { color: #94a3b8 !important; }
}

.overview-cards {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 20px;
}
.overview-card {
  background: #fff; border-radius: 12px; padding: 18px;
  display: flex; align-items: center; gap: 14px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.overview-icon {
  width: 46px; height: 46px; border-radius: 12px; display: flex;
  align-items: center; justify-content: center; font-size: 20px;
  &.blue { background: rgba(59,130,246,0.1); color: #3b82f6; }
  &.green { background: rgba(34,197,94,0.1); color: #22c55e; }
  &.orange { background: rgba(245,158,11,0.1); color: #f59e0b; }
  &.red { background: rgba(239,68,68,0.1); color: #ef4444; }
}
.overview-value { font-size: 22px; font-weight: 700; color: #1e293b; }
.overview-label { font-size: 12px; color: #94a3b8; }

.workshop-tabs {
  display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap;
}
.workshop-tab {
  padding: 8px 18px; border-radius: 20px; border: 1px solid #e2e8f0;
  background: #fff; cursor: pointer; font-size: 13px;
  color: #64748b; transition: all 0.2s;
  &:hover { border-color: #3b82f6; color: #3b82f6; }
  &.active { background: #3b82f6; color: #fff; border-color: #3b82f6; }
}

.equipment-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(360px, 1fr)); gap: 16px;
}
.equip-card {
  background: #fff; border-radius: 12px; overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1); transition: all 0.2s;
  &:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
}
.equip-card-header {
  padding: 14px 18px; display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid #f1f5f9;
}
.equip-name { font-weight: 600; font-size: 14px; color: #1e293b; display: flex; align-items: center; gap: 8px; }
.equip-location { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.equip-card-body { padding: 12px 18px; }

.check-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 0; border-bottom: 1px solid #f1f5f9;
  &:last-child { border-bottom: none; }
}
.check-label {
  font-size: 13px; color: #475569; display: flex; align-items: center; gap: 8px;
  i { color: #94a3b8; font-size: 15px; }
}

.check-btns { display: flex; gap: 5px; }
.check-btn {
  padding: 3px 12px; border-radius: 14px; font-size: 12px;
  border: 1px solid #e2e8f0; background: transparent; cursor: pointer; transition: all 0.2s;
  &.ok { color: #22c55e; &.selected { background: #22c55e; color: #fff; border-color: #22c55e; } }
  &.warn { color: #f59e0b; &.selected { background: #f59e0b; color: #fff; border-color: #f59e0b; } }
  &.bad { color: #ef4444; &.selected { background: #ef4444; color: #fff; border-color: #ef4444; } }
  &.disabled { opacity: 0.4; pointer-events: none; }
}

.equip-card-footer {
  padding: 10px 18px; background: #f8fafc;
  display: flex; justify-content: space-between; align-items: center;
}
.equip-status { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #64748b; }
.status-dot {
  width: 8px; height: 8px; border-radius: 50%;
  &.normal { background: #22c55e; }
  &.done { background: #3b82f6; }
}

.remark-input {
  width: 100%; border: 1px solid #e2e8f0; border-radius: 8px;
  padding: 8px 12px; font-size: 13px; resize: none; outline: none;
  background: #f8fafc; color: #1e293b;
  &:focus { border-color: #3b82f6; }
  &::placeholder { color: #94a3b8; }
}

@media (max-width: 1200px) {
  .overview-cards { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .overview-cards { grid-template-columns: 1fr; }
  .equipment-grid { grid-template-columns: 1fr; }
}
</style>
