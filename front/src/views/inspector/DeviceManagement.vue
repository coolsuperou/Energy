<template>
  <div class="device-manage-page">
    <div class="page-head">
      <div>
        <h4><i class="bi bi-hdd-network me-2 text-primary"></i>设备管理</h4>
      </div>
      <div class="d-flex flex-wrap gap-2 align-items-start">
        <button type="button" class="btn btn-outline-primary btn-sm" @click="exportDeviceList">
          <i class="bi bi-download me-1"></i>导出
        </button>
        <button type="button" class="btn btn-primary btn-sm" @click="openCreate">
          <i class="bi bi-plus-lg me-1"></i>新增设备
        </button>
      </div>
    </div>

    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-icon total"><i class="bi bi-hdd-stack"></i></div>
        <div>
          <div class="stat-num">{{ stats.total }}</div>
          <div class="stat-label">设备总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon ok"><i class="bi bi-check-circle"></i></div>
        <div>
          <div class="stat-num">{{ stats.normal }}</div>
          <div class="stat-label">正常运行</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon maint"><i class="bi bi-tools"></i></div>
        <div>
          <div class="stat-num">{{ stats.warning }}</div>
          <div class="stat-label">维护中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon fault"><i class="bi bi-exclamation-triangle"></i></div>
        <div>
          <div class="stat-num">{{ stats.fault }}</div>
          <div class="stat-label">故障</div>
        </div>
      </div>
    </div>

    <div class="filter-card">
      <div class="row g-3 align-items-end">
        <div class="col-lg-4 col-md-6">
          <div class="filter-field filter-field-workshop">
            <label class="compact-label">车间</label>
            <select v-model="filterWorkshop" class="form-select form-select-sm filter-control">
              <option value="">全部车间</option>
              <option v-for="w in workshopOptions" :key="w" :value="String(w)">{{ workshopLabel(w) }}</option>
            </select>
          </div>
        </div>
        <div class="col-lg-3 col-md-6">
          <div class="filter-field">
            <label class="compact-label">关键词</label>
            <input
              v-model="filterKeyword"
              type="text"
              class="form-control form-control-sm filter-control"
              placeholder="名称、型号或位置"
            />
          </div>
        </div>
        <div class="col-lg-2 col-md-6">
          <div class="filter-field">
            <label class="compact-label">设备状态</label>
            <select v-model="filterStatus" class="form-select form-select-sm filter-control">
              <option value="">全部</option>
              <option value="normal">正常</option>
              <option value="warning">维护中</option>
              <option value="fault">故障</option>
            </select>
          </div>
        </div>
        <div class="col-lg-3 col-md-6">
          <div class="filter-field">
            <label class="compact-label">排序</label>
            <select v-model="filterSort" class="form-select form-select-sm filter-control">
              <option value="created">最近创建</option>
              <option value="name">名称</option>
              <option value="status">状态优先（故障→维护→正常）</option>
            </select>
          </div>
        </div>
        <div class="col-12 col-lg-auto d-flex gap-2 filter-field-actions">
          <button type="button" class="btn btn-outline-secondary btn-sm px-4 filter-action-btn" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div class="table-card" v-loading="loading">
      <div class="card-head">
        <h5>设备列表</h5>
        <button type="button" class="btn btn-outline-secondary btn-sm" @click="loadData">
          <i class="bi bi-arrow-clockwise"></i> 刷新
        </button>
      </div>
      <div v-if="displayedRows.length === 0" class="empty-card">
        <i class="bi bi-inbox"></i>
        <div>暂无符合条件的设备</div>
      </div>
      <div v-else class="table-responsive">
        <table class="table table-hover mb-0">
          <thead>
            <tr>
              <th>车间内ID</th>
              <th>设备名称</th>
              <th>型号</th>
              <th>车间</th>
              <th>额定功率 (kW)</th>
              <th>位置</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in displayedRows" :key="row.id">
              <td class="fw-semibold">{{ workshopLocalId(row) }}</td>
              <td>
                <div class="device-title">{{ row.name }}</div>
              </td>
              <td>{{ row.model || '-' }}</td>
              <td>{{ workshopLabel(row.workshopId) }}</td>
              <td>{{ formatPower(row.ratedPower) }}</td>
              <td>{{ row.location || '-' }}</td>
              <td>
                <span :class="['badge', 'badge-status', statusBadgeClass(row)]">{{ statusText(normStatus(row.status)) }}</span>
              </td>
              <td class="text-muted small">{{ formatDateTime(row.createdAt) }}</td>
              <td>
                <div class="d-flex flex-wrap gap-1">
                  <button type="button" class="btn-action primary-outline" @click="openDetail(row)">详情</button>
                  <button type="button" class="btn-action" @click="openEdit(row)">编辑</button>
                  <button type="button" class="btn-action btn-action-danger" @click="confirmDelete(row)">删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination-bar">
        <span>共 {{ displayedRows.length }} 条</span>
      </div>
    </div>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="设备详情" width="520px" destroy-on-close align-center>
      <dl v-if="detailRow" class="row mb-0 small inspector-device-detail-dl">
        <template v-for="item in detailFields" :key="item.key">
          <dt class="col-sm-4 text-muted">{{ item.label }}</dt>
          <dd class="col-sm-8">
            <span v-if="item.key === 'status'" :class="['badge', 'badge-status', statusBadgeClass(detailRow)]">
              {{ statusText(normStatus(detailRow.status)) }}
            </span>
            <template v-else>{{ item.value }}</template>
          </dd>
        </template>
      </dl>
    </el-dialog>

    <!-- 新增 / 编辑 -->
    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? '新增设备' : '编辑设备'"
      width="640px"
      destroy-on-close
      align-center
      @closed="resetFormModel"
    >
      <el-form ref="formRef" :model="formModel" :rules="formRules" label-width="100px" label-position="right">
        <el-form-item label="设备名称" prop="name">
          <el-input v-model="formModel.name" placeholder="必填" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="所属车间" prop="workshopId">
          <el-select v-model="formModel.workshopId" placeholder="请选择车间" class="w-100">
            <el-option v-for="w in formWorkshopOptions" :key="w" :label="workshopLabel(w)" :value="w" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备型号" prop="model">
          <el-input v-model="formModel.model" placeholder="可选" maxlength="100" />
        </el-form-item>
        <el-form-item label="设备状态" prop="status">
          <el-select v-model="formModel.status" class="w-100">
            <el-option label="正常" value="NORMAL" />
            <el-option label="维护中" value="WARNING" />
            <el-option label="故障" value="FAULT" />
          </el-select>
        </el-form-item>
        <el-form-item label="额定功率" prop="ratedPower">
          <el-input-number v-model="formModel.ratedPower" :min="0" :precision="2" :step="0.5" class="w-100" controls-position="right" />
          <span class="ms-2 text-muted small">kW</span>
        </el-form-item>
        <el-form-item label="安装位置" prop="location">
          <el-input v-model="formModel.location" placeholder="可选" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSubmitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import { getEquipments, getEquipmentById, createEquipment, updateEquipment, deleteEquipment } from '@/api/equipment'

