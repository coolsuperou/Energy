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
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const inspectionTasks = ref([
  {
    id: 1,
    name: '生产一车间巡检',
    location: '配电室、变压器、电缆沟',
    time: '08:00-10:00',
    progress: '12项检查点',
    progressPercent: 100,
    status: 'completed'
  },
  {
    id: 2,
    name: '生产二车间巡检',
    location: '配电室、UPS机房',
    time: '10:30-12:00',
    progress: '8/15项',
    progressPercent: 53,
    status: 'in_progress'
  },
  {
    id: 3,
    name: '装配车间巡检',
    location: '配电柜、照明系统',
    time: '14:00-16:00',
    progress: '10项检查点',
    progressPercent: 0,
    status: 'pending'
  }
])

const currentTask = ref({
  id: 2,
  name: '生产二车间'
})

const checkItems = ref([
  {
    name: '配电柜外观',
    content: '检查柜体是否完好，无变形、锈蚀',
    result: 'normal',
    remark: ''
  },
  {
    name: '温度检测',
    content: '测量配电柜温度，正常≤45°C',
    result: 'abnormal',
    remark: '温度65°C，已报修'
  },
  {
    name: '接线检查',
    content: '检查接线端子是否松动、发热',
    result: 'normal',
    remark: ''
  },
  {
    name: '指示灯状态',
    content: '检查各指示灯工作是否正常',
    result: 'normal',
    remark: ''
  },
  {
    name: '仪表读数',
    content: '记录电压、电流、功率等参数',
    result: 'normal',
    remark: 'U=380V, I=125A'
  }
])

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

function startInspection(task) {
  task.status = 'in_progress'
  currentTask.value = { id: task.id, name: task.name.replace('巡检', '') }
  ElMessage.success('已开始巡检任务')
}

function submitInspection() {
  const abnormalItems = checkItems.value.filter(item => item.result === 'abnormal')
  
  if (abnormalItems.length > 0) {
    ElMessage.warning(`发现 ${abnormalItems.length} 项异常，请确认已处理`)
  } else {
    ElMessage.success('巡检记录已提交')
  }
  
  const task = inspectionTasks.value.find(t => t.id === currentTask.value.id)
  if (task) {
    task.status = 'completed'
    task.progressPercent = 100
    task.progress = task.progress.split('/')[1] || task.progress
  }
}
</script>

<style lang="scss" scoped>
.border-primary {
  border-width: 2px !important;
}
</style>
