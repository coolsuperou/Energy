<template>
  <div class="content-wrapper">
    <!-- 今日巡检任务 -->
    <div class="card mb-4">
      <div class="card-header d-flex justify-content-between align-items-center">
        <span>今日巡检任务</span>
        <span class="badge bg-primary">{{ pendingCount }}项待完成</span>
      </div>
      <div class="card-body">
        <div class="row g-4">
          <div v-for="task in inspectionTasks" :key="task.id" class="col-md-4">
            <div class="border rounded p-3" :class="{ 'border-primary': task.status === 'in_progress' }">
              <div class="d-flex justify-content-between align-items-start mb-2">
                <h6 class="mb-0">{{ task.name }}</h6>
                <span class="badge" :class="getStatusBadge(task.status)">{{ getStatusText(task.status) }}</span>
              </div>
              <p class="text-muted small mb-2">{{ task.location }}</p>
              <div class="d-flex justify-content-between text-muted small">
                <span><i class="bi bi-clock me-1"></i>{{ task.time }}</span>
                <span>{{ task.progress }}</span>
              </div>
              <div v-if="task.status === 'in_progress'" class="progress mt-2" style="height: 6px;">
                <div class="progress-bar" :style="{ width: task.progressPercent + '%' }"></div>
              </div>
              <div v-if="task.status === 'pending'" class="mt-2">
                <button class="btn btn-primary btn-sm w-100" @click="startInspection(task)">开始巡检</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 巡检记录表单 -->
    <div class="card">
      <div class="card-header d-flex justify-content-between align-items-center">
        <span>巡检记录 - {{ currentTask.name }}</span>
        <button class="btn btn-success btn-sm" @click="submitInspection">
          <i class="bi bi-check-lg me-1"></i>提交巡检
        </button>
      </div>
      <div class="card-body">
        <div class="table-responsive">
          <table class="table">
            <thead>
              <tr>
                <th>检查项目</th>
                <th>检查内容</th>
                <th>检查结果</th>
                <th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in checkItems" :key="index">
                <td>{{ item.name }}</td>
                <td>{{ item.content }}</td>
                <td>
                  <div class="btn-group btn-group-sm">
                    <input type="radio" class="btn-check" :name="'check' + index" :id="'check' + index + '-ok'" 
                           v-model="item.result" value="normal">
                    <label class="btn btn-outline-success" :for="'check' + index + '-ok'">正常</label>
                    <input type="radio" class="btn-check" :name="'check' + index" :id="'check' + index + '-warn'"
                           v-model="item.result" value="abnormal">
                    <label class="btn btn-outline-warning" :for="'check' + index + '-warn'">异常</label>
                  </div>
                </td>
                <td>
                  <input type="text" class="form-control form-control-sm" v-model="item.remark" placeholder="备注">
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTodayInspections, getInspectionItems, startInspection as startInspectionApi, submitInspection as submitInspectionApi } from '@/api/inspection'

const inspectionTasks = ref([])
const currentTask = ref(null)
const checkItems = ref([])

const pendingCount = computed(() => {
  return inspectionTasks.value.filter(t => t.status === 'pending' || t.status === 'in_progress').length
})

function getStatusBadge(status) {
  const map = {
    completed: 'bg-success',
    in_progress: 'bg-warning text-dark',
    pending: 'bg-secondary'
  }
  return map[status] || 'bg-secondary'
}

function getStatusText(status) {
  const map = {
    completed: '已完成',
    in_progress: '进行中',
    pending: '待开始'
  }
  return map[status] || status
}

// 加载今日巡检任务
async function loadInspectionTasks() {
  try {
    const res = await getTodayInspections()
    if (res && res.code === 200 && res.data) {
      inspectionTasks.value = (res.data || []).map(item => ({
        id: item.id,
        name: item.name || item.title,
        location: item.location,
        time: item.timeRange || `${item.startTime}-${item.endTime}`,
        progress: item.progress || `${item.completedItems || 0}/${item.totalItems || 0}项`,
        progressPercent: item.progressPercent || 0,
        status: item.status === 'COMPLETED' ? 'completed' : item.status === 'IN_PROGRESS' ? 'in_progress' : 'pending'
      }))
      
      // 默认选择第一个进行中的任务
      const inProgressTask = inspectionTasks.value.find(t => t.status === 'in_progress')
      if (inProgressTask) {
        currentTask.value = { id: inProgressTask.id, name: inProgressTask.name.replace('巡检', '') }
        loadCheckItems(inProgressTask.id)
      }
    }
  } catch (e) {
    console.error('加载巡检任务失败', e)
    ElMessage.error('加载巡检任务失败')
  }
}

// 加载检查项
async function loadCheckItems(inspectionId) {
  try {
    const res = await getInspectionItems(inspectionId)
    if (res && res.code === 200 && res.data) {
      checkItems.value = (res.data || []).map(item => ({
        id: item.id,
        name: item.name,
        content: item.description || item.content,
        result: item.result || 'normal',
        remark: item.remark || ''
      }))
    }
  } catch (e) {
    console.error('加载检查项失败', e)
  }
}

async function startInspection(task) {
  try {
    const res = await startInspectionApi(task.id)
    if (res && res.code === 200) {
      task.status = 'in_progress'
      currentTask.value = { id: task.id, name: task.name.replace('巡检', '') }
      await loadCheckItems(task.id)
      ElMessage.success('已开始巡检任务')
    }
  } catch (e) {
    console.error('开始巡检失败', e)
    ElMessage.error('开始巡检失败')
  }
}

async function submitInspection() {
  if (!currentTask.value) {
    ElMessage.warning('请先选择巡检任务')
    return
  }
  
  const abnormalItems = checkItems.value.filter(item => item.result === 'abnormal')
  
  try {
    const data = {
      items: checkItems.value.map(item => ({
        id: item.id,
        result: item.result,
        remark: item.remark
      }))
    }
    
    const res = await submitInspectionApi(currentTask.value.id, data)
    if (res && res.code === 200) {
      if (abnormalItems.length > 0) {
        ElMessage.warning(`巡检已提交，发现 ${abnormalItems.length} 项异常`)
      } else {
        ElMessage.success('巡检记录已提交')
      }
      
      const task = inspectionTasks.value.find(t => t.id === currentTask.value.id)
      if (task) {
        task.status = 'completed'
        task.progressPercent = 100
      }
      
      await loadInspectionTasks()
    }
  } catch (e) {
    console.error('提交巡检失败', e)
    ElMessage.error('提交巡检失败')
  }
}

onMounted(() => {
  loadInspectionTasks()
})
</script>

<style lang="scss" scoped>
.border-primary {
  border-width: 2px !important;
}
</style>
