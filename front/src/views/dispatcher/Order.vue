<template>
  <div class="order-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon orange"><i class="bi bi-hourglass-split"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.pending }}</div>
          <div class="stat-label">待处理工单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon blue"><i class="bi bi-gear"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.processing }}</div>
          <div class="stat-label">处理中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><i class="bi bi-check-circle"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.completed }}</div>
          <div class="stat-label">本月已完成</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><i class="bi bi-graph-up"></i></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.onTimeRate }}%</div>
          <div class="stat-label">按时完成率</div>
        </div>
      </div>
    </div>

    <!-- 筛选和操作 -->
    <div class="card filter-card">
      <div class="card-body">
        <div class="filter-row">
          <el-radio-group v-model="filter.status" @change="loadOrders">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="pending">待派单</el-radio-button>
            <el-radio-button label="processing">处理中</el-radio-button>
            <el-radio-button label="completed">已完成</el-radio-button>
          </el-radio-group>
          <el-select v-model="filter.type" placeholder="工单类型" clearable style="width: 150px" @change="loadOrders">
            <el-option label="故障维修" value="repair" />
            <el-option label="设备巡检" value="inspection" />
            <el-option label="预防维护" value="maintenance" />
          </el-select>
          <div class="filter-spacer"></div>
          <el-input v-model="filter.keyword" placeholder="搜索工单号..." style="width: 200px" clearable>
            <template #append>
              <el-button @click="loadOrders"><i class="bi bi-search"></i></el-button>
            </template>
          </el-input>
          <el-button type="primary" @click="showCreateDialog"><i class="bi bi-plus-lg"></i> 创建工单</el-button>
        </div>
      </div>
    </div>

    <!-- 工单列表 -->
    <div class="card">
      <el-table :data="filteredOrders" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="工单号" width="160">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)" size="small">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" width="180" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="assignee" label="处理人" width="100">
          <template #default="{ row }">
            {{ row.assignee || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span :class="['status-badge', 'status-' + row.status]">{{ getStatusText(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" type="primary" size="small" @click="assignOrder(row)">派单</el-button>
            <el-button size="small" @click="viewOrder(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination 
        v-model:current-page="pagination.page" 
        v-model:page-size="pagination.size"
        :total="pagination.total" 
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </div>

    <!-- 创建工单对话框 -->
    <el-dialog v-model="createDialog.visible" title="创建工单" width="500px">
      <el-form :model="createDialog.form" label-width="100px">
        <el-form-item label="工单类型" required>
          <el-select v-model="createDialog.form.type" placeholder="请选择" style="width: 100%">
            <el-option label="故障维修" value="repair" />
            <el-option label="设备巡检" value="inspection" />
            <el-option label="预防维护" value="maintenance" />
          </el-select>
        </el-form-item>
        <el-form-item label="位置" required>
          <el-input v-model="createDialog.form.location" placeholder="如：生产一车间-配电室" />
        </el-form-item>
        <el-form-item label="问题描述" required>
          <el-input v-model="createDialog.form.description" type="textarea" :rows="3" placeholder="请描述问题" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="createDialog.form.priority" style="width: 100%">
            <el-option label="普通" value="normal" />
            <el-option label="紧急" value="urgent" />
            <el-option label="非常紧急" value="critical" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitOrder">创建</el-button>
      </template>
    </el-dialog>

    <!-- 派单对话框 -->
    <el-dialog v-model="assignDialog.visible" title="派单" width="400px">
      <el-form label-width="80px">
        <el-form-item label="工单号">
          <span>{{ assignDialog.order?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="工单类型">
          <el-tag :type="getTypeTagType(assignDialog.order?.type)" size="small">{{ getTypeText(assignDialog.order?.type) }}</el-tag>
        </el-form-item>
        <el-form-item label="位置">
          <span>{{ assignDialog.order?.location || '-' }}</span>
        </el-form-item>
        <el-form-item label="处理人" required>
          <el-select v-model="assignDialog.assigneeId" placeholder="请选择巡检员" style="width: 100%">
            <el-option 
              v-for="inspector in inspectors" 
              :key="inspector.id" 
              :label="inspector.name"
              :value="inspector.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="assignDialog.remark" type="textarea" :rows="2" placeholder="可选，填写派单备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmAssign">确认派单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTasks, createTask, assignTask } from '@/api/task'
import { getUsers } from '@/api/user'

// 统计数据
const stats = ref({
  pending: 12,
  processing: 8,
  completed: 156,
  onTimeRate: 98.5
})

// 筛选条件
const filter = ref({
  status: '',
  type: '',
  keyword: ''
})

// 分页
const pagination = ref({
  page: 1,
  size: 10,
  total: 0
})

// 加载状态
const loading = ref(false)

// 工单列表
const orders = ref([])

// 过滤后的工单
const filteredOrders = computed(() => {
  let result = orders.value
  
  if (filter.value.status) {
    result = result.filter(o => o.status === filter.value.status)
  }
  if (filter.value.type) {
    result = result.filter(o => o.type === filter.value.type)
  }
  if (filter.value.keyword) {
    result = result.filter(o => o.orderNo.includes(filter.value.keyword))
  }
  
  pagination.value.total = result.length
  return result
})

// 创建工单对话框
const createDialog = ref({
  visible: false,
  form: {
    type: '',
    location: '',
    description: '',
    priority: 'normal'
  }
})

// 派单对话框
const assignDialog = ref({
  visible: false,
  order: null,
  assigneeId: null,
  remark: ''
})

// 巡检员列表
const inspectors = ref([])

// 加载工单列表
async function loadOrders() {
  loading.value = true
  try {
    const params = {
      page: 1,
      size: 100
    }
    
    if (filter.value.status) {
      params.status = filter.value.status.toUpperCase()
    }
    if (filter.value.type) {
      params.type = filter.value.type.toUpperCase()
    }
    
    const res = await getTasks(params)
    if (res && res.code === 200 && res.data) {
      const data = res.data.records || res.data || []
      orders.value = data.map(task => ({
        id: task.id,
        orderNo: `WO${task.id.toString().padStart(10, '0')}`,
        type: (task.taskType || '').toLowerCase(),
        location: task.description || '',
        description: task.title || '',
        assignee: task.assigneeName || '',
        status: (task.status || '').toLowerCase(),
        createdAt: task.createdAt ? task.createdAt.replace('T', ' ').substring(0, 16) : ''
      }))
      
      // 更新统计
      stats.value.pending = orders.value.filter(o => o.status === 'pending').length
      stats.value.processing = orders.value.filter(o => o.status === 'in_progress').length
      stats.value.completed = orders.value.filter(o => o.status === 'completed').length
    }
  } catch (e) {
    console.error('加载工单失败', e)
    ElMessage.error('加载工单失败')
  }
  loading.value = false
}

// 获取类型标签样式
function getTypeTagType(type) {
  const map = { repair: 'danger', inspection: 'warning', maintenance: 'info' }
  return map[type] || 'info'
}

// 获取类型文本
function getTypeText(type) {
  const map = { repair: '故障维修', inspection: '设备巡检', maintenance: '预防维护' }
  return map[type] || type
}

// 获取状态文本
function getStatusText(status) {
  const map = { 
    pending: '待派单', 
    in_progress: '处理中', 
    processing: '处理中',
    completed: '已完成' 
  }
  return map[status] || status
}

// 显示创建对话框
function showCreateDialog() {
  createDialog.value = {
    visible: true,
    form: {
      type: '',
      location: '',
      description: '',
      priority: 'normal'
    }
  }
}

// 提交工单
async function submitOrder() {
  const form = createDialog.value.form
  if (!form.type || !form.location || !form.description) {
    ElMessage.warning('请填写完整信息')
    return
  }
  
  try {
    const taskData = {
      taskType: form.type.toUpperCase(),
      title: form.description,
      description: form.location,
      priority: form.priority.toUpperCase(),
      dueDate: new Date().toISOString().split('T')[0]
    }
    
    const res = await createTask(taskData)
    if (res && res.code === 200) {
      ElMessage.success('工单创建成功')
      createDialog.value.visible = false
      loadOrders()
    }
  } catch (e) {
    console.error('创建工单失败', e)
  }
}

// 加载巡检员列表
async function loadInspectors() {
  try {
    const res = await getUsers('INSPECTOR')
    if (res && res.code === 200 && res.data) {
      inspectors.value = res.data
    }
  } catch (e) {
    console.error('加载巡检员列表失败', e)
  }
}

// 派单
function assignOrder(order) {
  assignDialog.value = {
    visible: true,
    order,
    assigneeId: null,
    remark: ''
  }
}

// 确认派单
async function confirmAssign() {
  if (!assignDialog.value.assigneeId) {
    ElMessage.warning('请选择处理人')
    return
  }
  
  try {
    const order = assignDialog.value.order
    const res = await assignTask(order.id, assignDialog.value.assigneeId)
    if (res && res.code === 200) {
      const inspector = inspectors.value.find(i => i.id === assignDialog.value.assigneeId)
      ElMessage.success(`已派单给 ${inspector?.realName || '处理人'}`)
      assignDialog.value.visible = false
      loadOrders()
    }
  } catch (e) {
    console.error('派单失败', e)
    ElMessage.error('派单失败，请重试')
  }
}

// 查看工单
function viewOrder(order) {
  ElMessageBox.alert(
    `<div style="line-height: 1.8">
      <p><strong>工单号：</strong>${order.orderNo}</p>
      <p><strong>类型：</strong>${getTypeText(order.type)}</p>
      <p><strong>位置：</strong>${order.location}</p>
      <p><strong>描述：</strong>${order.description}</p>
      <p><strong>处理人：</strong>${order.assignee || '未分配'}</p>
      <p><strong>状态：</strong>${getStatusText(order.status)}</p>
      <p><strong>创建时间：</strong>${order.createdAt}</p>
    </div>`,
    '工单详情',
    { dangerouslyUseHTMLString: true }
  )
}

onMounted(() => {
  loadOrders()
  loadInspectors()
})
</script>

<style lang="scss">
@use '@/styles/dispatcher.scss';
</style>