const CN_NUM = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十']

const loading = ref(false)
const rawList = ref([])

/** 默认筛选第一车间 */
const filterWorkshop = ref('1')
const filterKeyword = ref('')
const filterStatus = ref('')
const filterSort = ref('created')

const workshopOptions = computed(() => {
  const set = new Set()
  for (const e of rawList.value) {
    if (e.workshopId != null) set.add(Number(e.workshopId))
  }
  return Array.from(set).sort((a, b) => a - b)
})

const formWorkshopOptions = computed(() => {
  const fromData = workshopOptions.value
  if (fromData.length) return fromData
  return [1, 2, 3, 4]
})

function workshopLabel(id) {
  if (id == null || id === '') return '-'
  const n = Number(id)
  if (n >= 1 && n <= 10) return `第${CN_NUM[n - 1]}车间`
  return `车间 ${id}`
}

/**
 * 统一为 normal | warning | fault（与库中枚举存储一致）
 */
function normStatus(s) {
  if (s == null || s === '') return 'normal'
  let v = typeof s === 'string' ? s : s.value || s.name || ''
  v = String(v).toLowerCase()
  if (v === 'maintenance') return 'warning'
  if (['normal', 'warning', 'fault'].includes(v)) return v
  const up = String(v).toUpperCase()
  if (up === 'NORMAL') return 'normal'
  if (up === 'WARNING') return 'warning'
  if (up === 'FAULT') return 'fault'
  return 'normal'
}

function statusText(code) {
  const map = { normal: '正常', warning: '维护中', fault: '故障' }
  return map[code] || code
}

