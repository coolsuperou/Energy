<template>
  <div class="admin-logs">
    <!-- 搜索和筛选栏 -->
    <div class="toolbar">
      <div class="filter-area">
        <el-select
          v-model="searchParams.operationType"
          placeholder="操作类型"
          clearable
          style="width: 140px"
          @change="loadLogs"
        >
          <el-option label="全部类型" value="" />
          <el-option
            v-for="type in operationTypes"
            :key="type"
            :label="getOperationTypeName(type)"
            :value="type"
          />
        </el-select>
        <el-select
          v-model="searchParams.userId"
          placeholder="操作用户"
          clearable
          filterable
          style="width: 150px"
          @change="loadLogs"
        >
          <el-option label="全部用户" :value="null" />
          <el-option
            v-for="user in userList"
            :key="user.id"
            :label="user.name"
            :value="user.id"
          />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 260px"
          @change="handleDateChange"
        />
        <el-button type="primary" @click="loadLogs">
          <i class="bi bi-search me-1"></i>查询
        </el-button>
        <el-button @click="resetFilters">
          <i class="bi bi-arrow-counterclockwise me-1"></i>重置
        </el-button>
      </div>
    </div>

    <!-- 日志列表表格 -->
    <el-table :data="logList" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="userName" label="操作人" width="100">
        <template #default="{ row }">
          <div class="user-cell">
            <span class="user-avatar">{{ row.userName?.charAt(0) || '?' }}</span>
            <span>{{ row.userName || '未知' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="operationType" label="操作类型" width="110">
        <template #default="{ row }">
          <el-tag :type="getOperationTagType(row.operationType)" size="small">
            {{ getOperationTypeName(row.operationType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operationDesc" label="操作内容" min-width="250">
        <template #default="{ row }">
          <span class="operation-desc">{{ row.operationDesc || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="ipAddress" label="IP地址" width="140">
        <template #default="{ row }">
          <span class="ip-address">
            <i class="bi bi-geo-alt me-1"></i>{{ row.ipAddress || '-' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="操作时间" width="180">
        <template #default="{ row }">
          <span class="time-cell">
            <i class="bi bi-clock me-1"></i>{{ formatDateTime(row.createdAt) }}
          </span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadLogs"
        @current-change="loadLogs"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOperationLogs, getOperationTypes, getUserList } from '@/api/admin'

// 日志列表数据
const logList = ref([])
const loading = ref(false)
const operationTypes = ref([])
const userList = ref([])
const dateRange = ref(null)

const searchParams = reactive({
  operationType: '',
  userId: null,
  startDate: '',
  endDate: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 加载操作日志
async function loadLogs() {
  loading.value = true
  try {
    const res = await getOperationLogs({
      page: pagination.page,
      size: pagination.size,
      operationType: searchParams.operationType || undefined,
      userId: searchParams.userId || undefined,
      startDate: searchParams.startDate || undefined,
      endDate: searchParams.endDate || undefined
    })
    if (res.code === 200 && res.data) {
      logList.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } catch (e) {
    console.error('加载操作日志失败', e)
  } finally {
    loading.value = false
  }
}

// 加载操作类型列表
async function loadOperationTypes() {
  try {
    const res = await getOperationTypes()
    if (res.code === 200 && res.data) {
      operationTypes.value = res.data
    }
  } catch (e) {
    console.error('加载操作类型失败', e)
  }
}

// 加载用户列表（用于筛选）
async function loadUserList() {
  try {
    const res = await getUserList({ page: 1, size: 1000 })
    if (res.code === 200 && res.data) {
      userList.value = res.data.records || []
    }
  } catch (e) {
    console.error('加载用户列表失败', e)
  }
}

// 处理日期范围变化
function handleDateChange(val) {
  if (val && val.length === 2) {
    searchParams.startDate = val[0]
    searchParams.endDate = val[1]
  } else {
    searchParams.startDate = ''
    searchParams.endDate = ''
  }
  loadLogs()
}

// 重置筛选条件
function resetFilters() {
  searchParams.operationType = ''
  searchParams.userId = null
  searchParams.startDate = ''
  searchParams.endDate = ''
  dateRange.value = null
  pagination.page = 1
  loadLogs()
}

// 获取操作类型名称
function getOperationTypeName(type) {
  const typeMap = {
    login: '登录',
    logout: '登出',
    create: '创建',
    update: '更新',
    delete: '删除',
    approve: '审批通过',
    reject: '审批拒绝',
    assign: '派单',
    complete: '完成'
  }
  return typeMap[type] || type
}

// 获取操作类型标签样式
function getOperationTagType(type) {
  const typeMap = {
    login: 'success',
    logout: 'info',
    create: 'primary',
    update: 'warning',
    delete: 'danger',
    approve: 'success',
    reject: 'danger',
    assign: 'primary',
    complete: 'success'
  }
  return typeMap[type] || 'info'
}

// 格式化日期时间
function formatDateTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 初始化
onMounted(() => {
  loadLogs()
  loadOperationTypes()
  loadUserList()
})
</script>


<style lang="scss" scoped>
.admin-logs {
  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 16px;
    background: #1a1a2e;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);

    .filter-area {
      display: flex;
      gap: 12px;
      align-items: center;
      flex-wrap: wrap;
    }
  }

  .user-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .user-avatar {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: linear-gradient(135deg, #00d4ff 0%, #0066ff 100%);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 600;
    }
  }

  .operation-desc {
    color: rgba(255, 255, 255, 0.85);
    line-height: 1.5;
  }

  .ip-address {
    color: rgba(255, 255, 255, 0.65);
    font-family: 'Consolas', monospace;
    font-size: 13px;
  }

  .time-cell {
    color: rgba(255, 255, 255, 0.65);
    font-size: 13px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
    padding: 16px;
    background: #1a1a2e;
    border-radius: 0 0 8px 8px;
  }

  :deep(.el-table) {
    border-radius: 8px;
    overflow: hidden;
    
    --el-table-bg-color: #1a1a2e;
    --el-table-tr-bg-color: #1a1a2e;
    --el-table-header-bg-color: #16213e;
    --el-table-row-hover-bg-color: #16213e;
    --el-table-border-color: #2a2a4a;
    --el-table-text-color: rgba(255, 255, 255, 0.85);
    --el-table-header-text-color: rgba(255, 255, 255, 0.9);
  }

  :deep(.el-input__wrapper) {
    background-color: #16213e;
    box-shadow: 0 0 0 1px #2a2a4a inset;
  }

  :deep(.el-select .el-input__wrapper) {
    background-color: #16213e;
  }

  :deep(.el-date-editor) {
    --el-date-editor-bg-color: #16213e;
    
    .el-input__wrapper {
      background-color: #16213e;
    }
  }

  :deep(.el-pagination) {
    --el-pagination-bg-color: transparent;
    --el-pagination-text-color: rgba(255, 255, 255, 0.85);
    --el-pagination-button-bg-color: #16213e;
    --el-pagination-hover-color: #409eff;
  }

  :deep(.el-tag) {
    border: none;
  }
}
</style>
