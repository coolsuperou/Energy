<template>
  <div class="order-page">
    <!-- 周选择器 -->
    <div class="card filter-card">
      <div class="card-body">
        <div class="filter-row">
          <el-button @click="changeWeek(-1)"><i class="bi bi-chevron-left"></i> 上一周</el-button>
          <span class="week-label">{{ weekLabel }}</span>
          <el-button @click="changeWeek(1)">下一周 <i class="bi bi-chevron-right"></i></el-button>
          <el-button type="info" @click="goThisWeek">本周</el-button>
          <div class="filter-spacer"></div>
          <el-button type="warning" @click="handleAutoSchedule"><i class="bi bi-magic"></i> 自动排班</el-button>
          <el-button type="primary" @click="showAddDialog"><i class="bi bi-plus-lg"></i> 添加排班</el-button>
        </div>
      </div>
    </div>

    <!-- 排班列表 -->
    <div class="card">
      <el-table :data="plans" v-loading="loading" stripe>
        <el-table-column prop="inspectorName" label="巡检员" width="120" />
        <el-table-column prop="workshopName" label="负责车间" width="140" />
        <el-table-column label="完成进度" width="200">
          <template #default="{ row }">
            <div class="d-flex align-items-center gap-2">
              <el-progress 
                :percentage="row.totalCount > 0 ? Math.round(row.checkedCount / row.totalCount * 100) : 0" 
                :stroke-width="10"
                style="flex: 1"
              />
              <span class="text-muted small">{{ row.checkedCount || 0 }}/{{ row.totalCount || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'completed' ? 'success' : 'warning'" size="small">
              {{ row.status === 'completed' ? '已完成' : '待完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewRecords(row)">查看详情</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" :disabled="row.status === 'completed'">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="plans.length === 0 && !loading" class="text-center text-muted py-4">
        本周暂无排班数据，请添加排班或使用自动排班
      </div>
    </div>

    <!-- 添加排班对话框 -->
    <el-dialog v-model="addDialog.visible" title="添加排班" width="450px">
      <el-form label-width="80px">
        <el-form-item label="巡检员" required>
          <el-select v-model="addDialog.inspectorId" placeholder="请选择巡检员" style="width: 100%">
            <el-option v-for="item in inspectors" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="车间" required>
          <el-select v-model="addDialog.workshopId" placeholder="请选择车间" style="width: 100%">
            <el-option v-for="item in workshops" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排班周">
          <span>{{ weekLabel }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdd">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 巡检记录详情对话框 -->
    <el-dialog v-model="detailDialog.visible" :title="'巡检详情 - ' + (detailDialog.plan?.workshopName || '')" width="750px">
      <el-table :data="detailDialog.records" stripe size="small">
        <el-table-column prop="equipmentName" label="设备名称" width="140" />
        <el-table-column prop="equipmentLocation" label="设备位置" width="140" />
        <el-table-column label="检查类型" width="100">
          <template #default="{ row }">{{ checkTypeMap[row.checkType] || row.checkType }}</template>
        </el-table-column>
        <el-table-column label="检查结果" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.result" :type="resultTagType(row.result)" size="small">{{ resultText(row.result) }}</el-tag>
            <span v-else class="text-muted">未检查</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="转报修" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.repaired" type="danger" size="small">已报修</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="detailDialog.visible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getInspectionPlans, addInspectionPlan, deleteInspectionPlan,
  autoScheduleInspection, getInspectionPlanRecords,
  getInspectorList, getWorkshopList
} from '@/api/inspection'

// 检查类型映射
const checkTypeMap = {
  appearance: '外观检查',
  running: '运行状态',
  temperature: '温度检测',
  noise: '噪音振动',
  electrical: '电气安全'
}

// 当前周一日期
const currentWeekStart = ref(getMonday(new Date()))
const plans = ref([])
const loading = ref(false)
const inspectors = ref([])
const workshops = ref([])

// 周标签
const weekLabel = computed(() => {
  const start = new Date(currentWeekStart.value)
  const end = new Date(start)
  end.setDate(end.getDate() + 6)
  return `${formatDate(start)} ~ ${formatDate(end)}`
})

// 添加排班对话框
const addDialog = ref({ visible: false, inspectorId: null, workshopId: null })

// 详情对话框
const detailDialog = ref({ visible: false, plan: null, records: [] })

function getMonday(date) {
  const d = new Date(date)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1)
  d.setDate(diff)
  return d.toISOString().split('T')[0]
}

function formatDate(date) {
  const m = (date.getMonth() + 1).toString().padStart(2, '0')
  const d = date.getDate().toString().padStart(2, '0')
  return `${date.getFullYear()}-${m}-${d}`
}

function changeWeek(offset) {
  const d = new Date(currentWeekStart.value)
  d.setDate(d.getDate() + offset * 7)
  currentWeekStart.value = d.toISOString().split('T')[0]
  loadPlans()
}

function goThisWeek() {
  currentWeekStart.value = getMonday(new Date())
  loadPlans()
}

async function loadPlans() {
  loading.value = true
  try {
    const res = await getInspectionPlans(currentWeekStart.value)
    if (res && res.code === 200) {
      plans.value = res.data || []
    }
  } catch (e) {
    console.error('加载排班失败', e)
    ElMessage.error('加载排班数据失败')
  }
  loading.value = false
}

async function loadBaseData() {
  try {
    const [insRes, wsRes] = await Promise.all([getInspectorList(), getWorkshopList()])
    if (insRes && insRes.code === 200) inspectors.value = insRes.data || []
    if (wsRes && wsRes.code === 200) workshops.value = wsRes.data || []
  } catch (e) {
    console.error('加载基础数据失败', e)
  }
}

function showAddDialog() {
  addDialog.value = { visible: true, inspectorId: null, workshopId: null }
}

async function confirmAdd() {
  if (!addDialog.value.inspectorId || !addDialog.value.workshopId) {
    ElMessage.warning('请选择巡检员和车间')
    return
  }
  try {
    const res = await addInspectionPlan({
      inspectorId: addDialog.value.inspectorId,
      workshopId: addDialog.value.workshopId,
      weekStart: currentWeekStart.value
    })
    if (res && res.code === 200) {
      ElMessage.success('排班添加成功')
      addDialog.value.visible = false
      loadPlans()
    }
  } catch (e) {
    console.error('添加排班失败', e)
    ElMessage.error(e.response?.data?.message || '添加排班失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该排班记录？关联的巡检记录也会被删除。', '确认删除', { type: 'warning' })
    const res = await deleteInspectionPlan(row.id)
    if (res && res.code === 200) {
      ElMessage.success('删除成功')
      loadPlans()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
      ElMessage.error('删除失败')
    }
  }
}

async function handleAutoSchedule() {
  try {
    if (plans.value.length > 0) {
      await ElMessageBox.confirm('当前周已有排班记录，自动排班将覆盖现有数据，是否继续？', '确认自动排班', { type: 'warning' })
    }
    const res = await autoScheduleInspection(currentWeekStart.value)
    if (res && res.code === 200) {
      ElMessage.success('自动排班完成')
      loadPlans()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error('自动排班失败', e)
      ElMessage.error(e.response?.data?.message || '自动排班失败')
    }
  }
}

async function viewRecords(row) {
  try {
    const res = await getInspectionPlanRecords(row.id)
    if (res && res.code === 200) {
      detailDialog.value = { visible: true, plan: row, records: res.data || [] }
    }
  } catch (e) {
    console.error('加载详情失败', e)
  }
}

function resultTagType(result) {
  const map = { normal: 'success', abnormal: 'warning', fault: 'danger' }
  return map[result] || 'info'
}

function resultText(result) {
  const map = { normal: '正常', abnormal: '异常', fault: '故障' }
  return map[result] || result
}

onMounted(() => {
  loadPlans()
  loadBaseData()
})
</script>

<style lang="scss">
@use '@/styles/dispatcher.scss';

.week-label {
  font-size: 16px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 12px;
}
</style>