/** 与设计稿一致：正常=绿、维护中=黄、故障=红 */
function statusBadgeClass(rowOrStatus) {
  const code = typeof rowOrStatus === 'object' && rowOrStatus !== null ? normStatus(rowOrStatus.status) : normStatus(rowOrStatus)
  if (code === 'normal') return 'bg-success'
  if (code === 'warning') return 'bg-warning text-dark'
  if (code === 'fault') return 'bg-danger'
  return 'bg-secondary'
}

function formatPower(p) {
  if (p == null || p === '') return '-'
  return String(p)
}

function formatDateTime(val) {
  if (!val) return '-'
  if (typeof val === 'string') return val.substring(0, 16).replace('T', ' ')
  if (Array.isArray(val) && val.length >= 5) {
    const [y, m, d, h, mi] = val
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(mi).padStart(2, '0')}`
  }
  return String(val)
}

const displayedRows = computed(() => {
  let rows = [...rawList.value]

  if (filterWorkshop.value !== '' && filterWorkshop.value != null) {
    const wid = Number(filterWorkshop.value)
    rows = rows.filter((r) => Number(r.workshopId) === wid)
  }

  if (filterStatus.value) {
    rows = rows.filter((r) => normStatus(r.status) === filterStatus.value)
  }

  const kw = (filterKeyword.value || '').trim().toLowerCase()
  if (kw) {
    rows = rows.filter((r) => {
      const blob = `${r.name || ''} ${r.model || ''} ${r.location || ''}`.toLowerCase()
      return blob.includes(kw)
    })
  }

  const statusOrder = { fault: 0, warning: 1, normal: 2 }
  if (filterSort.value === 'name') {
    rows.sort((a, b) => (a.name || '').localeCompare(b.name || '', 'zh-CN'))
  } else if (filterSort.value === 'created') {
    rows.sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')))
  } else if (filterSort.value === 'status') {
    rows.sort((a, b) => {
      const sa = normStatus(a.status)
      const sb = normStatus(b.status)
      const d = (statusOrder[sa] ?? 9) - (statusOrder[sb] ?? 9)
      return d !== 0 ? d : (a.name || '').localeCompare(b.name || '', 'zh-CN')
    })
  }

  return rows
})

/**
 * 车间内ID：按当前列表展示顺序，在同车间内依次为 1、2、3…
 *（随排序/筛选变化；新增并刷新后「最近创建」下首行即为 1）
 */
const workshopLocalIdSeqMap = computed(() => {
  const map = new Map()
  const rows = displayedRows.value
  const byWsOrder = new Map()
  for (const r of rows) {
    const w = Number(r.workshopId)
    if (!Number.isFinite(w)) continue
    if (!byWsOrder.has(w)) byWsOrder.set(w, [])
    byWsOrder.get(w).push(r.id)
  }
  for (const ids of byWsOrder.values()) {
    ids.forEach((id, i) => map.set(id, i + 1))
  }
  return map
})

function workshopLocalId(row) {
  if (!row || row.id == null) return '-'
  const n = workshopLocalIdSeqMap.value.get(row.id)
  return n != null ? n : '-'
}

const stats = computed(() => {
  const rows = displayedRows.value
  let normal = 0
  let warning = 0
  let fault = 0
  for (const r of rows) {
    const s = normStatus(r.status)
    if (s === 'normal') normal++
    else if (s === 'warning') warning++
    else if (s === 'fault') fault++
  }
  return { total: rows.length, normal, warning, fault }
})

function resetFilters() {
  filterWorkshop.value = '1'
  filterKeyword.value = ''
  filterStatus.value = ''
  filterSort.value = 'created'
}

const detailVisible = ref(false)
const detailRow = ref(null)

const detailFields = computed(() => {
  const r = detailRow.value
  if (!r) return []
  return [
    { key: 'localId', label: '车间内ID', value: workshopLocalId(r) },
    { key: 'id', label: '系统编号', value: r.id },
    { key: 'name', label: '设备名称', value: r.name },
    { key: 'workshop', label: '所属车间', value: workshopLabel(r.workshopId) },
    { key: 'model', label: '型号', value: r.model || '-' },
    { key: 'ratedPower', label: '额定功率 (kW)', value: formatPower(r.ratedPower) },
    { key: 'location', label: '安装位置', value: r.location || '-' },
    { key: 'status', label: '状态', value: null },
    { key: 'createdAt', label: '创建时间', value: formatDateTime(r.createdAt) }
  ]
})

async function openDetail(row) {
  try {
    const res = await getEquipmentById(row.id)
    if (res && res.code === 200 && res.data) {
      detailRow.value = res.data
    } else {
      detailRow.value = row
    }
  } catch {
    detailRow.value = row
  }
  detailVisible.value = true
}

const formVisible = ref(false)
const formMode = ref('create')
const formSubmitting = ref(false)
const formRef = ref(null)
const editingId = ref(null)

const formModel = ref({
  name: '',
  workshopId: 1,
  model: '',
  status: 'NORMAL',
  ratedPower: 0,
  location: ''
})

const formRules = {
  name: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  workshopId: [{ required: true, message: '请选择车间', trigger: 'change' }],
  ratedPower: [
    { required: true, message: '请输入额定功率', trigger: 'blur' },
    {
      validator: (_r, v, cb) => {
        if (v == null || Number(v) < 0) cb(new Error('额定功率需为非负数'))
        else cb()
      },
      trigger: 'blur'
    }
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function resetFormModel() {
  editingId.value = null
  formModel.value = {
    name: '',
    workshopId: formWorkshopOptions.value[0] ?? 1,
    model: '',
    status: 'NORMAL',
    ratedPower: 0,
    location: ''
  }
  formRef.value?.resetFields?.()
}

function openCreate() {
  formMode.value = 'create'
  resetFormModel()
  formVisible.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  editingId.value = row.id
  formModel.value = {
    name: row.name || '',
    workshopId: Number(row.workshopId) || 1,
    model: row.model || '',
    status: (() => {
      const c = normStatus(row.status)
      if (c === 'fault') return 'FAULT'
      if (c === 'warning') return 'WARNING'
      return 'NORMAL'
    })(),
    ratedPower: row.ratedPower != null ? Number(row.ratedPower) : 0,
    location: row.location || ''
  }
  formVisible.value = true
}

function buildPayload() {
  return {
    name: formModel.value.name.trim(),
    workshopId: formModel.value.workshopId,
    ratedPower: formModel.value.ratedPower,
    status: formModel.value.status,
    model: formModel.value.model?.trim() || null,
    location: formModel.value.location?.trim() || null
  }
}

async function submitForm() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  formSubmitting.value = true
  try {
    const payload = buildPayload()
    if (formMode.value === 'create') {
      const res = await createEquipment(payload)
      if (res && res.code === 200) {
        ElMessage.success('已添加设备')
        formVisible.value = false
        await loadData()
      }
    } else {
      const res = await updateEquipment(editingId.value, payload)
      if (res && res.code === 200) {
        ElMessage.success('已保存修改')
        formVisible.value = false
        await loadData()
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    formSubmitting.value = false
  }
}

function confirmDelete(row) {
  ElMessageBox.confirm(`确定删除设备「${row.name}」吗？若仍有关联申请或巡检记录，删除将失败。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      try {
        const res = await deleteEquipment(row.id)
        if (res && res.code === 200) {
          ElMessage.success('已删除')
          await loadData()
        }
      } catch (e) {
        console.error(e)
      }
    })
    .catch(() => {})
}

async function loadData() {
  loading.value = true
  try {
    const res = await getEquipments()
    if (res && res.code === 200) {
      rawList.value = Array.isArray(res.data) ? res.data : []
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载设备列表失败')
  } finally {
    loading.value = false
  }
}

/** 导出当前筛选、排序后的设备列表（与表格一致，不含操作列） */
function exportDeviceList() {
  const rows = displayedRows.value
  if (!rows.length) {
    ElMessage.warning('暂无设备数据可导出')
    return
  }

  const exportData = rows.map((row) => ({
    车间内ID: workshopLocalId(row),
    设备名称: row.name || '',
    型号: row.model || '-',
    车间: workshopLabel(row.workshopId),
    '额定功率(kW)': formatPower(row.ratedPower),
    位置: row.location || '-',
    状态: statusText(normStatus(row.status)),
    创建时间: formatDateTime(row.createdAt)
  }))

  const wb = XLSX.utils.book_new()
  const ws = XLSX.utils.json_to_sheet(exportData)
  ws['!cols'] = [
    { wch: 10 },
    { wch: 18 },
    { wch: 14 },
    { wch: 12 },
    { wch: 14 },
    { wch: 22 },
    { wch: 10 },
    { wch: 18 }
  ]
  XLSX.utils.book_append_sheet(wb, ws, '设备列表')

  const dateStr = new Date().toISOString().split('T')[0]
  XLSX.writeFile(wb, `设备列表_${dateStr}.xlsx`)
  ElMessage.success('已导出')
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss">
@use '@/styles/inspector.scss';
</style>
